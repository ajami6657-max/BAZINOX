# BAZINOX Paid Listing System

## Product rule
A listing is not public until its fee has been paid and independently verified by the payment provider. The Android client must never mark a listing as paid or published based only on a local button tap or a client-supplied transaction status.

## Fee configuration
Fees are configured server-side, not hard-coded in the APK. A fee rule is scoped by:
- country (ISO 3166-1 alpha-2; `*` may be used only as an explicit fallback),
- category identifier,
- listing type / duration,
- currency (ISO 4217),
- amount in minor units (integer),
- active dates and enabled status.

The quote returned to the app includes an immutable quote ID, exact amount, currency, expiry time, fee-rule version, and any applicable tax. The user sees the total before confirming payment. Iran display rules must distinguish IRR (rial) from toman: 1 toman = 10 rials.

## Listing lifecycle
`DRAFT -> PAYMENT_PENDING -> PAID -> PUBLISHED`
Failure/expiry paths: `PAYMENT_FAILED`, `PAYMENT_EXPIRED`, `CANCELLED`, `REFUND_PENDING`, `REFUNDED`.
A listing must remain private while payment is pending. Publication is an atomic server-side action after verified settlement.

## Payment lifecycle
1. Authenticated client requests a fee quote for country, category, duration and currency.
2. Server validates the listing type and returns a short-lived quote.
3. Client creates a payment intent using the quote ID and an idempotency key.
4. A country-enabled payment provider or crypto processor receives the payment.
5. Provider webhook is authenticated and verified server-side. For crypto, verify chain, token contract, destination, amount, confirmations/finality and transaction uniqueness.
6. Server posts balanced double-entry ledger entries and reconciles provider/chain settlement.
7. Server marks the payment paid and publishes the listing exactly once.

## Suggested API contract
- `GET /v1/listing-fees/quote?country=XX&category=...&currency=...`
- `POST /v1/listings` creates a private draft.
- `POST /v1/listings/{id}/payment-intents` creates an idempotent payment intent.
- `GET /v1/payments/{id}` returns server-authoritative status.
- `POST /v1/webhooks/payments/{provider}` receives provider events (server-to-server only).
- `GET /v1/me/listings` returns the user's listing statuses.

The API must enforce authentication, ownership, rate limits, quote expiry, idempotency, and server-side price calculation. Never accept an amount or currency as authoritative from the Android client.

## Payment methods
Payment methods are selected by country and legal availability. The initial crypto design may support USDT on TRON testnet only. Testnet payments are demonstrations and do not collect real money. Production crypto requires a compliant provider/custody arrangement, sanctions screening where required, security review, and operational reconciliation. Never store private keys or seed phrases in the APK, GitHub, ordinary database fields, or CI secrets.

## Admin controls
Admin roles can create/version/disable fee rules, inspect payment and listing states, issue audited refunds, and view reconciliation reports. Every fee change and financial adjustment must be audit logged. Existing quotes retain their quoted amount until expiry.

## Acceptance tests
- Unpaid, failed, expired or unverified payments never publish a listing.
- A duplicate webhook or retry cannot double-credit or publish twice.
- Client tampering with price, currency, payment status or listing owner is rejected.
- A valid settlement publishes exactly once.
- Refunds create compensating ledger entries; historical ledger entries are never edited.
- Country/currency display is correct, including the rial/toman distinction.
