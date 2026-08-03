# Module — Sales & Distribution (SD)

## Scope

- Customer master, condition records (pricing), credit management
- Sales order → outbound delivery → goods issue → billing
- Pricing procedure (V/08), output determination, returns flow

## Master data keys

| Object | Key | Notes |
|---|---|---|
| Customer | KUNNR | per sales area (KNVV) |
| Condition record | KSCHL | per pricing procedure |
| Output | NACE | per condition + medium |

## BA touchpoints

- Pricing procedure determination: sales area × customer pricing proc × document pricing proc.
- Tax classification: customer × material → tax code → percentage.
- Credit check at sales order, delivery, or both.

## VN-specific

- Phiếu xuất kho (PXK) — printed alongside delivery note.
- Hóa đơn điện tử triggered post-billing (BIL → e-invoice gateway).

## Anti-patterns

- Manual pricing override without audit trail.
- Free goods (TANN) without tax-code adjustment.
