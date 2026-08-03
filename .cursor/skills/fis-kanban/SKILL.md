---
name: fis:kanban
description: "Alias launcher for the DAI plans dashboard. Use for visual plan boards, progress tracking, and quick navigation into plan files."
category: dev-tools
keywords: [kanban, plans, dashboard, progress, timeline]
argument-hint: "[deprecated plans path or flags]"
metadata:
  author: fis-ai-kit
  version: "2.0.0"
---

# Plans Dashboard Alias

`/fis-kanban` is now a thin alias for `/fis-plans-kanban`.

It no longer starts the retired standalone `plans-kanban` server. Instead, it opens the plans route exposed by the running DAI.

## Usage

```bash
node .claude/skills/fis-plans-kanban/scripts/open-dashboard.cjs
```

Legacy path and server flags are still accepted with warnings and redirected to the desktop dashboard flow.

## Current Behavior

- Opens the plans dashboard route from a running desktop application
- Probes local ports `3456-3460`
- Directs process management to the desktop application
- Warns instead of failing when deprecated standalone-server flags are used

## Recommended Command

```bash
node .claude/skills/fis-plans-kanban/scripts/open-dashboard.cjs
```

Read `plan.md` directly to inspect plan progress; edit Status column to update phase status.

## Note

For AI agent task orchestration boards that are not plan files, use the dedicated orchestration skills. This alias is now strictly for the plans dashboard.
