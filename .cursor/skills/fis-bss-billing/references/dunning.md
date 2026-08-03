# Dunning

Collection workflow when invoice is past due.

## Levels (typical)

| Level | Trigger | Action |
|---|---|---|
| **D0** | Invoice issued | Send copy via email / app |
| **D1** | 5 days past due | Reminder email + SMS |
| **D2** | 15 days past due | Stronger reminder + warn imminent suspension |
| **D3** | 21 days past due | Suspend outgoing services; allow incoming + emergency only |
| **D4** | 45 days past due | Full suspension (no service) |
| **D5** | 90 days past due | Terminate; transfer to collections / write-off review |

Each level → SMS / email / in-app notification + log to dunning history.

## Suspension semantics

- **Soft suspend** (D3): block outgoing, allow incoming + 911-equivalent + customer service hotline.
- **Hard suspend** (D4): all services off; SIM card status SUSPENDED on HLR.
- **Terminate** (D5): SIM removed from HLR; MSISDN held for 90-day cool-off then recyclable.

## Customer disputes

- Customer raises dispute → freeze dunning clock for that invoice during dispute window.
- Resolved in customer's favor → credit + clear past-due flag.
- Resolved against customer → resume dunning from where it paused.

## Reconnect

- Pay full past-due → unsuspend on next OCS / HLR sync (typically < 5 min).
- Reconnect fee may apply (per regulator + carrier policy).

## Write-off

- After D5 + N days (typically 90-180), write off remaining debt.
- Maintain customer history flag for "previously bad debt" — affects future credit decision.
- Audit trail mandatory.

## Anti-patterns

- D3 suspension before customer notification → regulator complaints.
- Reconnect without confirming payment cleared at bank → revenue leak.
- Write-off without explicit signoff → fraud risk.
