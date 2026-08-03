# plans-kanban Migration

`fis:plans-kanban` no longer runs a standalone server.

## What changed

- Old behavior: `plans-kanban` started its own HTTP server, renderer, assets bundle, and PID-managed background process.
- New behavior: `plans-kanban` connects to the DAI dashboard at `http://localhost:3456/plans`.

## Current workflow

```bash
node .claude/skills/fis-plans-kanban/scripts/open-dashboard.cjs
```

Start DAI before running the launcher.

## Legacy flag mapping

| Legacy usage | Replacement |
|-------------|-------------|
| `--dir ./plans` | No replacement needed. Dashboard auto-discovers plans. |
| `--plans ./plans` | No replacement needed. |
| `--port 3500` | Use the integrated dashboard default: `3456`. |
| `--host 0.0.0.0` | Configure the dashboard host in DAI. |
| `--background` | No replacement; DAI owns its process. |
| `--foreground` | No replacement; DAI owns its process. |
| `--stop` | Close the dashboard from DAI. |

## DAI

Use DAI settings to configure the local dashboard host and port.

Create and update plan files using Write/Edit tools. Update phase status by editing `plan.md` directly.
