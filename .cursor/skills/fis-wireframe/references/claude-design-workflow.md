# Claude Design Workflow (Figma) — default wireframe path

Drive UI design through prompts using the claude.ai **Figma** integration. Richest path: real design surfaces + design-to-code handoff. Advisory — stakeholder owns approval.

## Prerequisites

- Figma MCP available in the session (claude.ai Figma).
- Activate the Figma design skill first: `/figma-generate-design` (MANDATORY before `use_figma`). Fallback: `skill://figma/figma-generate-design/SKILL.md`.

## Inputs

1. **Source artifact** — feature description, Story (`US-NNNN`), or feature-spec (`FS`). Extract screen list + primary flow.
2. **Theme tokens** — from `docs/design/design.md` (Step 0 of SKILL.md). Map them into the prompt:

| design.md token | Figma prompt phrase |
|---|---|
| `colors.primary` / brand | "primary/brand color `<hex>`" |
| `colors.surface`, `colors.text` | "surface `<hex>`, text `<hex>`" |
| `typography.font`, scale | "font `<family>`, type scale `<…>`" |
| `spacing.unit`, radius | "spacing unit `<n>px`, corner radius `<n>px`" |
| `mode` (light/dark) | "light/dark theme" |

If no `design.md`, state the defaults you're using in the prompt so they're explicit.

## Prompt recipe

```
Design <N> screens for <feature/US-id>: <screen 1>, <screen 2>, …
Brand color <hex>, surface <hex>, text <hex>; font <family>; spacing <n>px; radius <n>px.
<light|dark> theme, mobile-first, WCAG-AA accessible.
Include states: empty, loading, error where relevant.
Lay them out as a clickable flow: <flow order>.
```

## Iterate

Refine with targeted follow-ups — "tighten dashboard spacing", "add an empty state to the list", "make the primary CTA more prominent". Keep each prompt scoped to one change.

## Handoff

- **Stakeholder review** — share the Figma link. Embed in DDD §II Mockup by linking (not inlining).
- **Implementation** — pull design context (`get_design_context`) and hand to `/fis-frontend-development`. Map Figma components to code via Code Connect when available.

## When to switch paths

- No Figma access, want fast Tailwind/HTML → `/fis-stitch` (Path B).
- Need a zero-dependency offline click-through → `--html` (Path C, `references/html-workflow.md`).
