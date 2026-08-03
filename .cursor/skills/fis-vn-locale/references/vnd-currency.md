# VND currency

## Format

- Currency code: **VND** (ISO 4217).
- Symbol: **₫** (`U+20AB`) — placed after the number per VN convention.
- **No decimals** in everyday use; smallest unit is 1 VND but transactions round to 100 / 500 / 1000.

## Display

| Context | Format |
|---|---|
| Plain amount | `1.234.567 ₫` (period as thousand separator, dot or space, then ₫) |
| Banking statement | `1,234,567` (comma) — depends on bank |
| Compact | `1,2 tỷ` (10⁹), `12,5 triệu` (10⁶), `123 nghìn` (10³) |

Locale tag: `vi-VN`.

```javascript
new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(1234567);
// "1.234.567 ₫"
```

## Storage

- **Always integer** (no decimals). Type: `BIGINT` (max ~9.2 × 10¹⁸ — enough for trillions of VND).
- Never use `FLOAT` / `DOUBLE` for money — rounding errors.
- For multi-currency: store amount + currency code separately (`amount_vnd`, `amount_currency_iso`).

## Rounding

- Standard transactions: round to nearest 1 VND (no rounding usually).
- Bulk reports: round per company policy (often nearest 1.000 VND for management reports).
- Tax (VAT) calculation: `Math.round(base × rate)` — rounding to whole VND.

## Anti-patterns

- Floating-point money: `1.0 + 2.0 - 3.0 != 0` issues.
- Storing in cents (`× 100`) — VND is already integer; no need for cents abstraction.
- Mixing `VND` and `đ` / `dong` in display → use `₫` consistently.
