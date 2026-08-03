---
name: fis:outcome
description: "Own one outcome end-to-end with a single human DRI. Use to frame, plan, implement, and verify a well-defined outcome against acceptance criteria and tests/CI."
category: utilities
keywords: [outcome, dri, acceptance-criteria, workflow, orchestration]
argument-hint: "[outcome description] [--fast|--parallel]"
metadata:
  author: fis-ai-kit
  version: "1.2.0"
---

# Outcome — One DRI, One Outcome

One human owns one outcome end-to-end. The system of record is the issue + PR + acceptance criteria + tests/CI.

**No BA→SA→DEV→QA relay. No mandatory artifact chain. No role handoffs.**

## Operating contract (read first)

This skill orchestrates a workflow. When invoked, **do not jump straight to producing the deliverable.**

1. **Always start at Step 1 (Frame). Never write code, files, or any deliverable before the outcome is framed** — even a single HTML page or a "quick" task.
2. **Emit the frame explicitly, in your reply:** one-sentence outcome, 3–5 acceptance criteria, out-of-scope, risk (R1–R4), and the chosen mode (`default` / `--fast` / `--parallel`). For anything above a trivial R1 change, **pause for DRI confirmation** before implementing.
   - **Acceptance criteria must have a home (issue/PR).** Keeping them chat-only is allowed *only* for a throwaway you flag as such and the user agrees — **never drop the issue/PR home silently, including in `--fast`.** If you cannot create the issue (e.g. wrong/absent Git CLI), say so and offer the fallback (see Step 1 + `references/frame-template.md`).
3. **Implement only through the nucleus skills** — `/fis-craft` or `/fis-fix` (frontend/UI work: `/fis-frontend-design`). Do not hand-roll the implementation outside the flow.
4. **"Done" requires verified acceptance criteria** (Step 4 Test, plus the mode's review/ship). Producing an artifact and saying "done" is not completion.
5. If the request is exploration or a throwaway prototype with **no scoped outcome**, say so and route to the right skill (see "When NOT to Use") instead of silently building.

If you skip framing and go straight to output, you are running the wrong flow.

## Principles

- One human DRI owns every decision and trade-off for the outcome.
- Acceptance criteria live in the issue / PR description — not a generated PRD.
- Tests and CI are the contract. Passing tests + accepted criteria = done.
- Pull in Tier 2 or domain skills when the outcome genuinely needs them; do not activate them by default.

## Usage

```
/fis-outcome <outcome description>
```

**Example:**
```
/fis-outcome "Add rate limiting to the payments API so abuse is caught before the daily cap"
/fis-outcome "Fix the session expiry bug reported in issue #421" --fast
```

## Modes

| Mode | Sequence | Allowed when |
|------|----------|--------------|
| default | full nucleus (Frame → Plan → Scout? → Implement → Test → Review → Ship) | any outcome |
| `--fast` | Frame → Implement → Test → Ship (Plan is an inline 3-line note; Scout only if unfamiliar; Review folds into Test as self-review) | Risk R1–R2 **and** ≤3 acceptance criteria **and** no public-contract/data-migration/security surface |
| `--parallel` | Plan first, then `/fis-craft --parallel` (or `/fis-team`) across phases | Plan has 3+ independent phases with no shared file ownership |

`--fast` and `--parallel` are mutually exclusive: `--parallel` requires a real plan, `--fast` skips it. If a `--fast` outcome turns out to be R3+ mid-flight, stop and rerun in default mode (see Failure Loop).

**`--fast` still frames.** It shortens the pipeline; it does **not** skip the frame, the acceptance-criteria home (issue/PR), or provider detection. It never drops the issue/PR home silently — at most it defers to chat-only with explicit user consent for a flagged throwaway.

## Workflow

### Step 1 — Frame

**This step is mandatory and comes before any implementation.** Output the frame in your reply as a short block (outcome, acceptance criteria, out-of-scope, risk, mode). For anything above a trivial R1 change, stop and get DRI confirmation before Step 2.

Clarify exactly one outcome with the DRI:

1. **One sentence:** What user or system problem does this solve?
2. **Acceptance criteria:** 3–5 concrete, testable criteria. Write them directly into the issue / PR as a checklist.
3. **Out of scope:** Explicitly name what is NOT included.
4. **Risk level (R1–R4):** Estimate blast radius. This value is not decorative — it drives the required gates below.

| Risk | Blast radius |
|------|--------------|
| R1 | Single file / isolated change |
| R2 | Single service |
| R3 | Multiple services or a shared internal contract |
| R4 | Public API, data migration, auth/security surface, or irreversible action |

If no issue/PR exists yet, create one first so the acceptance criteria have a home. Use `/fis-git` and **detect the provider from the git remote** — GitHub (`gh`) vs GitLab (`glab`, including self-hosted like `gitlab.fis.vn`); never assume GitHub. If the matching CLI is missing or unauthenticated, fall back to the prefilled issue URL or defer criteria into the PR (do not silently skip). Frame template, provider-aware creation, good vs bad criteria, and the checklist format: `references/frame-template.md`.

**Risk drives the gates** — consult `references/workflow-routing.md` for the Risk → Gate table (which of Scout, Architecture/ADR, Security scan, review depth, and DRI pre-approval become mandatory). Then consult `references/domain-routing.md` for Tier 2 / domain skills.

### Step 2 — Plan

Use `/fis-plan` to produce a scoped, phased plan.

- For complex outcomes: use `/fis-scout` first to map affected code, then plan.
- Acceptance criteria from Step 1 become the plan's success criteria.
- Do not create PRD/TRD/Story/TestSpec artifact chains by default.

### Step 3 — Implement

Use `/fis-craft` (feature work) or `/fis-fix` (bug/regression) to implement each phase.

Consult `references/workflow-routing.md` for nucleus sequencing and when to deviate.

### Step 4 — Test

Use `/fis-test` to run the test suite and confirm all acceptance criteria are covered by passing tests.

**Acceptance-criteria ↔ test traceability (anti "done-in-name-only"):** map every acceptance criterion to the test(s) that cover it. Any criterion with no covering test is a gap — add a test, or mark it explicitly untestable with a one-line DRI sign-off in the issue/PR. Record the mapping in the PR:

```
- [x] AC1 "rejects over daily cap" → test_ratelimit_daily_cap
- [x] AC2 "returns 429 with retry-after" → test_ratelimit_headers
```

If tests fail, follow the Failure Loop below (`/fis-debug` when the cause is unknown, then `/fis-fix`).

### Step 5 — Review

Use `/fis-code-review` to review the implementation against the acceptance criteria.

### Step 6 — Ship

Use `/fis-ship` to merge and close the outcome.

Mark the issue / PR resolved with evidence: passing CI link + acceptance criteria checked off in the PR description.

## Failure Loop

The workflow is not one-directional. When a step fails, loop back to the narrowest fixing step — do not silently retry the same action.

- **Test fails:** `/fis-debug` if the root cause is unknown, then `/fis-fix`, then re-test.
- **Review finds critical issues:** back to `/fis-craft` or `/fis-fix` for those findings only, then re-review.
- **Ship/CI fails:** `/fis-fix` on the CI failure, then re-run. Flaky or infra failures are escalated, not blindly retried.
- **Scope changes mid-outcome:** stop, return to Step 1, re-confirm acceptance criteria with the DRI. Never silently expand scope.

**Iteration cap:** after **3** fix attempts on the same failure without progress, stop and escalate to the DRI with what was tried and the current evidence. Blocked-by-external-decision or blocked-by-dependency also escalates immediately. Full loop-back table and escalation contract: `references/workflow-routing.md`.

## Resume (multi-session)

The issue/PR is the only state store — there is no separate progress file. To resume an in-flight outcome, read the issue/PR and derive the current step from its checklist:

| Signal in issue/PR | Current step |
|--------------------|--------------|
| No acceptance criteria written | Step 1 — Frame |
| Criteria written, no plan/branch | Step 2 — Plan |
| Branch/commits exist, tests not attached | Step 3–4 — Implement / Test |
| Test evidence attached, no review note | Step 5 — Review |
| Review approved, not merged | Step 6 — Ship |

## Routing

- See `references/workflow-routing.md` for nucleus sequencing, Risk → Gate table, mode behavior, and the Failure Loop / escalation contract.
- See `references/domain-routing.md` for when to activate Tier 2 or domain pack skills.
- See `references/frame-template.md` for acceptance-criteria templates and the issue/PR checklist format.

## When NOT to Use

- Exploratory research with no specific outcome → use `/fis-brainstorm` or `/fis-research`.
- Ongoing maintenance without a scoped outcome → use nucleus skills directly.
- Pure requirements discovery → use `/fis-elicit` then `/fis-requirements`.

## Workflow Position

**Precedes:** `/fis-plan` → `/fis-scout` → `/fis-craft` / `/fis-fix` → `/fis-test` → `/fis-code-review` → `/fis-ship`
**Related:** `/fis-requirements` (when explicit requirements capture is needed), `/fis-architecture` (when ADR-level decisions are needed)
