# CCCD / CMND / MST validation

## CCCD (Căn cước công dân) — 12 digits

Format: `pppyysssssss` where:
- `ppp` (3 digits) = mã tỉnh / thành phố (per Bộ Công an).
- `yy` (2 digits) = giới tính + thế kỷ sinh:
  - `0`-`1` = nữ XX, `2`-`3` = nam XX (sinh 1900-1999); `0`-`1` nam XX1 sinh 2000+, ...
- `sssssss` (7 digits) = số ngẫu nhiên.

Validation:
- Length exactly 12 digits.
- `ppp` must be in valid province codes table (1-99 + special codes).
- Check sum: no algorithmic checksum like Luhn — must validate against Bộ Công an database for hard checks.

## CMND cũ — 9 digits

Legacy ID before 2016. Still valid until expiry.

Validation:
- Length exactly 9 digits.
- First 3 digits = mã tỉnh.
- No checksum.

## MST (Mã số thuế) — 10 or 13 digits

- 10-digit: parent tax code (cá nhân, tổ chức).
- 13-digit: parent + 3-digit branch suffix (chi nhánh).

Format: `nnnnnnnnnn-bbb` (visually with dash; stored as 13-char string without dash).

Validation:
- Length 10 or 13.
- Check digit at position 10:
  ```
  W = [31, 29, 23, 19, 17, 13, 7, 5, 3]
  sum = Σ digit_i × W_i for i = 1..9
  cd = 10 - (sum mod 11)
  if cd == 10 → invalid
  digit_10 = cd
  ```

## Code reference

```typescript
function isValidMST(mst: string): boolean {
  const cleaned = mst.replace(/[-\s]/g, '');
  if (!/^\d{10}(\d{3})?$/.test(cleaned)) return false;
  const W = [31, 29, 23, 19, 17, 13, 7, 5, 3];
  let sum = 0;
  for (let i = 0; i < 9; i++) sum += parseInt(cleaned[i], 10) * W[i];
  let cd = 10 - (sum % 11);
  if (cd === 10) cd = 0;
  return cd === parseInt(cleaned[9], 10);
}
```

## Anti-patterns

- Validating CCCD by length only → accepts random digits.
- Storing CCCD plain text in DB → PII breach (NĐ 13/2023). Encrypt at rest (AES-256 column-level).
- Logging full MST in app logs → audit fail. Mask middle digits.
