# BAZINOX API foundation

This directory defines the server-side boundary for paid listings. The Android app is untrusted for price calculation, payment confirmation, balance credits and publication.

## Planned stack
- Node.js 22 + TypeScript API
- PostgreSQL for listings, fee quotes, payment intents and immutable ledger
- Payment-provider adapter selected after confirming merchant eligibility and country coverage
- Authenticated server-to-server webhooks

## Initial tariff
Iran base tariff: 100,000 toman (1,000,000 IRR) for a 30-day listing. This is a configurable default, not a live charge. Admin changes must be versioned; existing quotes retain their price until expiry.

## Safety
Do not put payment secrets, wallet private keys or seed phrases in this repository, APK, database plaintext or CI. Do not enable real collection until provider onboarding, webhook verification, refunds, reconciliation and security tests are complete.

API contracts: see `../docs/PAID_LISTING_SYSTEM.md` and `../docs/PAID_LISTING_API_EXAMPLE.md`.
