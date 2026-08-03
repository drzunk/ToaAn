# Rating engine

Converts CDRs (usage events) into charge amounts according to the customer's tariff plan.

## Inputs

- Normalized CDR (from mediation)
- Subscriber's tariff plan
- Bundles / promotions / loyalty status
- FUP (Fair Usage Policy) state — has the subscriber crossed thresholds?

## Outputs

- Rated event with amount + chargeable-to (account / bucket / credit balance)
- Bundle counter decrement
- FUP state update

## Tariff structure

| Component | Description |
|---|---|
| Base rate | per-unit price (VND / minute, VND / SMS, VND / MB) |
| Time-of-day | peak / off-peak multipliers |
| Calling-circle | reduced rate for in-network or "10 favorite numbers" |
| Bundles | included units (e.g. 1000 mins / month) — decremented first |
| Free units | promo units (signup bonus) — decremented before paid |
| FUP | post-threshold rate (e.g. unlimited data → throttled / charged after 50 GB) |
| Roaming | per partner network surcharge |

## Rating order (canonical)

1. Authenticate subscriber + plan currency.
2. Apply roaming if foreign network.
3. Decrement free units (signup bonus).
4. Decrement plan bundles.
5. Apply post-bundle rate (peak / off-peak / TOD).
6. Apply calling-circle discount if applicable.
7. Apply FUP rate if over threshold.
8. Compute tax (VAT 10% / 8% / 5%) per tax code.
9. Round per partner agreement (usually whole VND).

## Real-time vs offline

- **Offline (postpaid)** — events processed in batch (every 15 min — every cycle).
- **Real-time (prepaid)** — OCS Gy interface; balance check + decrement before service authorisation.

## Anti-patterns

- Rounding errors at each step → cumulative drift.
- Bundle counters not transactional → over-consumption when retried.
- FUP threshold not subscriber-state-aware → roaming fail.
