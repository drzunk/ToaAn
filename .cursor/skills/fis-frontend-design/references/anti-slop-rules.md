# Anti-Slop Rules: Avoiding AI Design Fingerprints

These are patterns LLMs gravitate toward by default. Treat them as "overused AI defaults" — not absolute bans. Context matters. A SaaS dashboard and a personal blog have different rules.

## Typography

**Strongly prefer alternatives to:**
- `Inter` — ubiquitous to the point of being invisible. Prefer `Geist`, `Outfit`, `Cabinet Grotesk`, `Satoshi`, `Plus Jakarta Sans`
- `Roboto` / `Arial` / `Open Sans` — browser defaults with no character
- `Space Grotesk` — overused in "tech startup" contexts

**Alternatives:** Variable fonts, display serifs (editorial), tight grotesks (minimal SaaS), humanist sans (consumer apps)

**Avoid:**
- Serif fonts on dashboards/data UIs — reserve for creative/editorial work
- Only Regular (400) + Bold (700) — use 500/600 for subtler hierarchy
- Orphaned words — use `text-wrap: balance` or `text-wrap: pretty`
- All-caps subheaders everywhere — try lowercase italic, sentence case, small-caps

## Color

**Strongly prefer alternatives to:**
- AI purple/blue gradient aesthetic — the single most common LLM design fingerprint
- Pure `#000000` — use off-black: `#0a0a0a`, `#111`, Zinc-950, or tinted dark
- Oversaturated accents (saturation > 80%) — desaturate to blend elegantly
- Gradient text on large headers — use sparingly, never on body copy

**Principles:**
- Max 1 accent color per project. Remove the rest.
- Stick to one gray family — never mix warm and cool grays
- Tint shadows to match background hue (dark navy shadow on navy bg), not pure black
- Flat design with zero texture feels sterile — add subtle noise/grain

## Layout

**Strongly prefer alternatives to:**
- 3-column equal card layouts as feature rows — the most generic AI layout. Use 2-col zig-zag, asymmetric grid, horizontal scroll, or masonry
- Centered hero with centered H1 at high variance — try split-screen, left-aligned, or asymmetric whitespace
- `h-screen` for full-height sections — always use `min-h-[100dvh]` (iOS Safari viewport bug)
- Complex flexbox `calc()` math for grids — use CSS Grid instead

**Avoid:**
- Everything centered and symmetrical — break with offset margins or mixed aspect ratios
- Equal card heights forced by flexbox — allow variable heights or use masonry
- Uniform border-radius everywhere — vary: tighter on inner elements, softer on containers
- Missing max-width — always constrain to ~1200-1440px with auto margins

## Content (The "Jane Doe" Effect)

**Avoid:**
- Generic names: "John Doe", "Jane Smith", "Sarah Chan" — use realistic, diverse names
- Round fake numbers: `99.99%`, `50%`, `$100.00` — use organic data: `47.2%`, `$99.00`
- Startup slop names: "Acme", "Nexus", "SmartFlow" — invent contextual brand names
- AI copywriting clichés: "Elevate", "Seamless", "Unleash", "Next-Gen", "Game-changer", "Delve", "Tapestry" — write plain, specific language
- Lorem Ipsum — write real draft copy, even if rough
- Exclamation marks in success messages — be confident, not loud
- "Oops!" error messages — be direct: "Connection failed. Try again."
- Title Case On Every Single Header — use sentence case

## Visual Effects

**Strongly prefer alternatives to:**
- Neon/outer glows (`box-shadow` glows) — use inner borders or subtle tinted shadows instead
- Custom mouse cursors — outdated, hurt performance and accessibility
- Standard `ease-in-out` / `linear` transitions — use spring physics or custom cubic-beziers

**Allowed with care:**
- Gradient text — sparingly on accent elements, never on body or large headers
- Glassmorphism — only when it goes beyond `backdrop-blur` (add inner border + refraction shadow)

## Components

**Strongly prefer alternatives to:**
- Unstyled/default shadcn components — always customize radii, colors, and shadows
- Generic card (border + shadow + white bg) at high visual density — use spacing/dividers instead
- Standard Lucide/Feather icons as the only icon set — try Phosphor, Heroicons, or custom SVG for differentiation
- Rocketship for "Launch", shield for "Security" — avoid cliché icon metaphors
- 3-card carousel testimonials with dots — masonry wall, embedded posts, or single rotating quote
- Pill-shaped "New"/"Beta" badges everywhere — try square badges or plain text labels
- Avatar circles exclusively — try squircles or rounded squares

## External Resources

**Avoid:**
- Unsplash direct links — use `https://picsum.photos/seed/{name}/800/600` or SVG UI Avatars
- Same avatar image for multiple users — use unique assets
- Stock "diverse team" photos — use real photos, candid shots, or consistent illustration style

## Pre-Flight Hard Rules (failing any of these is shipping broken work)

These are not stylistic preferences — they are shipping blockers. Audit every one before delivery.

- **Hero fits the initial viewport.** Headline max 2 lines desktop, subtext max ~20 words AND max 3-4 lines, CTAs visible without scroll. A 4-line hero headline is a font-size error, not a copy-length error. Hero top padding max `pt-24` at desktop.
- **Hero stack: max 4 text elements.** Allowed: eyebrow (0 or 1), headline, subtext, CTAs (1 primary + max 1 secondary). BANNED inside the hero: tagline below CTAs, trust micro-strip, pricing teaser, feature bullets, avatar social-proof row. "Used by / Trusted by" logo wall goes in its own section UNDER the hero, never inside it.
- **No duplicate CTA intent.** Two CTAs with the same intent on one page is a fail. "Get in touch" + "Contact us" + "Let's talk" = one intent → pick ONE label and reuse it (nav, hero, footer). Same for signup ("Try free"/"Get started") and portfolio ("View work"/"See projects").
- **CTA text fits one line at desktop.** If a label wraps to 2-3 lines, shorten it (3 words max for primary CTAs) or widen the button. Wrapped CTAs are a fail.
- **Button contrast (WCAG AA).** Verify button text is readable on the button background. White-on-white, `bg-white` + `text-white`, or transparent buttons over the page with no border are banned. Ghost buttons over photos need a scrim, backdrop, or stroke. Min 4.5:1 body, 3:1 for large text (18px+).
- **Form contrast (WCAG AA).** Inputs, placeholder text, focus rings, helper text, and error text all pass contrast against the section background. Label ABOVE input; error text BELOW; never placeholder-as-label.
- **Bento cell count matches content.** A bento grid has exactly as many cells as you have content for. 3 items → 3 cells; 5 items → 5 cells. No empty tile in the middle or at the end — reshape the grid instead.
- **Navigation renders on one line at desktop** (condense, drop secondary items, or hamburger at `lg`). Nav height cap 80px, default 64-72px.
- **Shape consistency lock.** One corner-radius scale for the page (all-sharp, all-soft, or all-pill), or a documented rule per element type followed everywhere.
- **Color consistency lock.** One accent locked for the whole page; do not introduce a new accent hue in a later section.

## AI Tells: Quick Self-Check

Before shipping, scan for these instant giveaways:
- [ ] Inter font anywhere in the project?
- [ ] Purple or blue gradient as the main aesthetic?
- [ ] Three equal-width cards in a row?
- [ ] Centered hero text over a dark gradient image?
- [ ] "John Doe" or "Acme Corp" in any content?
- [ ] Round placeholder numbers (50%, $100)?
- [ ] "Elevate your workflow" or similar AI copy?
- [ ] Pure `#000000` as background?
- [ ] Generic spinner (no skeleton loader)?
- [ ] No hover/active states on buttons?
- [ ] Hero overflowing the viewport or more than 4 text elements?
- [ ] Two CTAs with the same intent (e.g. "Contact us" + "Let's talk")?
- [ ] A CTA label wrapping to 2+ lines at desktop?
- [ ] Button or form text failing WCAG AA contrast?
- [ ] A bento grid with an empty/blank tile?

If any box is checked, the design reads as AI-generated or broken. Address before delivery.
