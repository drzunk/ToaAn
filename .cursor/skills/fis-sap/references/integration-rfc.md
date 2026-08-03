# Integration — RFC

## When to use

- Legacy synchronous integration with non-SAP system.
- SAP-to-SAP for non-IDoc data shapes.

## Variants

| Variant | Use |
|---|---|
| sRFC | synchronous (caller waits) |
| aRFC | async with callback |
| tRFC | transactional (queued, retry on fail) |
| qRFC | queued (FIFO order) |

## RFC destination (SM59)

- Logical name + connection type (3=ABAP, T=TCP/IP, H=HTTP, G=external SOAP)
- Authentication: trusted RFC (recommended for SAP-to-SAP), user/pass (legacy)

## Anti-patterns

- Hardcoded RFC destination in code → blocks transport between landscapes.
- sRFC for high-volume batch — should use tRFC + queue.
- Trusted RFC without strict role mapping → privilege escalation risk.
