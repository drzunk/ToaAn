# HLR / HSS — Subscriber registry

## HLR (Home Location Register)

- 2G/3G subscriber database.
- Stores: IMSI, MSISDN, subscription profile, current MSC location.
- Interfaces: SS7 MAP (legacy).

## HSS (Home Subscriber Server)

- 4G/5G subscriber database. Successor to HLR.
- Stores: same as HLR + EPS (Evolved Packet System) profile, AKA authentication keys.
- Interfaces: Diameter S6a (MME ↔ HSS), Cx/Dx (CSCF ↔ HSS for IMS).

## Combined HLR/HSS (HLR/HSS-FE)

- Most modern deployments fuse both into one logical entity.
- Single data model; protocol gateway dispatches based on access network.

## At FIS — light MVNO

- MVNO typically does NOT own HLR/HSS.
- Provisioning happens via host MNO API:
  - REST/SOAP for activate / deactivate / suspend.
  - File-based bulk for migration / reconciliation.

## At FIS — full MVNO

- Own HLR/HSS deployed.
- Provisioning via internal OSS → HLR/HSS via Diameter / SS7.
- Roaming agreements: HSS replicates with partner HSS via Diameter (DSC).

## Anti-patterns

- Bypassing HLR/HSS for "test" provisioning → leaks into production state.
- Double-bookkeeping (BSS subscriber DB ≠ HLR) → de-sync; mediation needs reconciliation.
