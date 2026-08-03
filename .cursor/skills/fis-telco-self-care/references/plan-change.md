# Plan change (đổi gói cước)

Customer changes from current tariff plan to a new one.

## Modes

| Mode | When | Effective |
|---|---|---|
| **Immediate** | upgrade with proration | now |
| **End-of-cycle** | downgrade or peer-change | next billing cycle start |
| **Scheduled** | future-dated | at scheduled date |

## API flow

```
1. GET /plans → list of available plans (filtered by eligibility per current customer)
2. POST /plans/quote { new_plan_id }
   → returns { proration, effective_date, total_due }
3. POST /plans/change { new_plan_id, mode } (with step-up auth)
   → returns { change_id, status: "scheduled" | "applied" }
4. (if mode=immediate) charge proration → apply new plan → confirm
5. Customer receives SMS / email / push: "Đổi gói thành công"
```

## Eligibility rules

- No outstanding debt.
- Within "fair-change" window (e.g. max 1 plan change per 30 days).
- Compatibility (some plans require specific SIM types).
- KYC complete.

## Proration math

Upgrade mid-cycle:
```
charge = (new_plan_price - current_plan_remaining_credit_value) × (days_left_in_cycle / cycle_days)
```

Most VN telcos charge full new plan from the change date and forfeit remaining old-plan balance — or refund pro-rata. Implementation depends on contract.

## Anti-patterns

- Plan change without quote step → customer surprised by charge.
- Changing plan with outstanding debt → invoice ambiguity.
- No audit trail of plan changes → regulator request can't be fulfilled.
