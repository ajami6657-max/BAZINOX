# BAZINOX

Global classifieds and commerce marketplace for Android, with country-based markets and a USDT payment architecture.

## Product scope
BAZINOX is planned as a multi-country marketplace inspired by classifieds platforms, expanded for international trade:
- Vehicles, real estate, electronics, home and general goods
- Jobs, services, wholesale, travel and accommodation
- Search, category filters, saved listings, seller profiles and ratings
- Buyer/seller messaging, offers, orders, reports and moderation
- Country/continent selection, city/region filters and localization
- Currency display for IRR (rial), USD, EUR, CNY and RUB
- USDT payment workflows, initially on TRON TRC-20 in test environments

## Architecture
- `android/`: Kotlin + Jetpack Compose Android client
- `backend/`: API and domain services
- `docs/`: product and security architecture
- `.github/workflows/`: CI automation

## Development status
This repository currently contains an initial project README. It is **not yet a functioning marketplace or payment service**. No real-money or mainnet transactions are enabled.

## Security rules
- Never commit API keys, seed phrases, private keys, keystore files or production credentials.
- Never put custody keys in the Android APK.
- Test blockchain integration on a testnet before any production consideration.
- Real custody, exchange, fiat payout and money transmission require qualified legal review, licensing and regulated providers in each operating jurisdiction.

## First milestone
Create the Android app shell, backend health endpoint, database migrations and automated build/test workflows. Then implement listing, search, messaging and testnet-only USDT deposit monitoring.

## License
Proprietary project. Add a license only after deciding the intended distribution and ownership terms.
