# NAPAS QR — VietQR

## What it is

NAPAS 247 QR (often called VietQR) — VN nationwide instant payment QR standard.

- Issued by NAPAS (National Payment Corporation of Vietnam).
- Conformant to **EMVCo QR standard** (TLV format).
- Supports static QR (fixed merchant) and dynamic QR (per-transaction with amount).

## QR payload structure (EMVCo TLV)

| Tag | Name | Value |
|---|---|---|
| 00 | Payload Format Indicator | `01` |
| 01 | Point of Initiation Method | `11` (static) / `12` (dynamic) |
| 38 | Merchant Account Information (NAPAS) | TLV nested: 00 = AID `A000000727`, 01 = beneficiary org BIN, 02 = account number / card |
| 52 | Merchant Category Code | per ISO 18245 |
| 53 | Currency | `704` (VND) |
| 54 | Transaction Amount | (dynamic only) |
| 58 | Country Code | `VN` |
| 59 | Merchant Name | |
| 60 | Merchant City | |
| 62 | Additional Data | TLV: 01 = bill #, 05 = ref label, 08 = purpose |
| 63 | CRC | CRC16-CCITT-FALSE |

## URL helper (NAPAS img CDN)

```
https://img.vietqr.io/image/<bank>-<account>-<template>.png?amount=<amount>&addInfo=<note>&accountName=<name>
```

Example:
```
https://img.vietqr.io/image/Vietcombank-0123456789-print.png?amount=2450000&addInfo=FIS+ORDER-001&accountName=CONG+TY+ABC
```

## Reconciliation note

When a customer scans VietQR and pays:
- The transaction lands in the merchant's bank account.
- A webhook (e.g. SePay, Casso, Onepay) notifies the merchant backend with parsed `addInfo`.
- Backend matches `addInfo` (transaction reference) to the order ID.
- Merchant marks order as paid.

Note: addInfo length is bounded (~25 chars practical). Use a short reference token like `FIS<order-id-suffix>` rather than UUIDs.

## Anti-patterns

- Generating QR with literal special chars (Unicode emoji, ₫) in `addInfo` — some banking apps strip them.
- Static QR for high-value transactions — no amount validation.
- Polling bank statements instead of webhook — slow + scrape-fragile.
