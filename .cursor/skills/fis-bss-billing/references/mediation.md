# Mediation

Pipeline that ingests CDRs (Call Detail Records) from network elements, deduplicates, normalizes, and forwards to the rating engine.

## CDR formats

| Format | Source | Notes |
|---|---|---|
| ASN.1 (3GPP) | MSC, SGSN, PGW (legacy/voice/data) | Binary; needs ASN.1 decoder |
| CSV | OSS dumps, BS file feeds | Easy parse; less structured |
| XML | Modern IMS / VoLTE / OTT | Structured; verbose |
| JSON | New 5G core, Diameter Gy events | Modern; native to event-driven stacks |

## Deduplication

- Dedup key per CDR type:
  - Voice: `(IMSI, called_party, start_time, switch_id)`
  - Data: `(IMSI, session_id, charging_id)`
  - SMS: `(IMSI, msg_ref, timestamp)`
- Window: configurable per partner (typical 24-48h).
- Dedup store: Redis with TTL ≥ window, or DB index with cleanup job.

## Normalization

- Time: convert to UTC, store original tz.
- Volume: convert KB / MB / pages → consistent unit (bytes for data; minutes for voice rounded per partner-specific rule — usually 1-min increments after first second).
- IMSI/MSISDN: validate format (15 digits IMSI, MCC `452` for VN).
- Service code mapping: each network gives its own code; map to canonical service catalog.

## Late records

- A CDR arriving > N hours after `start_time` should:
  - Still process if within billing cycle window.
  - Send to "late CDR" queue if past billing cutoff.
  - Generate accrual / adjustment in next cycle.

## Reconciliation

- Compare CDR count + bytes between mediation input vs rating output.
- Daily report; threshold alarm if delta > 0.1%.

## Anti-patterns

- No dedup window → duplicate billing on retried records.
- Lossy decimals on byte volume → 0.5% revenue leak at scale.
- Late-record handling missing → revenue assurance fail at audit.
