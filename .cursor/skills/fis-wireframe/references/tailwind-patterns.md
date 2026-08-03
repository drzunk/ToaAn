# Tailwind Patterns — Class Reference

Tailwind v4 class cheatsheet phổ biến cho wireframe. Skill `mode-html.md` reference file này khi generate HTML.

## Layout

### Flexbox
```
flex flex-row | flex-col
items-start | items-center | items-end | items-stretch
justify-start | justify-center | justify-end | justify-between | justify-around
gap-1 | gap-2 | gap-4 | gap-6 | gap-8
flex-1 | flex-auto | flex-none
shrink-0 | grow
```

### Grid
```
grid grid-cols-1 | grid-cols-2 | grid-cols-3 | grid-cols-4 | grid-cols-12
md:grid-cols-2 lg:grid-cols-3                  ← responsive
gap-x-4 gap-y-6
col-span-2 | col-span-full
```

### Container
```
container mx-auto px-4
max-w-sm | max-w-md | max-w-lg | max-w-xl | max-w-2xl | max-w-4xl | max-w-7xl
w-full | h-screen | min-h-screen
```

## Spacing

```
p-0 p-1 p-2 p-3 p-4 p-6 p-8 p-12
px-* | py-* | pt-* pb-* pl-* pr-*
m-0 m-1 m-2 m-4 m-auto
space-y-2 | space-y-4                          ← gap giữa children
```

## Typography

```
text-xs | text-sm | text-base | text-lg | text-xl | text-2xl | text-3xl | text-4xl
font-normal | font-medium | font-semibold | font-bold
text-gray-500 | text-gray-700 | text-gray-900
text-primary | text-error                       ← custom from design.md
text-center | text-left | text-right
leading-tight | leading-normal | leading-relaxed
tracking-tight | tracking-normal | tracking-wide
truncate | text-clip | text-ellipsis
underline | line-through | no-underline
```

## Colors

```
bg-white bg-gray-50 bg-gray-100 bg-gray-900
bg-blue-500 bg-blue-600 bg-blue-700 (hover)
bg-red-500 (error) bg-green-500 (success) bg-yellow-500 (warning)
text-* color
border-gray-200 border-gray-300
ring-2 ring-blue-500                            ← focus ring
```

## Border + Radius

```
border | border-2 | border-x | border-y
border-gray-200 | border-blue-500
rounded | rounded-md | rounded-lg | rounded-xl | rounded-full
rounded-t-md | rounded-b-md                     ← partial corner
divide-y divide-gray-200                        ← child divider
```

## Shadow

```
shadow-sm | shadow | shadow-md | shadow-lg | shadow-xl | shadow-2xl
shadow-none
ring-1 ring-black/5                             ← subtle border
```

## Interactive States

```
hover:bg-blue-700
hover:text-white
focus:outline-none focus:ring-2 focus:ring-blue-500
disabled:opacity-50 disabled:cursor-not-allowed
active:scale-95
group hover:bg-gray-50                          ← parent hover affects children
```

## Responsive Breakpoints

```
sm:  640px  (mobile landscape)
md:  768px  (tablet)
lg:  1024px (desktop)
xl:  1280px (large desktop)
2xl: 1536px (widescreen)

Pattern: <default mobile> md:<tablet> lg:<desktop>
Vd: w-full md:w-1/2 lg:w-1/3
```

## Common Component Patterns

### Button (primary)
```html
<button class="px-4 py-2 bg-blue-600 text-white rounded-md font-medium hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed">
  Submit
</button>
```

### Button (secondary outline)
```html
<button class="px-4 py-2 border border-gray-300 bg-white text-gray-700 rounded-md font-medium hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-500">
  Cancel
</button>
```

### Input
```html
<input
  type="text"
  class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100"
  placeholder="Enter value"
/>
```

### Label + Input pair
```html
<div>
  <label class="block text-sm font-medium text-gray-700 mb-1">
    Số tiền <span class="text-red-500">*</span>
  </label>
  <input class="w-full px-3 py-2 border border-gray-300 rounded-md ..." />
  <p class="mt-1 text-sm text-gray-500">VND, không có dấu phẩy thập phân</p>
</div>
```

### Card
```html
<div class="bg-white rounded-lg shadow-md p-6">
  <h3 class="text-lg font-semibold mb-2">{{Title}}</h3>
  <p class="text-gray-600">{{Body}}</p>
</div>
```

### Badge / Tag
```html
<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
  Approved
</span>
```

### Table row
```html
<tr class="border-b border-gray-200 hover:bg-gray-50">
  <td class="px-4 py-3 text-sm text-gray-900">{{Cell}}</td>
</tr>
```

### Modal backdrop
```html
<div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
  <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
    <h3 class="text-lg font-semibold mb-4">{{Title}}</h3>
    <!-- body -->
    <div class="mt-6 flex justify-end gap-2">
      <button>Cancel</button>
      <button>Confirm</button>
    </div>
  </div>
</div>
```

## Inject design.md vars

Khi `design.md` config có custom colors:

```html
<script>
  tailwind.config = {
    theme: {
      extend: {
        colors: {
          primary: '{{design.colors.primary}}',
          'primary-hover': '{{design.colors.primary_hover}}',
          accent: '{{design.colors.accent}}',
          success: '{{design.colors.success}}',
          error: '{{design.colors.error}}'
        },
        fontFamily: {
          sans: ['{{design.typography.font_family.sans}}']
        }
      }
    }
  }
</script>
```

Sau đó dùng class custom: `bg-primary text-primary-foreground`.

## Reference

- Tailwind v4 docs: https://tailwindcss.com/docs
- Tailwind playground: https://play.tailwindcss.com/
- shadcn theme generator: https://ui.shadcn.com/themes
