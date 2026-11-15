# BAZINOX Crypto Pay — Hybrid USDT Architecture

## Scope and launch boundary
USDT is the initial crypto asset. TRON TRC-20 is the first integration target. Development and automated tests must use testnet/test tokens only. This document does not authorize custody, exchange, remittance or fiat payout. Production activation requires jurisdiction-by-jurisdiction legal review, licensed providers where required, security audit and operational controls.

## Hybrid custody model
1. **User-controlled wallet path:** users may connect an external wallet and approve transactions themselves. BAZINOX does not collect or store their seed phrase/private key.
2. **Platform payment ledger:** BAZINOX maintains an internal double-entry ledger for marketplace orders and payment states. Ledger entries represent verified liabilities/credits and must reconcile to actual assets; they are not a substitute for custody controls.
3. **Custody provider path (optional, gated):** any custodial deposits/withdrawals must be handled by a qualified, authorized custody provider or hardened MPC/HSM infrastructure after legal approval. No private keys in Android, source control, ordinary database rows or CI secrets.

## Services
- Payment API: creates payment intents and returns amount, token contract, network, destination and expiry.
- Address service: obtains deposit addresses from the approved wallet/custody provider. Address allocation must be network- and asset-specific.
- Chain watcher: monitors finalized TRON events; validates chain/network, trusted USDT contract address, recipient, amount, transaction status and finality.
- Reconciliation worker: independently compares chain activity, provider statements and ledger entries.
- Ledger: append-only double-entry postings, idempotency keys, unique chain event identifiers and explicit reversal entries.
- Withdrawal service: policy checks, risk screening, address allowlists, limits, cooling-off period and multi-party approval.
- FX quote service: returns indicative or executable quotes only when a real liquidity/settlement provider is connected. Quotes include source, timestamp, expiry, fees and spread.
- Audit/risk service: records actor, device/session, decision and reason for sensitive actions.

## Required invariants
- Never credit a deposit from a client callback or unverified transaction hash alone.
- Verify the exact token contract and network; symbols are not proof of token identity.
- Handle duplicate events idempotently.
- Use decimal-safe arithmetic; never floating-point balances.
- Separate pending, confirmed, available, held and reversed balances.
- Reconcile liabilities against safeguarded assets.
- Do not advertise IRR/USD/EUR/CNY/RUB as withdrawable balances until a compliant liquidity and payout route exists.
- Never promise a single address works across every chain/token.

## Test plan
- TRON testnet only; test tokens have no monetary value.
- Valid transfer, wrong token contract, wrong network, wrong recipient, underpayment, overpayment, duplicate event, failed transaction, delayed indexing and provider outage.
- Ledger idempotency, concurrent order settlement, refund and reconciliation tests.
- Security tests for authorization, rate limits, replay, webhook signatures and secret leakage.

## Production gates
1. Legal classification and licensing analysis for every launch market.
2. Contract/provider due diligence and sanctions/AML controls.
3. Independent integration and application security review.
4. Custody architecture review, key ceremonies, recovery and incident response.
5. Operational reconciliation, support, dispute and financial reporting procedures.
