# Backend

Recommended initial implementation: modular monolith with clear service boundaries, PostgreSQL, database migrations, OpenAPI contract and containerized local development. Split into independently deployed services only when scale and operations justify it.

Initial domains:
- identity: accounts, sessions, roles and verification status
- marketplace: countries, cities, categories and listings
- search: indexing and filtering
- messaging: conversations, messages and attachments
- commerce: offers, orders, refunds and disputes
- payments: payment intents, provider adapters and webhook verification
- ledger: append-only double-entry accounting
- chain-monitor: testnet event ingestion and reconciliation
- fx: quotes and currency metadata
- moderation: reports, policy decisions and audit trail

Never store private keys or seed phrases in PostgreSQL. Secrets belong in a managed secret store. Webhooks must be authenticated, replay-protected and idempotent.
