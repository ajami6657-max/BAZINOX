# BAZINOX GLOBAL — Iran-first MVP plan

## Release principle
Launch one dependable Iranian marketplace before enabling international checkout. The data model must be country-aware from day one, but country expansion is feature-flagged.

## MVP user journeys
1. Buyer registers and verifies an available contact method.
2. Seller creates a listing with category, title, description, photos, price in IRR, province and city.
3. Buyer searches and opens listing details.
4. Buyer contacts seller, places an order and sees the order state.
5. Seller accepts or rejects the order and updates fulfilment.
6. Payment provider confirms payment server-to-server; the app never marks an order paid based only on a client callback.
7. Admin reviews reported listings, sellers and orders.

## Core domain entities
- users, seller_profiles
- countries, regions, cities
- categories, listings, listing_images
- conversations, messages
- orders, order_items, order_events
- payment_intents, payment_events, refunds
- reviews, reports, moderation_actions

All persisted timestamps use UTC. Every listing and order has an explicit country and currency. Iran launch uses IRR as the accounting currency; the UI may show toman as a clearly labelled display unit. Do not treat a display conversion as a payment conversion.

## API boundaries
- Auth: registration, login, session refresh, contact verification
- Catalog: categories, listing create/update, listing media, listing detail
- Search: query, category, province, city, price range, sort and pagination
- Messaging: conversations and messages
- Checkout: quote, create order, payment intent, payment status
- Seller: profile, inventory and order fulfilment
- Admin: reports, moderation and audit trail

## Reliability and low-bandwidth requirements
- Paginated results and small image thumbnails
- Upload retry with resumable/chunked support where available
- Explicit loading, empty, offline and retry states
- Avoid loading full-resolution images in listing feeds
- Cache public category and location metadata
- Do not promise offline checkout

## Payment safety
Payment providers must be selected only after confirming legal availability and merchant onboarding for the operating country. Use provider-hosted or tokenized payment flows where possible. Verify signed webhooks on the backend, make event processing idempotent, reconcile provider settlements, and support refunds. Do not embed payment secrets in the APK. No production money movement is enabled in this milestone.

## International expansion gates
A new country is enabled only after country-specific review of consumer protection, privacy, tax, prohibited goods, seller verification, payment providers, currency handling, delivery and dispute resolution. Feature flags control country availability and payment methods.

## Current implementation boundary
This document is the approved implementation plan. It does not claim the backend, account system, checkout, payment gateway, chat or shipping service already exists.
