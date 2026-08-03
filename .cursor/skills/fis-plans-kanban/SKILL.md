---
name: fis:plans-kanban
description: Open the DAI plans dashboard. Use for plan kanban views, progress tracking, timeline checks, and quick navigation into plan files.
category: dev-tools
keywords: [plans, dashboard, kanban, progress, timeline]
argument-hint: "[deprecated flags are accepted with warnings]"
metadata:
  author: fis-ai-kit
  version: "2.0.0"
---

# plans-kanban

Thin connector for the DAI plans dashboard.

It opens a compatible running dashboard at `http://localhost:3456/plans` instead of starting a server.
If `3456` is already in use, it probes `3457-3460` for the desktop dashboard.

## Quick Start

```bash
node .claude/skills/fis-plans-kanban/scripts/open-dashboard.cjs
```

Start DAI first. The launcher opens the plans route only after detecting a compatible local dashboard.

## Purpose

Use this skill when you want the visual plans dashboard for:
- Multi-plan kanban and grid views
- Timeline and progress overview
- Navigating into `plan.md` and `phase-*.md` files
- Quick visibility into active vs completed work

Scope note:
- Project dashboards should show project-scoped plans only.
- Global dashboards should show global-scoped plans only.
- Read `plan.md` directly as the authoritative dependency/status view for `blockedBy` / `blocks`; `plans-kanban` is a launcher, not the source of cross-scope dependency truth.
- The generic `/plans` route defaults to `plans` unless a `dir` query param is already present; scope-aware plan roots come from the project/global dashboard context, not from deprecated launcher flags.

## Dashboard Workflow

```bash
# Open the plans dashboard
node .claude/skills/fis-plans-kanban/scripts/open-dashboard.cjs
```

Primary URL:

```text
http://localhost:3456/plans
```

## Deprecated Compatibility

The old standalone server flags are accepted for compatibility and replaced with guidance:

| Legacy input | Current behavior |
|-------------|------------------|
| `--dir <path>` / positional path | Warns and ignores. This launcher always opens the generic `/plans` route; it does not choose a custom plan root. |
| `--plans <path>` | Warns and ignores. |
| `--port <n>` | Warns and ignores. `plans-kanban` probes the desktop dashboard on `3456-3460`. |
| `--host <addr>` | Warns and ignores. Configure the host in the desktop application. |
| `--background` / `--foreground` | Warns and ignores. The launcher does not manage the desktop process. |
| `--stop` | Warns and directs you to close the desktop application. |
| `--open` | Accepted. Opening is now the default behavior. |

## Desktop Application

Open DAI and enable its local plans dashboard before running the launcher.

Read `plan.md` directly to inspect plan progress and update phase status.

## Requirements

### Desktop Compatibility

The launcher performs a capability probe before opening the browser.
The dashboard at `/plans` is only opened when the running desktop application supports it — detected by either:

- `/api/health` response containing `"plans-dashboard"` in its `features` array, or
- `/api/plans` responding with a 2xx status (backward-compat for early dev builds).

If neither probe succeeds, the launcher prints an upgrade message and exits with code 1 without opening the browser. Upgrade the desktop application to a build that exposes the plans-dashboard capability.

## Migration Notes

The legacy standalone server, renderer, and assets have been retired from this skill.

For migration details:

```text
.claude/skills/fis-plans-kanban/deprecated/MIGRATION.md
```

## Troubleshooting

**Dashboard is not running**
Start DAI, then run the launcher again.

**Dashboard did not open**
Confirm DAI exposes `/api/health` and the `plans-dashboard` feature, then open `/plans` on its local port.

**Need to stop the dashboard**
Close it from DAI.

**Need custom host or different port**
Configure DAI. The launcher intentionally probes only local ports `3456-3460`.
