-- BAZARINO initial PostgreSQL schema (amounts are integer IRR)
CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY,
  phone_e164 VARCHAR(20) UNIQUE NOT NULL,
  display_name VARCHAR(120),
  role VARCHAR(20) NOT NULL DEFAULT 'buyer',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS listings (
  id UUID PRIMARY KEY,
  seller_id UUID NOT NULL REFERENCES users(id),
  title VARCHAR(160) NOT NULL,
  description TEXT NOT NULL,
  category_slug VARCHAR(80) NOT NULL,
  price_irr BIGINT NOT NULL CHECK (price_irr >= 0),
  province VARCHAR(100) NOT NULL,
  city VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'draft'
    CHECK (status IN ('draft','pending_fee','published','rejected','archived')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS listing_fee_payments (
  id UUID PRIMARY KEY,
  listing_id UUID NOT NULL UNIQUE REFERENCES listings(id),
  amount_irr BIGINT NOT NULL CHECK (amount_irr = 1000000),
  provider VARCHAR(40) NOT NULL,
  provider_reference VARCHAR(160) UNIQUE,
  status VARCHAR(20) NOT NULL DEFAULT 'pending'
    CHECK (status IN ('pending','paid','failed','refunded')),
  verified_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS orders (
  id UUID PRIMARY KEY,
  buyer_id UUID NOT NULL REFERENCES users(id),
  seller_id UUID NOT NULL REFERENCES users(id),
  listing_id UUID NOT NULL REFERENCES listings(id),
  total_irr BIGINT NOT NULL CHECK (total_irr > 0),
  commission_rate_bps INTEGER NOT NULL DEFAULT 500 CHECK (commission_rate_bps = 500),
  commission_irr BIGINT NOT NULL CHECK (commission_irr >= 0),
  seller_net_irr BIGINT NOT NULL CHECK (seller_net_irr >= 0),
  status VARCHAR(24) NOT NULL DEFAULT 'pending_payment'
    CHECK (status IN ('pending_payment','paid','fulfilled','completed','cancelled','refunded','disputed')),
  idempotency_key VARCHAR(120) UNIQUE NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (commission_irr = (total_irr * commission_rate_bps) / 10000),
  CHECK (seller_net_irr = total_irr - commission_irr)
);

CREATE TABLE IF NOT EXISTS payment_events (
  id UUID PRIMARY KEY,
  provider VARCHAR(40) NOT NULL,
  provider_event_id VARCHAR(180) NOT NULL,
  order_id UUID REFERENCES orders(id),
  listing_fee_payment_id UUID REFERENCES listing_fee_payments(id),
  event_type VARCHAR(80) NOT NULL,
  verified BOOLEAN NOT NULL DEFAULT false,
  payload_digest VARCHAR(128) NOT NULL,
  received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(provider, provider_event_id)
);

CREATE TABLE IF NOT EXISTS commission_ledger (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL REFERENCES orders(id),
  entry_type VARCHAR(20) NOT NULL CHECK (entry_type IN ('commission','commission_reversal')),
  amount_irr BIGINT NOT NULL CHECK (amount_irr >= 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_listings_search ON listings(status, province, city, category_slug);
CREATE INDEX IF NOT EXISTS idx_orders_seller_status ON orders(seller_id, status);
