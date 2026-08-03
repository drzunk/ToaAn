# Invoicing

Generate the customer-facing invoice from rated events at the end of a billing cycle.

## Cycle structure

- Cycle anchor day per subscriber (e.g. day 1, 5, 15, 25).
- Window: from previous cycle anchor (exclusive) to current anchor (inclusive).
- Invoice issue date: anchor day or anchor + N (where N is processing time, typically 1-3 days).
- Due date: invoice issue + payment terms (e.g. 15 days).

## Steps

1. Aggregate rated events for the cycle window per subscriber.
2. Apply recurring charges (plan fee, addon fees, equipment lease).
3. Compute one-time charges (activation fees, late penalty if any).
4. Apply credits (promotions, refund from previous cycle).
5. Compute taxes — VAT per service category.
6. Apply rounding per accounting policy (usually whole VND, half-up).
7. Generate invoice number (sequential, regulator-mandated format).
8. Persist invoice + line items.
9. Generate PDF + e-invoice XML (HOADON gateway, see `claude/skills/fis-fintech-vn/references/e-invoice-hoadon.md`).
10. Send to customer (email / SMS link / app push / paper mail).

## Proration

Mid-cycle changes (plan upgrade, late activation, mid-cycle suspension) require pro-rata adjustment:

```
prorated_amount = full_amount × (active_days / cycle_days)
```

## VN-specific

- Hóa đơn điện tử (e-invoice) mandatory since 2022.
- Sequential invoice number must comply with Tổng cục Thuế format (ký hiệu mẫu, ký hiệu hóa đơn).
- VAT rate: V1=10% standard, V3=8% reduced (Nghị quyết 110/2023/QH15 — temporary 2024-2026).

## Anti-patterns

- Async generation without atomic invoice number assignment → duplicate / gap in sequence.
- Re-issuing invoice without `hóa đơn thay thế` flow → audit fail.
- Computing tax before discount → wrong amount; tax applies to NET.
