# E-invoice (Hóa đơn điện tử) — Vietnam

E-invoice is mandatory for most VN businesses since 1 July 2022 per Nghị định 123/2020/NĐ-CP and Thông tư 78/2021/TT-BTC.

## Format

- Standard: XML signed with company digital certificate.
- Two e-invoice types:
  - **Có mã của cơ quan thuế** — pre-validated by tax authority before sending to buyer.
  - **Không có mã** — sent directly buyer; reported to tax authority within deadline (next billing day).

Most non-banking businesses use "có mã".

## Lifecycle

```
1. Issue invoice (XML).
2. Sign with digital cert.
3. Submit to https://hoadondientu.gdt.gov.vn (Tổng cục Thuế gateway).
4. Receive cấp mã (assigned tax code) ← for "có mã" type.
5. Deliver to buyer (PDF + XML or just XML).
6. Buyer can verify on the GDT portal by code.
```

## Status

| Status | Meaning |
|---|---|
| Draft | not yet submitted |
| Sent | submitted to GDT |
| Accepted (cấp mã) | tax code assigned; legally valid |
| Rejected | GDT rejected; needs correction |
| Cancelled | post-issue cancellation; replacement issued |
| Replaced | superseded by new invoice |

## Reverse / replace

- "Hóa đơn thay thế" — issue new invoice that supersedes the old one (same or different total).
- "Hóa đơn điều chỉnh" — issue adjustment invoice (positive or negative delta).
- Either requires reason text.

## Integration providers

VAS-listed providers (must be authorized by GDT):

- Misa Meinvoice
- Viettel SInvoice
- VNPT Invoice
- BKAV eHoadon
- M-Invoice
- ...

Most FIS engagements integrate via one of these — standardised SOAP/REST APIs.

## Anti-patterns

- Issuing invoice without digital cert → invalid; auditor flags.
- Ignoring rejection notification → invoice not legally valid; buyer can't claim VAT.
- Hard-coding provider API contract → switching provider blocked.
