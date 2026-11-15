# Android app

Target stack: Kotlin, Jetpack Compose, Navigation Compose, ViewModel, Kotlin Coroutines/Flow and Retrofit or Ktor client.

Planned modules:
- app: application shell, navigation and dependency injection
- core: design system, networking, localization and common models
- feature-market: home, continent/country/city selection and category browsing
- feature-listings: create/edit listing, listing detail, search and filters
- feature-chat: buyer/seller messaging
- feature-account: profile, favorites, verification and settings
- feature-orders: orders, offers, refunds and receipts
- feature-wallet: balance display, payment intents and transaction history

Security: no seed phrases, private keys, production credentials or custody signing logic in the APK. Wallet connection must use a reviewed external-wallet flow.
