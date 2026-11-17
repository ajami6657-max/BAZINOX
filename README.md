# BAZINOX GLOBAL

**BAZINOX GLOBAL** is an Iran-first online classifieds and commerce marketplace, designed to expand country by country.

## Product decisions
- Brand: BAZINOX GLOBAL
- Launch market: Iran
- Expansion: international markets after the Iran launch is stable
- Commerce model: online orders and payments, with seller/buyer messaging
- Android package: `com.bazinox.app`

## First release scope
1. Country and city selection, defaulting to Iran
2. Classified listings with photos, category, price and location
3. Search, category filters and listing details
4. Buyer/seller accounts and seller profiles
5. Messaging, orders and order status
6. Payment integration through licensed providers available in each market
7. Admin moderation, reports and seller verification

## Technical structure
- `app/`: Android Kotlin + Jetpack Compose client
- `backend/`: backend API and database migrations
- `docs/`: product, API and security decisions
- `.github/workflows/`: automated Android build and test

## Development status
The current Android project is an early UI shell. It is **not yet a live marketplace**: accounts, server-backed listings, chat, orders, payment collection and delivery integrations still need implementation. The CI workflow builds a debug APK artifact; a successful build does not mean those services are live.

## Security and payments
- Never commit API keys, passwords, private keys, seed phrases or signing keystores.
- Never store custody keys in the Android app.
- Do not collect real money until a verified payment provider, server-side payment confirmation, refunds and reconciliation are implemented.
- USDT or cross-border payment support must be reviewed against local laws, sanctions, licensing and provider availability before activation.
- Use sandbox/test environments before production payment integration.

## Build
GitHub Actions runs `gradle assembleDebug` and `gradle testDebugUnitTest` on pushes to `main`. The debug APK is uploaded as a workflow artifact.

## License
Proprietary project. No open-source license has been granted.
