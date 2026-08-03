---
id: DESIGN-{{project}}
type: design-config
version: 1
project_code: ""
last_updated: "{{YYYY-MM-DD}}"
---

# Design Config — {{Project Name}}

> Customize wireframe style. Lưu file này tại `docs/design/design.md`. `/fis-wireframe html` sẽ ĐỌC config này và inject vào HTML output.
> Nếu file không tồn tại → fallback default Tailwind v4 + shadcn/ui defaults.

## 1. Theme

```yaml
theme:
  mode: light                          # light | dark | auto (system preference)
  background: "#f9fafb"                # bg-gray-50 default
  surface: "#ffffff"                   # card / modal background
  text:
    primary: "#111827"                 # main text
    secondary: "#6b7280"               # muted text
    inverse: "#ffffff"                 # on primary color
  border: "#e5e7eb"                    # divider lines
```

## 2. Color Palette

```yaml
colors:
  # Brand
  primary: "#2563eb"                   # FIS blue (default — change to brand color)
  primary_hover: "#1d4ed8"
  primary_active: "#1e40af"

  secondary: "#64748b"                 # neutral slate
  accent: "#f59e0b"                    # warm amber (CTA / highlight)

  # Status
  success: "#10b981"                   # green
  warning: "#f59e0b"                   # amber
  error: "#ef4444"                     # red
  info: "#3b82f6"                      # blue

  # Data viz (charts)
  chart_palette:
    - "#2563eb"                        # primary
    - "#10b981"                        # success
    - "#f59e0b"                        # warning
    - "#8b5cf6"                        # purple
    - "#ec4899"                        # pink
```

**FIS brand presets** (uncomment nếu dùng):

```yaml
# FIS Vietnam brand
# colors:
#   primary: "#0f4c81"                 # FIS deep blue
#   accent: "#f47b20"                  # FIS orange
```

```yaml
# EVN brand
# colors:
#   primary: "#005baa"                 # EVN blue
#   accent: "#fdb813"                  # EVN yellow
```

## 3. Typography

```yaml
typography:
  font_family:
    sans: "Inter, system-ui, sans-serif"
    serif: "Merriweather, serif"
    mono: "JetBrains Mono, monospace"

  scale:
    xs: "0.75rem"                      # 12px — caption
    sm: "0.875rem"                     # 14px — body small
    base: "1rem"                       # 16px — body default
    lg: "1.125rem"                     # 18px — body large
    xl: "1.25rem"                      # 20px — h4
    "2xl": "1.5rem"                    # 24px — h3
    "3xl": "1.875rem"                  # 30px — h2
    "4xl": "2.25rem"                   # 36px — h1

  font_weight:
    normal: 400
    medium: 500
    semibold: 600
    bold: 700

  line_height:
    tight: 1.25                        # heading
    normal: 1.5                        # body
    relaxed: 1.75                      # long-form prose
```

**Vietnamese-friendly fonts** (cho doc với diacritics):

```yaml
# typography:
#   font_family:
#     sans: "'Be Vietnam Pro', Inter, system-ui, sans-serif"  # Google Font Vietnamese
```

## 4. Spacing Scale

```yaml
spacing:
  unit: "0.25rem"                      # 4px base (Tailwind default)

  # Tailwind classes mapped:
  # 0  = 0
  # 1  = 4px
  # 2  = 8px
  # 4  = 16px
  # 6  = 24px
  # 8  = 32px
  # 12 = 48px
  # 16 = 64px

  density: comfortable                 # compact | comfortable | spacious
```

**Density preset:**
- `compact` — gap-2, p-3, text-sm (cho dashboard, list view)
- `comfortable` — gap-4, p-4, text-base (default)
- `spacious` — gap-6, p-6, text-lg (cho landing, marketing pages)

## 5. Border Radius

```yaml
border_radius:
  style: rounded                       # sharp | rounded | pill

  # Tailwind classes mapped per style:
  # sharp:    rounded-none (0)
  # rounded:  rounded-md (6px) — default
  # pill:     rounded-full (9999px)

  default: "0.375rem"                  # 6px
  sm: "0.25rem"                        # 4px
  lg: "0.5rem"                         # 8px
  full: "9999px"                       # pill
```

## 6. Shadow

```yaml
shadow:
  sm: "0 1px 2px 0 rgb(0 0 0 / 0.05)"
  md: "0 4px 6px -1px rgb(0 0 0 / 0.1)"
  lg: "0 10px 15px -3px rgb(0 0 0 / 0.1)"
  xl: "0 20px 25px -5px rgb(0 0 0 / 0.1)"
```

## 7. Component Density

```yaml
component:
  button:
    height_default: "2.5rem"           # 40px
    padding_x: "1rem"
    padding_y: "0.5rem"
    font_size: "0.875rem"

  input:
    height: "2.5rem"
    border_width: "1px"
    padding_x: "0.75rem"

  card:
    padding: "1.5rem"
    border_radius: "0.5rem"
    shadow: "md"

  table:
    row_height: "3rem"
    cell_padding: "0.75rem"
    border: "subtle"                   # subtle | divider | none
```

## 8. Brand Identity

```yaml
brand:
  name: "{{Project / Company name}}"
  logo:
    light: "/assets/logo-light.svg"    # path tương đối từ docs/
    dark: "/assets/logo-dark.svg"
    favicon: "/assets/favicon.ico"
  tagline: "{{Optional tagline}}"

  copyright: "© 2026 FIS — All rights reserved"

  social:
    website: "https://fis.com.vn"
    support_email: "support@fis.com.vn"
```

## 9. Icon Set

```yaml
icons:
  library: lucide                      # lucide | heroicons | font-awesome
  cdn: "https://cdnjs.cloudflare.com/ajax/libs/lucide/0.344.0/umd/lucide.min.js"

  size_default: "1.25rem"              # 20px
  stroke_width: 2

  # Aliases (mapping common actions → icon name)
  actions:
    save: "save"
    cancel: "x"
    edit: "pencil"
    delete: "trash-2"
    search: "search"
    filter: "filter"
    download: "download"
    upload: "upload"
    add: "plus"
    settings: "settings"
    user: "user"
    logout: "log-out"
```

## 10. Locale

```yaml
locale:
  default: "vi-VN"                     # Vietnamese primary
  fallback: "en-US"

  date_format: "DD/MM/YYYY"            # Vietnamese standard
  time_format: "HH:mm"                 # 24-hour
  currency:
    code: "VND"
    symbol: "₫"
    position: suffix                   # "1,000,000 ₫"
    thousands_sep: ","
    decimal_sep: "."
    fraction_digits: 0                 # VND no decimal
```

## 11. Accessibility

```yaml
a11y:
  contrast_min: "AA"                   # WCAG AA (4.5:1) | AAA (7:1)
  focus_ring: "2px solid {{primary}}"
  reduce_motion: respect-prefers       # auto detect prefers-reduced-motion
  font_min_size: "0.875rem"            # 14px minimum readable
```

## How to use

1. Copy template này → `docs/design/design.md`
2. Customize values (color, typography, density, brand)
4. Skill ĐỌC config + inject vào HTML output

## Reference

- Tailwind v4: https://tailwindcss.com/docs/theme
- shadcn/ui themes: https://ui.shadcn.com/themes
- WCAG color contrast: https://webaim.org/resources/contrastchecker/
- Vietnamese font: Be Vietnam Pro (Google Fonts)
