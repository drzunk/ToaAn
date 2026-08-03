# Integration — OData + Fiori (S/4HANA)

## When to use

- Modern UI / mobile / external API on S/4HANA.
- Replacing legacy SAPGUI for custom transactions.

## Stack

```
CDS view (annotated) → OData service (auto-exposed) →
  SAP Gateway → REST/JSON → Fiori frontend / mobile / external
```

## CDS annotations relevant

- `@OData.publish: true` — expose service automatically.
- `@UI.lineItem` / `@UI.facet` — drive Fiori list/detail UI.
- `@Search.searchable` — enable full-text on the entity.

## OData operations

- `$filter`, `$select`, `$expand`, `$orderby` — must be supported server-side.
- `$top` + `$skip` — pagination (always implement; never return all rows).
- Batch (`$batch`) — combines multiple ops into one HTTP roundtrip.

## Anti-patterns

- OData service without server-side paging → mobile / external client OOM.
- Exposing all CDS fields without `@AccessControl` → data leak.
- Frontend binding to `Edm.Decimal` without scale config → rounding errors VND.
