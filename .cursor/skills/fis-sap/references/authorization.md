# Authorization

## Auth object hierarchy

- **Auth class** (e.g. `FI`, `MM`) groups objects.
- **Auth object** (e.g. `S_TCODE`, `F_BKPF_BUK`) — actual check point.
- **Field × value** — specific check (e.g. `BUKRS = 1000`).

## Common objects

| Object | Use |
|---|---|
| `S_TCODE` | execute T-code |
| `S_RFC` | call RFC (function group level) |
| `S_DEVELOP` | ABAP development access (sensitive!) |
| `F_BKPF_BUK` | post FI document per company code |
| `M_MATE_MAR` | material master per material type |

## Role design

- Single role = task (e.g. "AP clerk", "GR poster").
- Composite role = job position (multiple single roles).
- Derived role = template + org-level restriction (per plant / per company code).

## VN-specific

- Segregation of duties (SoD) for SOX-equivalent VN compliance:
  - GL posting vs payment vs reconciliation must be different roles.
  - PO creation vs PO release vs GR must be different.

## Anti-patterns

- `SAP_ALL` in prod → reject unconditionally.
- Assigning T-code via custom transaction without auth check → exposure.
- Wide-open S_RFC (`*`) → external attacker can call any FM.
