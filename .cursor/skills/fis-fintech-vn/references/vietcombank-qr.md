# Vietcombank VietQR

Vietcombank-issued VietQR conformant to NAPAS 247 standard. See also `claude/skills/fis-vn-locale/references/napas-qr.md` for the underlying QR EMVCo TLV structure.

## Bank routing parameters

- BIN (NAPAS-assigned bank ID) for Vietcombank: `970436`.
- Account number format: 13 digits (newer accounts) or 8-10 digits (legacy).

## URL helper

```
https://img.vietqr.io/image/Vietcombank-<account>-<template>.png
   ?amount=<amount-VND>
   &addInfo=<reference>
   &accountName=<URL-encoded-name>
```

Templates: `compact` (mobile), `compact2` (desktop), `print` (high-res for receipts).

## Reconciliation

Vietcombank itself does not push webhooks directly to merchants. Common patterns:

- Use **third-party gateway** (SePay, Casso, Onepay) that scrapes / receives bank statements and forwards webhooks.
- Use **Vietcombank Cash Management Services (VCB CMS)** — direct integration with API contract for enterprise (XML-based, end-of-day batch).

## Reference token convention

For invoice matching, FIS standard prefixes the order ID:
```
addInfo = "FIS<order-id-suffix>"
```
Length-bounded (~25 chars). Use base36-encoded UUID suffix to fit.

## Anti-patterns

- Using full UUID in addInfo → truncated by some bank apps; reconciliation fail.
- Polling bank statements via screen-scrape → fragile, often breaks on UI updates.
- Trusting addInfo without amount + bank-side audit cross-check → fraud risk.
