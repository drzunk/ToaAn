# fis:wireframe changelog

## v4.1.0 — 2026-07-16

- Output location moved from `artifacts/` to `docs/` (`docs/design/design.md`, `docs/wireframes/WF-NNNN/*`) to align with the outcome-first layout.

## v1.0.0 — 2026-05-05

- Initial release. 4 fidelity tiers: html (default) / mermaid-flow / ascii / figma.
- HTML primary: Tailwind v4 CDN + shadcn/ui inline + Lucide icons. Self-contained `.html` render trong Claude Cowork artifact panel.
- 6 HTML patterns: form / list / detail / dashboard / modal / navbar.
- ASCII fallback DSL với box-drawing characters.
- Mermaid user flow + state machine conventions.
- Figma MCP escalation cho stakeholder review.
- **Design config support:** đọc `artifacts/design/design.md` để áp dụng theme/colors/typography/density. `init-design` mode copy template.
- Output `WF-NNNN.{html,md}` (HTML + Markdown companion với code embedded).
- Cross-integration: invoked từ `/fis-outcome` cho §II Mockup, từ `/fis-plan` cho ui_story.
