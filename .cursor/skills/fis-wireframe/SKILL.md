---
name: fis:wireframe
description: "Wireframe / mockup guide for FIS engagements — drive UI design through prompts using Claude design (Figma MCP) as primary, Google Stitch (/fis-stitch) as alternative. Reads docs/design/design.md for theme tokens. Produces a Figma design (shareable link) or Stitch export; lightweight HTML prototype available as fallback. Use when a feature needs a clickable mockup for stakeholder review before implementation."
category: frontend
keywords: [wireframe, mockup, prototype, claude-design, figma, stitch, ui, design, click-through, fis]
license: MIT
argument-hint: "[feature|story|FS-id] [--figma|--stitch|--html] [--init-design]"
metadata:
  author: fis-ai-kit
  version: "4.1.0"
---

# Wireframe — Prompt-Driven Design

Produce a wireframe/mockup for a FIS feature by **prompting a design tool**, not by hand-generating files. Default path is **Claude design via the Figma MCP**; Stitch is a solid alternative. The kit is advisory — pick whichever fits the engagement; nothing is enforced.

## Step 0 — Theme tokens (read first)

If `docs/design/design.md` exists, read it and feed its tokens (colors / typography / spacing / brand) into the design prompt. If missing → ask whether to scaffold it (`/fis-wireframe --init-design`, see `references/init-design-workflow.md`) or proceed with sensible defaults. Never invent brand colors when project tokens exist.

## Path A (default) — Claude design via Figma

Use the claude.ai **Figma** integration to generate the design from a prompt. This is the richest path: real design surfaces, components, and design-to-code handoff.

1. Activate the Figma design skill (`/figma-generate-design`) — MANDATORY before calling `use_figma`.
2. Compose the prompt from: the source artifact (feature / story / FS-ID), the screen list / flow, and the `design.md` tokens. Example:
   > "Design <N> screens for <feature>: <screen list>. Use brand colors <…>, font <…>, spacing scale <…> from our design system. Mobile-first, accessible. Produce a clickable flow."
3. Generate in Figma; iterate via follow-up prompts ("tighten spacing on the dashboard", "add an empty state").
4. Share the Figma link with stakeholders. For implementation, hand off via Figma's design-to-code (`get_design_context`) into `/fis-frontend-development`.

See `references/claude-design-workflow.md` for the full prompt recipe + token-mapping table.

## Path B (alternative) — Stitch

If Figma isn't available or you want fast Tailwind/HTML output, route to **`/fis-stitch`** (Google Stitch AI design → Tailwind/HTML/DESIGN.md). Pass the same artifact + tokens. Stitch is great for rapid prototyping and exports code-ready markup.

## Path C (fallback) — static HTML prototype

Only when no design tool is available and a no-dependency click-through is needed, generate a static multi-screen HTML prototype under `docs/wireframes/WF-NNNN/`. The legacy generator references remain for this: `references/html-workflow.md`, `references/screen-template.md`, `references/index-router-pattern.md`, `references/tailwind-patterns.md`, `references/shadcn-component-map.md`.

## Routing

Parse `$ARGUMENTS`:
- `--init-design` → `references/init-design-workflow.md`
- `--stitch` → delegate to `/fis-stitch`
- `--html` → `references/html-workflow.md` (fallback path C)
- otherwise (or `--figma`) → `references/claude-design-workflow.md` (default path A)

## Output & handoff

- **Figma**: shareable link → embed in DDD §II Mockup by linking (not inlining).
- **Stitch**: exported markup + DESIGN.md.
- **HTML**: `docs/wireframes/WF-NNNN/index.html` (open in browser; deep-link individual screens).

**IMPORTANT:** This skill produces wireframes/mockups only — not production code. For implementation use `/fis-frontend-development`. Approval of the design is the stakeholder's/user's call (advisory).
