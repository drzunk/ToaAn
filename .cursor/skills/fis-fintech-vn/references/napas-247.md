# NAPAS 247 — Inter-bank instant transfer

NAPAS 247 is the VN national instant inter-bank transfer rail. Settlement < 30 sec.

## How it differs from VietQR

- **VietQR** = QR encoding standard.
- **NAPAS 247** = the underlying transfer rail that actually moves money.
- Most VietQR scans trigger a NAPAS 247 transfer.

## Direct integration (enterprise)

For high-volume merchants, direct connection to NAPAS:

- API: ISO 8583 message format (legacy) or REST/JSON (newer).
- Authentication: TLS + mTLS + signed payload.
- Daily reconciliation file via SFTP.

Most FIS engagements use a payment gateway (PG) middleware (Onepay, NganLuong, VNPAY, MoMo) instead of direct NAPAS integration — easier to onboard, slightly higher fee.

## Settlement model

- T+0 for transfers below threshold (e.g. 500M VND).
- T+1 for above-threshold or cross-bank reconciliation.
- Failed transfer auto-reversed within 30 min.

## Fees

- ~ 1,000 - 11,000 VND per transfer (depends on amount band, payer-pays vs payee-pays).
- Some banks waive fees for in-bank transfers.

## Anti-patterns

- Treating NAPAS 247 transfer notification as immediate proof — verify via bank statement reconciliation.
- Hardcoding bank BIN list — refresh from NAPAS quarterly.
