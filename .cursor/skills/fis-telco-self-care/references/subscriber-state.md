# Subscriber state machine

State of a subscriber (the SIM × MSISDN × customer triple).

## States

```
NEW              ─ before activation
  ↓ activate (KYC + first top-up)
ACTIVE           ─ normal usage
  ↓ payment fail / customer request
SUSPENDED        ─ outgoing blocked; incoming may continue
  ↓ payment / customer reactivate
ACTIVE
  ↓ extended non-payment / customer terminate
TERMINATED       ─ services off; balance settled
  ↓ MNP-out
PORTED-OUT       ─ number active at another carrier
  ↓ regulatory cool-off (≥ 90 days)
RECYCLABLE       ─ MSISDN can be reassigned
```

## Triggers

| Event | From | To | Side-effects |
|---|---|---|---|
| `ACTIVATE` | NEW | ACTIVE | charge plan setup; provision HLR; SMS welcome |
| `SUSPEND_PAYMENT` | ACTIVE | SUSPENDED | block outgoing on HLR; SMS notice |
| `SUSPEND_FRAUD` | ACTIVE | SUSPENDED | block all on HLR; ticket |
| `REACTIVATE` | SUSPENDED | ACTIVE | unblock HLR; SMS confirm |
| `TERMINATE_REQUEST` | ACTIVE / SUSPENDED | TERMINATED | refund balance; deprovision HLR; close billing |
| `TERMINATE_NONPAY` | SUSPENDED | TERMINATED | write-off balance; deprovision HLR |
| `MNP_OUT` | ACTIVE | PORTED-OUT | release MSISDN; close billing |
| `COOL_OFF_DONE` | TERMINATED / PORTED-OUT | RECYCLABLE | scheduled job after 90+ days |
| `RECYCLE` | RECYCLABLE | NEW | new customer activates |

## VN-specific

- Nghị định 49/2017/NĐ-CP — KYC mandatory for activation.
- Nghị định 13/2023/NĐ-CP — PII retention rules apply to suspended/terminated subscribers.
- Cool-off period for MSISDN recycling: typically 6 months (carrier policy + Cục Viễn thông).

## Anti-patterns

- Reactivation without payment confirmation → revenue leak.
- Skipping cool-off → previous customer's SMS / calls hit new owner; privacy breach.
- Termination without billing finalization → orphan invoices.
