-- BAZINOX paid-listing foundation (PostgreSQL)
-- Money is stored as integer currency units, never floating point.
CREATE TABLE IF NOT EXISTS listing_fee_rules (
 id UUID PRIMARY KEY, country_code CHAR(2) NOT NULL, category_id TEXT NOT NULL,
 currency CHAR(3) NOT NULL, amount_minor BIGINT NOT NULL CHECK (amount_minor >= 0),
 duration_days INTEGER NOT NULL CHECK (duration_days > 0), version INTEGER NOT NULL,
 enabled BOOLEAN NOT NULL DEFAULT TRUE, starts_at TIMESTAMPTZ NOT NULL,
 ends_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(country_code, category_id, currency, duration_days, version)
);
-- 100,000 toman = 1,000,000 IRR (rial).
INSERT INTO listing_fee_rules
(id,country_code,category_id,currency,amount_minor,duration_days,version,enabled,starts_at)
VALUES ('11111111-1111-4111-8111-111111111111','IR','*','IRR',1000000,30,1,TRUE,now())
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS listings (
 id UUID PRIMARY KEY, owner_id UUID NOT NULL, country_code CHAR(2) NOT NULL,
 category_id TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL,
 status TEXT NOT NULL CHECK (status IN ('DRAFT','PAYMENT_PENDING','PAID','PUBLISHED','PAYMENT_FAILED','PAYMENT_EXPIRED','CANCELLED','REFUND_PENDING','REFUNDED')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), published_at TIMESTAMPTZ
);
CREATE TABLE IF NOT EXISTS fee_quotes (
 id UUID PRIMARY KEY, listing_id UUID NOT NULL REFERENCES listings(id), owner_id UUID NOT NULL,
 currency CHAR(3) NOT NULL, amount_minor BIGINT NOT NULL CHECK (amount_minor >= 0),
 fee_rule_version INTEGER NOT NULL, expires_at TIMESTAMPTZ NOT NULL,
 consumed_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS payment_intents (
 id UUID PRIMARY KEY, listing_id UUID NOT NULL REFERENCES listings(id),
 quote_id UUID NOT NULL REFERENCES fee_quotes(id), provider TEXT NOT NULL,
 provider_reference TEXT, idempotency_key TEXT NOT NULL UNIQUE,
 currency CHAR(3) NOT NULL, amount_minor BIGINT NOT NULL CHECK (amount_minor >= 0),
 status TEXT NOT NULL CHECK (status IN ('CREATED','PENDING','SUCCEEDED','FAILED','EXPIRED','REFUNDED')),
 verified_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(provider,provider_reference)
);
CREATE TABLE IF NOT EXISTS ledger_transactions (
 id UUID PRIMARY KEY, payment_intent_id UUID NOT NULL REFERENCES payment_intents(id),
 external_event_id TEXT NOT NULL UNIQUE, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS ledger_entries (
 id UUID PRIMARY KEY, transaction_id UUID NOT NULL REFERENCES ledger_transactions(id),
 account_code TEXT NOT NULL, direction TEXT NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
 amount_minor BIGINT NOT NULL CHECK (amount_minor > 0), currency CHAR(3) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_listings_owner_status ON listings(owner_id,status);
CREATE INDEX IF NOT EXISTS idx_payment_intents_listing ON payment_intents(listing_id);
CREATE INDEX IF NOT EXISTS idx_fee_quotes_expiry ON fee_quotes(expires_at);
CREATE OR REPLACE FUNCTION prevent_ledger_mutation() RETURNS trigger AS $$
BEGIN RAISE EXCEPTION 'ledger rows are immutable; create compensating entries'; END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS ledger_entries_no_update_delete ON ledger_entries;
CREATE TRIGGER ledger_entries_no_update_delete BEFORE UPDATE OR DELETE ON ledger_entries
FOR EACH ROW EXECUTE FUNCTION prevent_ledger_mutation();
