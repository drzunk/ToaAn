# Revenue assurance

Detect leakage in the rating + billing pipeline. Goal: < 0.5% leakage of theoretical revenue.

## Leakage sources

| Source | Detection |
|---|---|
| Lost CDRs in mediation | dedup-vs-source count delta |
| Mis-rated events | sample replay against rule engine; expected vs actual |
| Bundle over-consumption | bundle counter trace per subscriber |
| Billing not invoiced | unbilled rated events at cycle close |
| Invoice not paid (collection efficiency) | aging buckets vs total |
| Rounding drift | sum over time vs theoretical |

## Reconciliation chain

```
Network elements → Mediation → Rating → Billing → Invoice → Payment
     (a)              (b)        (c)       (d)        (e)        (f)

For each link:
- count records in == count records out (within tolerance)
- sum amount in == sum amount out (within tolerance)
- per-subscriber detail spot-check
```

## Sampling

- 100% reconciliation is expensive; sample 1-5% subscribers per cycle for end-to-end manual review.
- High-ARPU customers: 100% review.
- Suspicious patterns (sudden ARPU drop, new tariff): full review.

## Tools

- ETL pipeline → data warehouse (BigQuery / Snowflake / ClickHouse).
- Dashboards per leakage source.
- Scheduled SQL alerts when delta > threshold.

## Anti-patterns

- Reconciliation only at billing cycle end → leakage detected too late.
- No sampling for low-ARPU mass-market → skim leakage uncovered.
- Rounding silently → cumulative drift over months.
