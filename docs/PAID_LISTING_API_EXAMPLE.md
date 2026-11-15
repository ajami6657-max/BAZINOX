# Paid listing API contract (not yet live)

## Quote
GET /v1/listing-fees/quote?country=IR&category=vehicles&currency=IRR&durationDays=30

Example response:
```json
{"quoteId":"server-generated-uuid","currency":"IRR","amountMinor":1000000,"displayAmount":"100,000 toman","durationDays":30,"expiresAt":"2026-11-15T21:00:00Z"}
```
100,000 toman equals 1,000,000 rials. The currency code is IRR; the display layer must not confuse rials and tomans.

## Create payment intent
POST /v1/listings/{listingId}/payment-intents

The request contains quoteId; an Idempotency-Key header is required. Server loads the quote and determines amount/currency. Never trust amount, currency, owner ID or paid status sent by the app.

## Status and publication
GET /v1/payments/{paymentId} returns server-authoritative status. Only an authenticated, verified provider settlement webhook can mark payment succeeded and publish a listing. Android cannot set payment status.
