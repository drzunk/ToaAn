---
id: WF-{{NNNN}}
type: wireframe
fidelity: ascii
pattern: list
parent_artifact: "{{DDD-NNNN | US-NNNN}}"
version: 1
---

# WF-{{NNNN}}: {{Title}} (ASCII list)

## Mockup

```
+============================================+
| {{Title}}                       [+ Add]    |
+============================================+
|                                            |
|  Search: [.................]  [v Filter]  |
|                                            |
+--------+--------+--------+----------+------+
| Col 1  | Col 2  | Col 3  | Status   |  •   |
+========+========+========+==========+======+
| Row A1 | Row A2 | Row A3 | [Approv] | [..] |
+--------+--------+--------+----------+------+
| Row B1 | Row B2 | Row B3 | [Pendin] | [..] |
+--------+--------+--------+----------+------+
| Row C1 | Row C2 | Row C3 | [Reject] | [..] |
+--------+--------+--------+----------+------+
|                                            |
|  Hiển thị 1-10 / 247    [<] 1 2 3 ... [>] |
+============================================+
```

## Components used

- Search input
- Filter dropdown
- Add button
- 4-column table
- Status badge column
- Action menu column ([..])
- Pagination

## Status badges

- `[Approv]` — green (Approved)
- `[Pendin]` — yellow (Pending)
- `[Reject]` — red (Rejected)
- `[Draft ]` — gray (Draft)

## Notes

- ASCII fallback only — convert HTML khi stakeholder review: `/fis-wireframe html --pattern=list`
- Truncate column labels nếu quá dài; expand trong prose

## Reference

- ASCII DSL: `references/ascii-dsl.md`
- Pattern: list (table với search/filter/pagination)
