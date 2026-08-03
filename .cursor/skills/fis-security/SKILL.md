---
name: fis:security
description: "STRIDE + OWASP-based security audit with optional red-team persona discovery loop and auto-fix. Scans code for vulnerabilities from multiple attacker perspectives (auth attacker, supply chain, insider, infrastructure), categorizes by severity, and can iteratively fix findings using fis:autoresearch pattern. Advisory — reports findings; user owns remediation/acceptance."
category: utilities
keywords: [security, STRIDE, OWASP, audit, red-team, penetration-testing, vulnerability-discovery]
argument-hint: "<scope glob or 'full'> [--fix] [--red-team] [--iterations N]"
metadata:
  author: fis-ai-kit
  attribution: "Security audit pattern adapted from autoresearch by Udit Goenka (MIT)"
  license: MIT
  version: "1.0.0"
---

# fis:security — Security Audit

Runs a structured STRIDE + OWASP security audit on a given scope. Produces a severity-ranked findings report. With `--fix`, applies fixes iteratively using the fis:autoresearch guard pattern.

## When to Use

- Before a release or major deployment
- After adding auth, payment, or data-handling features
- Periodic security review (monthly/quarterly)
- Compliance check (SOC 2, GDPR, PCI-DSS prep)

## When NOT to Use

- Purely cosmetic changes (CSS, copy edits)
- No user-facing code or data handling involved

---

## Modes

| Mode | Invocation | Behavior |
|------|-----------|----------|
| Audit only | `/fis-security <scope>` | Scan → categorize → report (one-shot) |
| Red-team discovery | `/fis-security <scope> --red-team` | Iterate 4 attacker personas → STRIDE/OWASP sweep → report |
| Bounded red-team | `/fis-security <scope> --red-team --iterations N` | Cap persona discovery to N iterations total |
| Audit + Fix | `/fis-security <scope> --fix` | Scan → categorize → fix iteratively |
| Red-team + Fix | `/fis-security <scope> --red-team --fix` | Full persona discovery → fix confirmed Critical/High |
| Bounded fix | `/fis-security <scope> --fix --iterations N` | Limit fix iterations to N |

---

## Audit Methodology

### 1. Scope Resolution
Expand the provided glob or `full` keyword into a file list. Read all in-scope files before analysis.

### 2. STRIDE Analysis
Evaluate each threat category systematically:
- **S**poofing — identity/authentication weaknesses
- **T**ampering — input validation, integrity controls
- **R**epudiation — audit logging gaps
- **I**nformation Disclosure — data leakage, secret exposure
- **D**enial of Service — rate limits, resource exhaustion
- **E**levation of Privilege — broken access control, RBAC gaps

### 3. OWASP Top 10 Check
Map findings to OWASP categories (A01–A10). See `references/stride-owasp-checklist.md` for per-category checks.

### 4. Dependency Audit
Run the appropriate package audit tool for the detected stack:
- Node.js: `npm audit`
- Python: `pip-audit`
- Go: `govulncheck`
- Ruby: `bundle audit`

### 5. Secret Detection
Scan for hardcoded API keys, passwords, tokens, and private keys using regex patterns. See `references/stride-owasp-checklist.md` → Secret Patterns.

### 6. Finding Categorization
Assign each finding a severity level (see Severity Definitions below).

---

## Output Format

```
## Security Audit Report

### Summary
- Files scanned: N
- Findings: X critical, Y high, Z medium, W low, V info

### Findings

| # | Severity | Category | File:Line | Description | Fix Recommendation |
|---|----------|----------|-----------|-------------|-------------------|
| 1 | Critical  | Injection | api/users.ts:45 | SQL string concatenation | Use parameterized queries |
| 2 | High      | Auth      | auth/login.ts:12 | No rate limiting | Add express-rate-limit |
```

---

## Red-Team Discovery Mode (--red-team)

When `--red-team` is provided, the audit runs a **multi-persona iterative discovery loop** before (or instead of) the standard one-shot STRIDE/OWASP sweep. Each persona represents a distinct attacker mindset with its own threat model and probe targets.

### Persona Execution Order

1. **Security Adversary** — external hacker; auth bypass, injection, IDOR, privilege escalation
2. **Supply Chain Attacker** — dependency/CI poisoning; CVEs, unsigned artifacts, overly permissive CI
3. **Insider Threat** — compromised internal account; horizontal/vertical escalation, bulk export, audit gaps
4. **Infrastructure Attacker** — runtime/deployment foothold; SSRF, secrets in env, container misconfig

Each persona phase: select next untested attack vector → assume that attacker's mindset → probe code + trace data flows → validate with proof (file:line, scenario, impact) → log to `security-audit-results.tsv` with a `persona` column. Prior persona findings compound into later phases. After all 4 personas, a standard STRIDE/OWASP sweep fills remaining coverage gaps.

> See `references/red-team-personas.md` for the full persona catalog: threat models, attack vectors, per-persona probe checklists, discovery-loop phases, TSV schema, and coverage summary.

### Credential Hygiene (Mandatory)

All findings across every persona MUST mask secret values before logging. Never emit raw JWTs (`eyJ...`), 32+ char hex strings, AWS key prefixes (`AKIA`, `ASIA`), or connection strings with embedded passwords. Use `<REDACTED_TOKEN>`, `<REDACTED_PASSWORD>`, or reference the env var name only.

---

## Fix Mode (--fix)

When `--fix` is provided, apply fixes iteratively after the audit:

1. Sort all findings by severity (Critical → High → Medium → Low)
2. For each finding:
   a. Apply one targeted fix
   b. Run guard (tests or lint) to verify no regression
   c. Commit: `security(fix-N): <short description>`
   d. Advance to next finding
3. Stop early if guard fails — report the failure instead of proceeding
4. Uses `fis:autoresearch` guard pattern for regression prevention

> Tip: Use `--iterations N` to cap total fix iterations when scope is large.

---

## Severity Definitions

| Severity | Description | Suggested Priority (advisory) |
|----------|-------------|-------------|
| Critical | Exploitable now, data breach or RCE risk | Recommended to fix before release (user decides) |
| High | Exploitable with moderate effort, significant impact | This sprint |
| Medium | Limited exploitability or impact | Next sprint |
| Low | Theoretical risk, defense-in-depth improvement | Backlog |
| Info | Best practice suggestion, no direct risk | Optional |

---

## Integration with Other Skills

- Run after `fis:predict` when the security persona flags concerns
- Feed Critical/High findings into `fis:autoresearch --fix` for automated remediation
- Use `fis:scenario` with `--focus authorization` for deeper auth flow testing
- Pair with `fis:plan` to schedule Medium/Low findings as sprint tasks

---

## Example Invocations

```bash
# Audit API layer only
/fis-security src/api/**/*.ts

# Audit entire src/ and auto-fix, max 15 iterations
/fis-security src/ --fix --iterations 15

# Full codebase audit (no fix)
/fis-security full
```

---

See `references/stride-owasp-checklist.md` for the detailed per-category checklist and secret detection regex patterns.
