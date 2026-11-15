# BAZINOX Product Scope

## Market selection
Users can select a continent, country, and then a city/region. Country and city catalogs must be data-driven and localizable; do not hardcode a short sample list as the production catalog.

## Marketplace categories
1. Vehicles: cars, motorcycles, commercial vehicles, parts and accessories.
2. Real estate: sale, rent, residential, land and commercial property.
3. Electronics: phones, computers, cameras and accessories.
4. Home and living: furniture, appliances, tools and household goods.
5. Fashion and personal goods.
6. Jobs and recruitment: employment, remote work and internships.
7. Services: repairs, education, transport, professional and creative services.
8. Wholesale and B2B: suppliers, bulk listings, import/export inquiries.
9. Travel and stays: accommodation, tours and travel services.
10. Pets and pet supplies, sports, books, arts, agriculture, industrial equipment, food and events.

Restricted or regulated categories must be controlled by country-specific policy, age/identity checks where required, and moderation. Prohibited goods and services must not be listed.

## Core user features
- Registration, sign-in, account recovery and profile
- Create/edit/pause/delete listings with photos and optional video
- Category-specific attributes and validation
- Search, sort, filters, favorites and saved searches
- Buyer/seller chat, attachments and price offers
- Seller pages, ratings and review moderation
- Reports, blocking and dispute/support requests
- Notifications and account privacy controls

## Commerce features
- Orders and order states
- Seller/buyer receipts and transaction history
- Refund and cancellation workflow
- Optional promoted listings and business accounts
- Payment provider abstraction; never assume one provider supports every country/currency

## Localization and money
- UI localization with RTL/LTR support
- Market currencies: IRR, USD, EUR, CNY and RUB
- Store money as integer minor units or exact decimal values with explicit currency codes; never use floating point for balances.
- Clearly distinguish rial (IRR) from toman (10 IRR).
- FX quotes must include provider, timestamp, spread, fees, expiry and quote status.
- Display-only FX conversion is not a completed exchange or fiat payout.

## Admin and trust
- Listing moderation queue
- User and seller verification workflows
- Category/country configuration
- Abuse, fraud and suspicious transaction review
- Dispute handling and audit logs
- Role-based admin access and immutable audit events
