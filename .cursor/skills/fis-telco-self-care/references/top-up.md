# Top-up (nạp tiền)

Add balance to prepaid / postpaid account.

## Channels

| Channel | Latency | Reconciliation |
|---|---|---|
| App + bank transfer (NAPAS QR) | seconds | webhook from bank gateway |
| App + e-wallet (MoMo, ZaloPay, ViettelPay) | seconds | webhook |
| Card (visa/master) | seconds | gateway response |
| Voucher code (scratch card) | immediate (validate code) | offline upload from channel |
| Bank transfer manual | hours | end-of-day reconciliation |

## API flow (NAPAS QR top-up)

```
1. POST /topup/create { amount }
   → returns { topup_id, vietqr_url, expires_at }

2. Customer scans QR + pays via banking app.

3. Bank → webhook → POST /webhooks/napas
   { transaction_id, account, amount, addInfo: "FIS<topup_id>" }

4. Backend matches addInfo → topup_id
   → credit account balance
   → push notification "Nạp tiền thành công +X₫"
   → POST /topup/{id}/status returns "completed"

5. App polls or receives WebSocket → UI shows new balance.
```

## Idempotency

- Webhook may retry; idempotency key = bank transaction_id.
- Same transaction_id twice → ignore second.
- topup_id has TTL (e.g. 15 min); expired → reject and refund.

## Anti-patterns

- Crediting balance before bank webhook confirmation → fraud.
- Webhook handler not idempotent → double-credit on retry.
- No expiry on QR → customer pays old QR for old amount, expects current balance.
