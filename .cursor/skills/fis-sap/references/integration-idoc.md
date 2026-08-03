# Integration — IDoc

## When to use

- Async transaction (no immediate response needed).
- High volume (batch) or unreliable network.
- Cross-system EDI / ALE (SAP-to-SAP) or partner integration.

## Architecture

```
Sender system → IDoc segments → outbound port (tRFC/file) →
  receiver port → IDoc inbound function module → BAPI → DB
```

## Setup checklist

- Partner profile (WE20) per partner × message type.
- Port definition (WE21) — file / tRFC / SOAP.
- Distribution model (BD64) for ALE scenarios.
- Process code (WE42 inbound, WE41 outbound).
- Filter / convert (BD79) if needed.

## Common IDoc message types

| Type | Use |
|---|---|
| ORDERS05 | sales / purchase order |
| INVOIC02 | invoice |
| MATMAS05 | material master |
| DEBMAS06 | customer master |
| CREMAS05 | vendor master |

## Anti-patterns

- Ad-hoc partner profile created in prod without transport → desync between systems.
- Not configuring inbound error workflow → IDocs stuck in 51 status forever.
- Custom Z-IDoc without segment versioning → upgrade nightmare.
