# MVNO architecture — overview

MVNO (Mobile Virtual Network Operator) — provides mobile services without owning radio spectrum / RAN. Leases capacity from MNO (Mobile Network Operator) host.

## Layers

```
[ Customer-facing apps + portals ]   ← MVNO owns
       ↓
[ BSS — billing, CRM, charging ]      ← MVNO owns
       ↓
[ OSS — service provisioning ]        ← MVNO owns
       ↓
[ HLR / HSS / EIR ]                   ← MVNO owns (full MVNO) or MNO (light MVNO)
       ↓
[ Core network — MSC / SGSN / PGW ]   ← MNO owns
       ↓
[ Radio Access Network (RAN) ]        ← MNO owns
```

## MVNO tiers

| Tier | What MVNO owns | Examples |
|---|---|---|
| **Branded reseller** | brand only | basic prepaid SIMs |
| **Light MVNO** | BSS + customer service | most consumer MVNOs |
| **Full MVNO** | BSS + OSS + HLR/HSS | telcos with own SIM cards |
| **Enhanced MVNO** | full + own services (VoIP, OTT) | enterprise-focused MVNOs |

## At FIS

Most FIS MVNO engagements are **light MVNO**: BSS (rating, billing, invoice, dunning) + customer self-care + integration to host MNO via standard interfaces (Diameter, IDoc, REST).
