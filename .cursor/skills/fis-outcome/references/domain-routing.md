# Domain Routing

When to activate Tier 2 and domain pack skills during an `/fis-outcome` workflow.

## Decision Rule

Default: use only Tier 1 nucleus skills. Pull in Tier 2 or domain skills **only when the outcome genuinely needs them**.

## Tier 2 Activation Triggers

| Signal | Skill to Activate |
|--------|-------------------|
| Outcome involves unclear or contested requirements | `/fis-elicit` → `/fis-requirements` |
| Outcome requires a non-trivial architectural decision (ADR) | `/fis-architecture` |
| Outcome is exploratory — multiple valid approaches | `/fis-brainstorm` |
| Outcome depends on undocumented external systems | `/fis-research` |
| Outcome touches backend services | `/fis-backend-development` |
| Outcome touches frontend components | `/fis-frontend-development` |
| Outcome involves database schema changes | `/fis-databases` |
| Outcome involves deployment or infrastructure | `/fis-devops` / `/fis-deploy` |
| Outcome includes a security-sensitive surface | `/fis-security-scan` |
| Outcome needs web/browser test coverage | `/fis-web-testing` |
| Code search across many files | `/fis-docs-seeker` |
| MCP server needed for this outcome | `/fis-use-mcp` |
| Preview required | `/fis-preview` |
| Git worktree isolation needed | `/fis-worktree` |

## Domain Pack Activation Triggers

Activate a domain pack skill when the outcome explicitly targets that domain:

| Domain | Skills |
|--------|--------|
| SAP ERP | `fis-sap` (enterprise-apps pack) |
| Government HR | `fis-ehrp` (enterprise-apps pack) |
| Telecom/BSS billing | `fis-bss-billing`, `fis-mvno`, etc. (telecom-billing pack) |
| Utility billing | `fis-utility-billing` (telecom-billing pack) |
| Vietnam finance / locale | `fis-fintech-vn`, `fis-vn-locale` (vietnam-market pack) |
| UI/brand/design work | visual-design pack skills |
| AI/agent automation | ai-automation pack skills |

## Never Activate by Default

The following skills should only appear when explicitly triggered:
- Any domain pack skill (enterprise, telecom, vietnam-market, etc.)
- `/fis-team` — only when outcome genuinely needs multiple independent parallel workstreams
- `/fis-scenario` — only when edge-case coverage is explicitly required
- `/fis-ask` — only when the outcome needs an expert Q&A session
