# SIM / IMSI lifecycle

## Identifiers

| ID | Length | Description |
|---|---|---|
| **IMSI** | 15 digits | International Mobile Subscriber Identity. `MCC (3) + MNC (2-3) + MSIN (9-10)`. Lives on SIM. |
| **ICCID** | 19-20 digits | Integrated Circuit Card ID. SIM card serial number. Has Luhn checksum. |
| **MSISDN** | up to 15 | Mobile Subscriber ISDN. The phone number user types. |
| **IMEI** | 15 digits | International Mobile Equipment Identity. Handset serial. Has Luhn checksum. |

VN MCC: 452. MNCs: Viettel 04, Mobifone 01, Vinaphone 02, Vietnamobile 05, Gmobile 07, ITelecom 08.

## Lifecycle states

```
[Manufactured]
  ↓ MNO/MVNE provisions on HLR
[Stocked]                    — at warehouse / partner channel
  ↓ partner sells to customer; activate
[Active]
  ↓ customer suspends / non-payment
[Suspended]
  ↓ customer reactivates / pays
[Active]
  ↓ customer terminates / number portable out
[Terminated]
  ↓ regulatory cool-off (90 days minimum per VN)
[Recyclable]
  ↓ ICCID/IMSI reused for new customer (typically 6-12 mo later)
```

## State transitions — events

- `SIM_REGISTERED` — issued ICCID stored in inventory.
- `SIM_SHIPPED` — sent to channel partner.
- `SIM_ACTIVATED` — customer registers + KYC + activate on HLR.
- `SIM_SUSPENDED` — payment fail / fraud / customer request.
- `SIM_REACTIVATED` — payment cleared / fraud cleared.
- `SIM_TERMINATED` — closed; balance refunded if prepaid.
- `MNP_OUT` — number ported away; SIM locked.
- `SIM_RECYCLED` — ICCID/IMSI reusable.

## Anti-patterns

- Reusing ICCID/IMSI without 6+ months cool-off → previous customer SMS/calls hit new customer.
- Terminating without releasing MSISDN → number stays orphan.
- Activating without KYC verification → regulatory non-compliance (Nghị định 49/2017 VN).
