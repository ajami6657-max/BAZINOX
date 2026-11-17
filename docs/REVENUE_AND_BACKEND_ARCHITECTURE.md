# BAZARINO — Iran launch architecture and revenue implementation

## Confirmed commercial rules
- Every submitted listing is charged **100,000 toman = 1,000,000 IRR**.
- Successful online transactions carry a **5% platform commission**.
- Commission is calculated against the final settled order amount. Refunds/cancellations must reverse the applicable commission.
- A listing is not published until the server confirms the listing-fee payment.
- Seller payout must wait for order completion and the configured dispute/return window.

## Important implementation boundary
The Android app displays these rules and contains a local calculation helper for estimates. The helper is not authoritative. No live payment gateway, production backend, wallet, settlement or money collection is connected in this commit. The server must calculate prices and verify provider webhooks before production activation.

## Recommended deployable architecture
- Android: Kotlin + Jetpack Compose; UI never stores provider credentials.
- API: HTTPS REST service (Ktor or Spring Boot) behind a reverse proxy.
- Database: PostgreSQL with integer amounts in IRR, idempotency keys and audit records.
- Media: object storage with signed upload URLs and generated thumbnails.
- Payments: an Iran-licensed payment provider after merchant eligibility and legal review; hosted/tokenized checkout, signed callbacks, reconciliation and refunds.
- Operations: admin console, structured logs, monitoring, backups and role-based access.

## Core tables
users, listings, listing_fee_payments, orders, order_items, payment_intents, payment_events, commission_ledger, seller_payouts, refunds, admin_audit_log.

All money columns are integer IRR. Provider references must be unique. Webhook processing must be idempotent.

## API contract (first milestone)
- GET /v1/config/pricing — server-authoritative listing fee and commission rate.
- POST /v1/listings — create draft listing.
- POST /v1/listings/{id}/fee-checkout — create listing-fee payment intent.
- GET /v1/payments/{id} — read verified payment state.
- POST /v1/orders/quote — server calculates order total, commission and seller net.
- POST /v1/orders — create pending order with idempotency key.
- POST /v1/orders/{id}/checkout — create buyer payment intent.
- POST /v1/webhooks/payments/{provider} — verify signature, deduplicate event, update payment/order state.
- POST /v1/orders/{id}/complete — completion transition after server-side eligibility checks.
- GET /v1/admin/revenue/summary — admin-only revenue totals.
- GET /v1/admin/ledger — admin-only fee/commission/refund/payout ledger.

## Commission accounting
For a completed order:
- gross settled order amount = G
- platform commission = floor(G * 500 / 10000) IRR
- seller payable before other disclosed adjustments = G - commission
- reverse commission proportionally when an eligible refund is approved.
- Record immutable ledger entries; never edit historical money rows in place.

## Security and launch gates
1. Authenticate users and enforce ownership on every listing/order endpoint.
2. Verify provider callbacks server-to-server; never trust a client "payment successful" screen.
3. Use idempotency keys for checkout and webhook events.
4. Encrypt transport; rate-limit auth, listing and checkout endpoints.
5. Keep secrets in server environment/secret manager, never GitHub source or APK.
6. Obtain legal/payment-provider approval for marketplace collection, holding funds, refunds and seller payouts.
7. Run provider sandbox tests and reconciliation before accepting real money.
