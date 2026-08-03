# MVNE handoff

MVNE (Mobile Virtual Network Enabler) — third-party that provides BSS / OSS infrastructure to multiple MVNOs.

## Roles

```
MNO (host network)
  ↑ wholesale capacity
MVNE (shared platform: BSS + OSS + HLR/HSS optional)
  ↑ multi-tenant platform
MVNO 1 — brand A
MVNO 2 — brand B
MVNO 3 — brand C
```

## When FIS plays MVNE role

- FIS hosts BSS for multiple MVNO brands (multi-tenant SaaS).
- Each MVNO has its own:
  - Brand (logo, copy)
  - Tariff plans
  - Subscriber base (logically partitioned)
  - Channel inventory
- Shared:
  - Billing engine
  - Mediation pipeline
  - Reporting infrastructure

## Multi-tenancy strategy

| Concern | Approach |
|---|---|
| Subscriber data | tenant_id on every row + row-level security |
| Tariff config | per-tenant config table |
| Reporting | tenant-scoped dashboards |
| Audit log | tenant + user trail |
| Encryption | per-tenant keys (KMS-managed) |

## Anti-patterns

- Schema-per-tenant in PostgreSQL when tenant count > 100 → migration hell.
- Single shared subscriber table without tenant_id → data leak risk.
- MVNE billing not granular per-tenant → settlement disputes.
