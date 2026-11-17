-- BAZARINO: atomically publish a listing only after the server verifies ZarinPal.
-- Apply after migrations 001-006. Only service_role may execute this function.
CREATE OR REPLACE FUNCTION public.confirm_listing_fee_payment(
  p_payment_id UUID,
  p_provider_event_id VARCHAR,
  p_payload_digest VARCHAR
) RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_payment public.listing_fee_payments%ROWTYPE;
  v_listing public.listings%ROWTYPE;
  v_event_id UUID;
BEGIN
  SELECT * INTO v_payment
  FROM public.listing_fee_payments
  WHERE id = p_payment_id
  FOR UPDATE;

  IF NOT FOUND THEN RAISE EXCEPTION 'Listing payment not found'; END IF;
  IF v_payment.provider <> 'zarinpal' THEN RAISE EXCEPTION 'Unexpected payment provider'; END IF;

  IF v_payment.status = 'paid' THEN RETURN TRUE; END IF;
  IF v_payment.status <> 'pending' THEN
    RAISE EXCEPTION 'Payment cannot be confirmed from status %', v_payment.status;
  END IF;

  SELECT * INTO v_listing
  FROM public.listings
  WHERE id = v_payment.listing_id
  FOR UPDATE;

  IF NOT FOUND OR v_listing.status <> 'pending_fee' THEN
    RAISE EXCEPTION 'Listing is not awaiting payment';
  END IF;

  INSERT INTO public.payment_events(
    id, provider, provider_event_id, listing_fee_payment_id,
    event_type, verified, payload_digest
  ) VALUES (
    gen_random_uuid(), 'zarinpal', p_provider_event_id, p_payment_id,
    'listing.payment_succeeded', TRUE, p_payload_digest
  )
  ON CONFLICT (provider, provider_event_id) DO NOTHING
  RETURNING id INTO v_event_id;

  IF v_event_id IS NULL THEN
    -- A repeated callback is safe only if this payment was already completed.
    SELECT * INTO v_payment FROM public.listing_fee_payments WHERE id = p_payment_id;
    IF v_payment.status = 'paid' THEN RETURN TRUE; END IF;
    RAISE EXCEPTION 'Payment event was already used';
  END IF;

  UPDATE public.listing_fee_payments
  SET status = 'paid', verified_at = now()
  WHERE id = p_payment_id;

  UPDATE public.listings
  SET status = 'published'
  WHERE id = v_listing.id AND status = 'pending_fee';

  RETURN TRUE;
END;
$$;

REVOKE ALL ON FUNCTION public.confirm_listing_fee_payment(UUID, VARCHAR, VARCHAR)
  FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.confirm_listing_fee_payment(UUID, VARCHAR, VARCHAR)
  TO service_role;
