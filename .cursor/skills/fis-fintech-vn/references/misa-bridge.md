# MISA accounting bridge

MISA (https://www.misa.vn) — leading VN accounting software (SME segment). FIS systems often need to push transactions to a customer's MISA SME for posting.

## Products commonly integrated

- **MISA SME.NET** — desktop accounting, has SQL Server / Access DB backend.
- **MISA AMIS Kế toán** — cloud accounting with REST API.
- **MISA Mimosa** — for nonprofits / government units.

## Integration patterns

| Pattern | Use |
|---|---|
| Direct DB write | rare, only with MISA partner agreement |
| Excel import (manual) | small volume, customer pulls daily file |
| API push (AMIS) | recommended for cloud customers |
| MISA Connector partner | mid-size; vendor-blessed |

## API push (AMIS)

REST API:
- Auth: OAuth 2.0 client_credentials.
- Voucher creation: POST `/v1/vouchers` with payload (Hóa đơn / Phiếu thu / Phiếu chi / Phiếu xuất kho).
- Status check: GET `/v1/vouchers/{id}/status`.

## Voucher types relevant

- Phiếu thu (PT) — cash receipt.
- Phiếu chi (PC) — cash payment.
- Phiếu nhập kho (PN) — goods receipt.
- Phiếu xuất kho (PX) — goods issue.
- Hóa đơn bán hàng — sales invoice.

## Reconciliation

- After push, query MISA voucher number; store in our DB as `misa_voucher_id`.
- Daily reconciliation: count and amount sum match between FIS DB and MISA report.

## Anti-patterns

- Pushing draft transactions to MISA before approval in FIS → MISA voucher created prematurely.
- No retry / dead-letter for MISA API failures → silent data loss.
- Treating MISA as source-of-truth → if FIS edits voucher, must propagate; conflict resolution unclear.
