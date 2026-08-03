---
id: WF-{{NNNN}}
type: wireframe
fidelity: ascii
pattern: form
parent_artifact: "{{DDD-NNNN | US-NNNN}}"
version: 1
---

# WF-{{NNNN}}: {{Title}} (ASCII form)

## Mockup

```
+============================================+
| {{Header / breadcrumb}}                    |
+============================================+
|                                            |
|  {{Form Title}}                            |
|  ----------------------------------        |
|                                            |
|  {{Field 1}}: [{{Value or placeholder}}]  |
|  {{Field 2}}: [{{Value}}              ]   |
|  {{Field 3}}: [v {{Select option}}    ]   |
|  {{Field 4}}: [ ] {{Checkbox label}}      |
|                                            |
|  [v {{Field 5 select}}                ]   |
|                                            |
|  [!] {{Optional alert/hint}}               |
|  ----------------------------------        |
|                                            |
|              [Cancel]      [Submit]        |
+============================================+
```

## Components used

- 3 text inputs
- 1 dropdown select
- 1 checkbox
- 1 alert (warning)
- 2 buttons (Cancel + Submit)

## Notes

- English shorthand trong box (alignment lý do)
- Vietnamese full trong DDD prose section parent

## Reference

- ASCII DSL: `references/ascii-dsl.md`
- Pattern: form (single-step input/edit)
