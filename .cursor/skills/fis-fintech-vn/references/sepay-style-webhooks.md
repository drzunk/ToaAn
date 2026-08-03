# SePay-style webhook (bank-statement bridge)

SePay (sepay.vn), Casso, OnPay are gateways that bridge bank-account-statement → webhook for the merchant.

## How it works

```
1. Merchant authorizes gateway with read-only access to bank statement (e-banking credentials or bank API).
2. Gateway polls / receives statement updates.
3. For each new transaction, gateway POSTs a webhook to merchant URL.
4. Merchant verifies signature, parses addInfo, matches order, marks paid.
```

## Webhook payload (representative)

```json
{
  "id": "<gateway-internal-id>",
  "gateway": "Vietcombank",
  "transaction_id": "FT24050712345",
  "transaction_date": "2026-05-07T14:32:00+07:00",
  "amount_in": 2450000,
  "amount_out": 0,
  "transaction_content": "FIS ORDER-001 - tu khach hang ABC",
  "reference_code": "<bank-side-reference>",
  "account_number": "0123456789",
  "signature": "<HMAC-SHA256>"
}
```

## Verification

- HMAC-SHA256 over payload using shared secret.
- Compare bank-side `reference_code` if available — strongest match.
- Match `addInfo` (transaction_content) regex-extract: `/\bFIS([A-Z0-9-]+)\b/`.
- Validate `amount_in` matches expected amount.

## Idempotency

- Gateway may retry on failure → idempotency key = `transaction_id`.
- Same `transaction_id` twice → ignore second; respond 200.

## Anti-patterns

- Trusting webhook without signature verification → fraud easy.
- Crediting before signature + amount validation → financial loss on race conditions.
- No DLQ for failed webhooks → manual reconciliation pain.
