# `--init-design` workflow

Scaffold `docs/design/design.md` — the theme token source-of-truth. Read by all WF-NNNN folders to keep prototypes consistent.

## Output

`docs/design/design.md` with frontmatter:

```yaml
---
type: design-tokens
project: <Project Name>
last_updated: YYYY-MM-DD
---
```

## Token sections

```markdown
## Brand colors

| Token | Hex | Use |
|---|---|---|
| primary | #1F4E79 | header, primary buttons |
| secondary | #2E5597 | secondary actions |
| accent | #F59E0B | highlights |
| success | #10B981 | success states |
| warning | #F59E0B | warning states |
| error | #DC2626 | error states |
| text-primary | #1E293B | body text |
| text-secondary | #64748B | helper text |
| surface | #FFFFFF | cards |
| background | #F8FAFC | page background |

## Typography

- Sans-serif: Inter, system-ui, -apple-system, sans-serif
- Mono: ui-monospace, "JetBrains Mono", Consolas, monospace
- Base size: 14px desktop / 16px mobile
- Heading scale: 1.25 modular

## Spacing

8-pt grid: 4 / 8 / 12 / 16 / 24 / 32 / 48 / 64 px.

## Component patterns

Reference `claude/skills/fis-wireframe/references/shadcn-component-map.md` for inlinable component recipes.
```

After scaffolding, the user fills in project-specific tokens. The HTML prototype generation extracts these tokens into `WF-NNNN/shared/styles.css`.
