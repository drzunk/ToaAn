---
name: fis:bootstrap
description: "Bootstrap new projects with research, tech stack, design, planning, and implementation. Modes: full (interactive), auto (default), fast (skip research), parallel (multi-agent)."
category: utilities
keywords: [scaffold, project, setup, boilerplate]
license: MIT
argument-hint: "[requirements] [--full|--auto|--fast|--parallel]"
metadata:
  author: fis-ai-kit
  version: "1.0.0"
---

# Bootstrap - New Project Scaffolding

End-to-end project bootstrapping from idea to running code.

**Principles:** YAGNI, KISS, DRY | Token efficiency | Concise reports

## Usage

```
/fis-bootstrap <user-requirements>
```

**Flags** (optional, default `--auto`):

| Flag | Mode | Thinking | User Gates | Planning Skill | Craft Skill |
|------|------|----------|------------|----------------|------------|
| `--full` | Full interactive | Ultrathink | Every phase | `--hard` | (interactive) |
| `--auto` | Automatic | Ultrathink | Design only | `--auto` | `--auto` |
| `--fast` | Quick | Think hard | None | `--fast` | `--auto` |
| `--parallel` | Multi-agent | Ultrathink | Design only | `--parallel` | `--parallel` |

**Example:**
```
/fis-bootstrap "Build a SaaS dashboard with auth" --fast
/fis-bootstrap "E-commerce platform with Stripe" --parallel
```

## Workflow Overview

```
[Git Init] → [Research?] → [Tech Stack?] → [Design?] → [Planning] → [Implementation] → [Test] → [Review] → [Docs] → [Onboard] → [Final]
```

Each mode loads a specific workflow reference + shared phases.

## Mode Detection

If no flag provided, default to `--auto`.

Load the appropriate workflow reference:
- `--full`: Load `references/workflow-full.md`
- `--auto`: Load `references/workflow-auto.md`
- `--fast`: Load `references/workflow-fast.md`
- `--parallel`: Load `references/workflow-parallel.md`

All modes share: Load `references/shared-phases.md` for implementation through final report.

## Step 0: Git Init (ALL modes)

Check if Git initialized. If not:
- `--full`: Ask user if they want to init → `git-manager` subagent (`main` branch)
- Others: Auto-init via `git-manager` subagent (`main` branch)

## Skill Triggers (MANDATORY)

After early phases (research, tech stack, design), trigger downstream skills:

### Planning Phase
Activate **fis:plan** skill with mode-appropriate flag:
- `--full` → `/fis-plan --hard <requirements>` (thorough research + validation)
- `--auto` → `/fis-plan --auto <requirements>` (auto-detect complexity)
- `--fast` → `/fis-plan --fast <requirements>` (skip research)
- `--parallel` → `/fis-plan --parallel <requirements>` (file ownership + dependency graph)

Planning skill outputs a plan path. Pass this to craft.

### Implementation Phase
Activate **fis:craft** skill with the plan path and mode-appropriate flag:
- `--full` → `/fis-craft <plan-path>` (interactive review gates)
- `--auto` → `/fis-craft --auto <plan-path>` (skip review gates)
- `--fast` → `/fis-craft --auto <plan-path>` (skip review gates)
- `--parallel` → `/fis-craft --parallel <plan-path>` (multi-agent execution)

## Role

Elite software engineering expert specializing in system architecture and technical decisions. Brutally honest about feasibility and trade-offs.

## Critical Rules

- Activate relevant skills from catalog during the process
- Keep all research reports ≤150 lines
- All docs written to `./docs` directory
- Plans written to `./plans` directory using naming from `## Naming` section
- DO NOT implement code directly — delegate through planning + craft skills
- Sacrifice grammar for concision in reports
- List unresolved questions at end of reports
- Run `/fis-journal` to write a concise technical journal entry upon completion

## References

- `references/workflow-full.md` - Full interactive workflow
- `references/workflow-auto.md` - Auto workflow (default)
- `references/workflow-fast.md` - Fast workflow
- `references/workflow-parallel.md` - Parallel workflow
- `references/shared-phases.md` - Common phases (implementation → final report)
