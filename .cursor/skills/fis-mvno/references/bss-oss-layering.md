# BSS / OSS layering

## BSS (Business Support Systems)

Customer-facing business operations.

| Component | Function |
|---|---|
| CRM | customer master data |
| Sales | order management, channel inventory |
| Charging | rating engine, balance management (prepaid) |
| Billing | invoicing, discounts, taxes |
| Customer Care | ticketing, complaint handling |
| Dunning | collection, suspension |

## OSS (Operations Support Systems)

Network-facing operations.

| Component | Function |
|---|---|
| Provisioning | activate / deactivate services on network elements |
| Inventory | what's deployed where (cell towers, SIM cards, IPs) |
| Fault management | network alarm correlation, trouble ticket |
| Performance | KPIs (call drop %, MOS), SLA reporting |
| Mediation | CDR ingestion / dedup / normalization |
| Workforce management | field tech dispatch |

## Boundary

```
[ BSS ]  ← customer / business operations
   |
   v        ↑ provisioning order, deactivation
   |        ↓ usage records (CDRs)
[ OSS ]  ← network operations
```

CDRs flow: network → mediation (in OSS) → rating (in BSS) → billing → invoice.
Provisioning orders flow: BSS → OSS → network elements.

## At FIS

- BSS is typically owned + customised by FIS for the MVNO.
- OSS often partially provided by host MNO; FIS integrates.
- Mediation may be FIS-built or COTS (Comverse, Convergys); CDR formats: ASN.1, CSV, XML.
