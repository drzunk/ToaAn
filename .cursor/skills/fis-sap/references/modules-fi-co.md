# Modules — Financial Accounting (FI) + Controlling (CO)

## FI scope

- Sổ cái (G/L), AR (Accounts Receivable), AP (Accounts Payable), Asset Accounting
- Posting period control (open / close)
- Document split per profit center (mandatory in S/4HANA)
- Tax code mapping per country (Vietnam VAT: V1=10%, V2=5%, V0=0%, V3=8% reduced 2024-2026)

## CO scope

- Cost center / Profit center hierarchy
- Internal order, project (CO/PS integration)
- Budget vs actual reporting
- Allocations (assessment, distribution)

## BA touchpoints

- Posting date validation (must fall in open period).
- Vendor invoice 3-way match (PO + GR + IR) — see `modules-mm.md`.
- Tax determination at PO line item (per material × supplier × plant).
- Document split rules (config in OBBH).

## Reports common at FIS

- Báo cáo thuế GTGT — VN tax declaration (Thông tư 200).
- Sổ cái chi tiết (GL detail) — local format.
- Báo cáo cash-flow — VAS structure.

## Anti-patterns

- Posting outside open period → use OB52 carefully; never bypass document split config.
- Currency translation without rate type → leads to FX P/L exposure.
