# VN phone number format

## Mobile (di động)

- Country code: `+84`.
- Total length after country code: **9 digits**.
- Carrier prefixes (1st digit after `+84`):
  - Viettel: 3, 5, 7, 8, 9 (most ranges)
  - Vinaphone: 8, 9
  - MobiFone: 7, 8, 9
  - Vietnamobile: 5
  - Gmobile: 5, 9 (small)
  - Itelecom: 8, 7

Old (pre-2018) prefixes started with 0 + 10 digits total. Migration: dropped first `0` → `+84` + 9.

## Landline (cố định)

- Country code: `+84`.
- Total length after country code: **9 digits**.
- Format: `+84 + area-code (1-3 digits) + subscriber (6-7 digits)`.
- Example: `+84 24 3823 4567` (Hà Nội), `+84 28 3822 5678` (TP.HCM).

## Display formats

| Format | Example |
|---|---|
| E.164 (storage) | `+84912345678` |
| International dialing | `+84 91 234 5678` or `+84 (091) 234 56 78` |
| Local dialing | `091 234 5678` (drop +84, prepend 0) |

## Validation

```typescript
function isValidVNMobile(phone: string): boolean {
  // Accept E.164, local, or 10-digit-with-zero
  const cleaned = phone.replace(/[\s()-]/g, '');
  return /^(\+84|84|0)(3|5|7|8|9)\d{8}$/.test(cleaned);
}

function toE164(phone: string): string {
  const cleaned = phone.replace(/[\s()-]/g, '');
  if (cleaned.startsWith('+84')) return cleaned;
  if (cleaned.startsWith('84')) return '+' + cleaned;
  if (cleaned.startsWith('0')) return '+84' + cleaned.slice(1);
  return '+84' + cleaned;
}
```

## Anti-patterns

- Storing phones in mixed formats (some `0...`, some `+84...`) → dedup fails.
- Displaying `+84...` to VN users → confusing; localize to `0...`.
- Allowing 11 digits (legacy) → no longer valid since 2018 migration.
