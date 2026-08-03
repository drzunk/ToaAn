# Module — Materials Management (MM)

## Scope

- Vendor master, material master, info record, source list
- Purchase Order (PO) lifecycle: requisition → PO → goods receipt (GR) → invoice receipt (IR)
- Inventory: stock types (101 nhập, 261 xuất sản xuất, 411 transfer), stock posting (MIGO)
- Release strategy (PO approval workflow per amount × material group)

## Master data keys

| Object | Key | Notes |
|---|---|---|
| Vendor | LIFNR (10 chars) | per company code (LFB1) — fields differ |
| Material | MATNR (18 chars) | per plant (MARC) — fields differ |
| Info record | EINA + EINE | vendor × material × plant |
| Source list | EORD | per material × plant — controls allowed vendors |

## BA touchpoints

- 3-way match invariants — fail if PO total ≠ GR total ≠ IR total within tolerance.
- Movement type catalog — must be authorized per role.
- Tax determination — per material × vendor combination.

## Anti-patterns

- Direct stock posting (MB1A) bypassing PO — kills audit trail.
- Negative stock allowed by config — reconcile pain at month-end.
