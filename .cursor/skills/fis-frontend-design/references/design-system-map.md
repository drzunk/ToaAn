# Design System Map

Once you have the design read and dials, pick the right foundation. Do not invent CSS for something that has an official package. Do not pretend an aesthetic trend is an official system.

## When to reach for a real design system (use official packages)

| Brief reads as… | Reach for | Why |
|---|---|---|
| Microsoft / enterprise SaaS / dashboards | `@fluentui/react-components` or `@fluentui/web-components` | Official Fluent UI, Microsoft tokens, accessibility done |
| Google-ish, Material-flavored product | `@material/web` + Material 3 tokens | Official, theme-able via Material Theming |
| IBM-style B2B / enterprise analytics | `@carbon/react` + `@carbon/styles` | Official Carbon, mature data-density patterns |
| Shopify app surfaces | Polaris (web components / React) | Required for Shopify admin UI |
| Atlassian / Jira-style product | `@atlaskit/*` + `@atlaskit/tokens` | Official Atlassian DS |
| GitHub-style devtool / community page | `@primer/css` or `@primer/react-brand` | Official Primer; Brand variant for marketing |
| Public-sector UK service | `govuk-frontend` | Legally / regulatorily expected |
| US public-sector / trust-first | `uswds` | Same |
| Fast local-business / agency MVP | Bootstrap 5.3 | Boring, fast, works |
| Modern accessible React foundation | `@radix-ui/themes` | Primitives + polished theme |
| Modern SaaS where you own components | shadcn/ui (`npx shadcn@latest add ...`) | You own the code; never ship default state |
| Tailwind-based modern SaaS / AI marketing | Tailwind v4 utilities + `dark:` variant | Default for indie + small-team builds |

**Honesty rule:** if the brief reads as one of these systems, install and use the **official** package. Do not recreate its CSS by hand. Do not import a system's tokens and then override 90% of them.

**One system per project.** Do not mix Fluent React with Carbon in the same tree. Do not import shadcn/ui components into a Material 3 app.

## When the brief is an aesthetic, not a system

For these directions there is no single official package. Build with native CSS + Tailwind + a maintained component library. Be honest in comments about what is borrowed inspiration vs. official material.

| Aesthetic | Honest implementation |
|---|---|
| Glassmorphism / frosted glass | `backdrop-filter`, layered borders, highlight overlays. Provide solid-fill fallback for `prefers-reduced-transparency`. |
| Bento (Apple-style tile grids) | CSS Grid with mixed cell sizes. No single library owns this. |
| Brutalism | Native CSS, monospace, raw borders. No library. |
| Editorial / magazine | Serif type, asymmetric grid, generous whitespace. No library. |
| Dark tech / hacker | Mono + accent neon, terminal motifs. No library. |
| Aurora / mesh gradients | SVG or layered radial gradients. No library. |
| Kinetic typography | Native CSS + scroll-driven animations; GSAP for scroll hijacks. No library. |
| Apple "Liquid Glass" | Apple documents this for Apple platforms only. There is no official `liquid-glass.css`. Web versions are approximations using `backdrop-filter` + layered borders + highlights. Label clearly as approximation. |

## Selection checklist

1. State the Design Read first (page kind, audience, vibe, family).
2. If it maps to an official system above, install that package — do not rebuild it.
3. If it is an aesthetic, choose native CSS + Tailwind and label inspiration honestly.
4. Verify the package exists in `package.json` before importing; output the install command if missing.
5. Lock to one system and one accent/type/radius scale for the whole project.

---

Adapted from taste-skill (`design-taste-frontend` §2, MIT © leonxlnx — https://github.com/leonxlnx/taste-skill).
