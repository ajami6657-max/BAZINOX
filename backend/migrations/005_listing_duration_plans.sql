-- BAZARINO listing duration plans: all amounts are stored in IRR.
ALTER TABLE public.listings
  ADD COLUMN IF NOT EXISTS plan_days INTEGER,
  ADD COLUMN IF NOT EXISTS listing_fee_irr BIGINT,
  ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;

ALTER TABLE public.listings
  DROP CONSTRAINT IF EXISTS listings_plan_days_check,
  ADD CONSTRAINT listings_plan_days_check
    CHECK (plan_days IN (30, 60, 90)),
  DROP CONSTRAINT IF EXISTS listings_listing_fee_irr_check,
  ADD CONSTRAINT listings_listing_fee_irr_check
    CHECK (
      (plan_days = 30 AND listing_fee_irr = 750000)
      OR (plan_days = 60 AND listing_fee_irr = 1500000)
      OR (plan_days = 90 AND listing_fee_irr = 2250000)
    );

ALTER TABLE public.listing_fee_payments
  DROP CONSTRAINT IF EXISTS listing_fee_payments_amount_irr_check,
  ADD CONSTRAINT listing_fee_payments_amount_irr_check
    CHECK (amount_irr IN (750000, 1500000, 2250000));

-- A listing's expiry is assigned only when a trusted payment process publishes it.
CREATE OR REPLACE FUNCTION public.set_listing_expiry_on_publish()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NEW.status = 'published' AND OLD.status IS DISTINCT FROM 'published' THEN
    IF NEW.plan_days NOT IN (30, 60, 90) OR NEW.plan_days IS NULL THEN
      RAISE EXCEPTION 'A valid paid listing plan is required before publishing';
    END IF;
    NEW.published_at := now();
    NEW.expires_at := now() + make_interval(days => NEW.plan_days);
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_listing_expiry_on_publish ON public.listings;
CREATE TRIGGER trg_listing_expiry_on_publish
BEFORE UPDATE OF status ON public.listings
FOR EACH ROW
EXECUTE FUNCTION public.set_listing_expiry_on_publish();

CREATE INDEX IF NOT EXISTS idx_listings_expiry
  ON public.listings(status, expires_at);
