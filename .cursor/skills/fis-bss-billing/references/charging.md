# Charging

Real-time deduction of balance / bundle for prepaid services. Distinct from "rating" (which computes amount); charging actually moves money.

## Online Charging System (OCS)

- 3GPP standard for real-time charging.
- Interfaces:
  - **Gy** (Diameter) — core network ↔ OCS for data/voice charging.
  - **Ro** (Diameter) — IMS / VoIP charging.
  - **Sy** (Diameter) — for policy + charging interaction.

## Reservation pattern

```
1. Network element requests authorisation: "Subscriber X starts data session, reserve 5 MB."
2. OCS checks balance: ≥ 5 MB worth of credit?
   Yes → reserve 5 MB; return granted.
   No → return denied (or grant smaller chunk).
3. Subscriber consumes; element reports actual usage on session end / interim update.
4. OCS reconciles: actual ≤ reserved → release unused; actual > reserved → re-authorise next chunk or stop.
```

## State persistence

- Reservation state must survive OCS restart.
- Use distributed cache (Redis Cluster) + persistent log (DB) for reconciliation post-crash.

## Quota types

- **Volume quota** (data) — bytes.
- **Time quota** (voice) — seconds.
- **Service-specific quota** — bundle-mapped.

## Anti-patterns

- No reservation timeout → if subscriber session never closes (network glitch), credit locked forever.
- Reservation > balance → over-grant; subscriber can use beyond credit.
- No reconciliation between Gy reports and balance store → drift over time.
