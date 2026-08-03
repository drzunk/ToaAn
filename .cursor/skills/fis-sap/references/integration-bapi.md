# Integration — BAPI

## When to use

- Sync transaction needed (response < 5 sec).
- Standard SAP function exists (don't reinvent).
- Caller is SAP itself or external system tolerant of sync latency.

## Pattern

```abap
DATA: lt_return TYPE bapiret2_t.
CALL FUNCTION 'BAPI_<OBJECT>_<ACTION>'
  EXPORTING ...
  TABLES return = lt_return.
" CRITICAL — without this, BAPI changes are NOT persisted
CALL FUNCTION 'BAPI_TRANSACTION_COMMIT' EXPORTING wait = 'X'.
```

## Common BAPIs at FIS

| BAPI | Use |
|---|---|
| BAPI_PO_CREATE1 | create PO |
| BAPI_GOODSMVT_CREATE | post goods movement |
| BAPI_INCOMINGINVOICE_CREATE | post AP invoice |
| BAPI_SALESORDER_CREATEFROMDAT2 | create sales order |
| BAPI_BUSPROCESSND_CREATEMULTI | CRM activity |

## Anti-patterns

- **Forgetting `BAPI_TRANSACTION_COMMIT`** — most common bug. Changes seem to work in dev, vanish in prod.
- Not checking `lt_return` for E/A type messages → silent failure.
- Using BAPI in a loop without intermediate commits → memory bloat.
