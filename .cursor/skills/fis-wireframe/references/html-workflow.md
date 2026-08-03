# Mode: html — Tailwind + shadcn/ui artifact (DEFAULT)

## Trigger
"Wireframe HTML" / "Mockup [screen]" / "Vẽ form/list/dashboard"

## Stack

- **Tailwind CSS v4** via CDN: `https://cdnjs.cloudflare.com/ajax/libs/tailwindcss/...`
- **shadcn/ui inline** — copy components directly (no npm install)
- **Lucide icons** via CDN
- **Vanilla JS only** — no React/Vue (keep self-contained artifact)

## Steps

### 1. Resolve input

| Input | Source |
|---|---|
| `--pattern=<form|list|detail|dashboard|modal|navbar>` | Pre-select template |

### 2. Read design config (optional)

Check `docs/design/design.md` exists:
- Yes → load theme/colors/typography/density → inject vào HTML
- No → fallback default Tailwind + shadcn

### 3. Auto-detect pattern (nếu không pass --pattern)

AskUserQuestion 6 options + brief description:
- **Form** — single-step input/edit
- **List** — table with search/filter/pagination
- **Detail** — read view với Edit/Delete actions
- **Dashboard** — metrics + charts + activity
- **Modal** — confirmation dialog / mini form
- **Navbar** — top nav + sidebar layout

### 4. Gather component details

**Form pattern:**
- Field list (name, type, required, validation hint)
- Sections (group fields)
- Footer actions (Cancel, Save, Submit)

**List pattern:**
- Columns (name, type)
- Search/filter chips
- Bulk actions
- Row click target (modal / detail page)

**Detail pattern:**
- Field grid (read-only display)
- Tabs hoặc sections
- Actions (Edit / Delete / Back / Export)
- Related items list

**Dashboard pattern:**
- Top metric cards (4-6)
- Chart areas (line / bar / pie / Recharts)
- Activity feed
- Quick actions

**Modal pattern:**
- Title + close X
- Body content
- Footer Cancel/Confirm

**Navbar pattern:**
- Logo + search + user menu
- Sidebar items + collapse
- Main content slot

### 5. Apply design config

Inject từ `design.md`:

```html
<!-- Default fallback -->
<body class="bg-gray-50 text-gray-900">

<!-- Apply design.md theme -->
<body class="bg-{{theme.background}} text-{{theme.text}}">
```

Tailwind config inline (nếu cần extend):

```html
<script>
  tailwind.config = {
    theme: {
      extend: {
        colors: {
          primary: '{{design.colors.primary}}',
          secondary: '{{design.colors.secondary}}',
          accent: '{{design.colors.accent}}'
        },
        fontFamily: {
          sans: ['{{design.typography.sans}}']
        },
        borderRadius: {
          DEFAULT: '{{design.borderRadius.default}}'
        }
      }
    }
  }
</script>
```

### 6. Generate HTML

Pick template từ `references/templates/html-<pattern>.html`. Replace placeholders:
- `{{TITLE}}` — screen title
- `{{FIELDS}}` — field rows from input
- `{{ACTIONS}}` — button list

### 7. Save

Output 2 files:

**Path A:** `docs/wireframes/WF-NNNN-<slug>.html` (self-contained, browser-renderable)

**Path B:** `docs/wireframes/WF-NNNN-<slug>.md` (companion với HTML embedded code block + extracted props/components):

```markdown
---
id: WF-NNNN
type: wireframe
fidelity: html
pattern: form
parent_artifact: "DDD-NNNN | US-NNNN"
design_config: "docs/design/design.md"
version: 1
---

# WF-NNNN: {{Title}} (HTML wireframe)

## Preview
[Open WF-NNNN-<slug>.html](./WF-NNNN-<slug>.html) trong browser hoặc Claude Cowork artifact panel.

## Components used (shadcn/ui inline)
- Button (primary/secondary)
- Input (text/number)
- Select
- Card

## Props extracted (cho DEV)
| Component | Props |
|---|---|
| TransferForm | { from, to, amount, note, onSubmit, onCancel } |

## HTML source

\`\`\`html
{{embedded HTML}}
\`\`\`
```

### 8. Optional escalate

AskUserQuestion: "Cần escalate fidelity?"
- HTML đủ → keep
- Stakeholder review meeting → invoke `figma` mode
- Show artifact panel → display `.html` directly

## Vietnamese label support

UTF-8 OK trong HTML. Sample button labels: "Lưu", "Hủy", "Phê duyệt", "Từ chối", "Thêm mới", "Xuất Excel", "Tải lên", "Tìm kiếm".

## Convergence check

- HTML render OK (no syntax error, browser preview works)
- Tailwind classes valid (use Tailwind playground để verify)
- Responsive (`md:`, `lg:` classes có) — works mobile
- A11y minimum (`<label>`, semantic `<nav><main>`, `aria-label` cho icon buttons)

## Reference

- `references/tailwind-patterns.md` — class cheatsheet
- `references/shadcn-component-map.md` — shadcn inline patterns
- `references/templates/design.template.md` — design config schema
- `references/templates/html-*.html` — 6 pattern templates
