-- BAZARINO trusted payment and 2% commission foundation.
-- Only a server-side verified payment webhook may call privileged functions.
-- Never put the Supabase service_role key in Android or a public repository.

-- New orders use 2%; preserve 5% on historical orders.
ALTER TABLE public.orders ALTER COLUMN commission_rate_bps SET DEFAULT 200;
ALTER TABLE public.orders DROP CONSTRAINT IF EXISTS orders_commission_rate_bps_check;
ALTER TABLE public.orders ADD CONSTRAINT orders_commission_rate_bps_check
  CHECK (commission_rate_bps IN (200, 500));

CREATE UNIQUE INDEX IF NOT EXISTS ux_commission_ledger_order_entry
  ON public.commission_ledger(order_id, entry_type);

-- Create an order with its commission calculated on the server.
CREATE OR REPLACE FUNCTION public.create_marketplace_order(
  p_order_id UUID, p_buyer_id UUID, p_seller_id UUID,
  p_listing_id UUID, p_total_irr BIGINT, p_idempotency_key VARCHAR
) RETURNS UUID
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
  v_commission BIGINT;
  v_existing UUID;
BEGIN
  IF p_total_irr <= 0 OR p_buyer_id = p_seller_id THEN
    RAISE EXCEPTION 'Invalid order details';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM public.listings
    WHERE id = p_listing_id AND seller_id = p_seller_id AND status = 'published'
  ) THEN
    RAISE EXCEPTION 'Listing is not available';
  END IF;

  SELECT id INTO v_existing FROM public.orders WHERE idempotency_key = p_idempotency_key;
  IF v_existing IS NOT NULL THEN RETURN v_existing; END IF;

  v_commission := (p_total_irr * 200) / 10000;
  INSERT INTO public.orders(
    id, buyer_id, seller_id, listing_id, total_irr,
    commission_rate_bps, commission_irr, seller_net_irr, status, idempotency_key
  ) VALUES (
    p_order_id, p_buyer_id, p_seller_id, p_listing_id, p_total_irr,
    200, v_commission, p_total_irr - v_commission, 'pending_payment', p_idempotency_key
  );
  RETURN p_order_id;
END;
$$;

-- Call only after a trusted webhook verifies provider signature, amount,
-- currency, merchant account and successful transaction status.
CREATE OR REPLACE FUNCTION public.confirm_marketplace_order_payment(
  p_order_id UUID, p_provider VARCHAR,
  p_provider_event_id VARCHAR, p_payload_digest VARCHAR
) RETURNS BOOLEAN
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
  v_order public.orders%ROWTYPE;
  v_event_id UUID;
BEGIN
  SELECT * INTO v_order FROM public.orders WHERE id = p_order_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Order not found'; END IF;

  INSERT INTO public.payment_events(
    id, provider, provider_event_id, order_id,
    event_type, verified, payload_digest
  ) VALUES (
    gen_random_uuid(), p_provider, p_provider_event_id, p_order_id,
    'order.payment_succeeded', TRUE, p_payload_digest
  )
  ON CONFLICT (provider, provider_event_id) DO NOTHING
  RETURNING id INTO v_event_id;

  IF v_event_id IS NULL THEN RETURN TRUE; END IF;
  IF v_order.status <> 'pending_payment' THEN
    RAISE EXCEPTION 'Order cannot be paid from status %', v_order.status;
  END IF;

  UPDATE public.orders SET status = 'paid' WHERE id = p_order_id;
  INSERT INTO public.commission_ledger(id, order_id, entry_type, amount_irr)
  VALUES (gen_random_uuid(), p_order_id, 'commission', v_order.commission_irr)
  ON CONFLICT (order_id, entry_type) DO NOTHING;
  RETURN TRUE;
END;
$$;

REVOKE ALL ON FUNCTION public.create_marketplace_order(UUID, UUID, UUID, UUID, BIGINT, VARCHAR) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.confirm_marketplace_order_payment(UUID, VARCHAR, VARCHAR, VARCHAR) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.create_marketplace_order(UUID, UUID, UUID, UUID, BIGINT, VARCHAR) TO service_role;
GRANT EXECUTE ON FUNCTION public.confirm_marketplace_order_payment(UUID, VARCHAR, VARCHAR, VARCHAR) TO service_role;
