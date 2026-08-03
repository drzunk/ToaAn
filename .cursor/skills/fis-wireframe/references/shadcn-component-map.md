# shadcn/ui Component Map (inline patterns)

shadcn/ui là collection of copy-paste React components. Plugin spec-forge HTML mode dùng patterns này inline (vanilla HTML + Tailwind, KHÔNG cần React/npm install).

## Buttons

### Primary
```html
<button class="inline-flex items-center justify-center rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 bg-primary text-primary-foreground hover:bg-primary/90 h-10 px-4 py-2">
  Submit
</button>
```

Simplified Tailwind class (skill output):
```html
<button class="px-4 py-2 bg-blue-600 text-white rounded-md font-medium hover:bg-blue-700 disabled:opacity-50">
  Submit
</button>
```

### Secondary / Outline
```html
<button class="px-4 py-2 border border-gray-300 bg-white text-gray-700 rounded-md font-medium hover:bg-gray-50">
  Cancel
</button>
```

### Destructive
```html
<button class="px-4 py-2 bg-red-600 text-white rounded-md font-medium hover:bg-red-700">
  Delete
</button>
```

### Icon button
```html
<button class="p-2 rounded-md hover:bg-gray-100" aria-label="Edit">
  <svg class="w-4 h-4" data-lucide="pencil"></svg>
</button>
```

## Form Controls

### Input
```html
<input
  type="text"
  class="flex h-10 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
  placeholder="..."
/>
```

### Textarea
```html
<textarea
  rows="4"
  class="flex w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
></textarea>
```

### Select
```html
<select class="flex h-10 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
  <option value="">-- Chọn --</option>
  <option value="1">Option 1</option>
</select>
```

### Checkbox
```html
<label class="inline-flex items-center gap-2 cursor-pointer">
  <input type="checkbox" class="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500" />
  <span class="text-sm">Đồng ý điều khoản</span>
</label>
```

### Radio
```html
<label class="inline-flex items-center gap-2">
  <input type="radio" name="auth" class="w-4 h-4 border-gray-300 text-blue-600 focus:ring-blue-500" />
  <span class="text-sm">Manager approval</span>
</label>
```

### Switch / Toggle
```html
<button role="switch" aria-checked="false" class="relative inline-flex h-6 w-11 items-center rounded-full bg-gray-200 transition-colors data-[state=checked]:bg-blue-600">
  <span class="inline-block h-4 w-4 transform rounded-full bg-white transition-transform translate-x-1 data-[state=checked]:translate-x-6"></span>
</button>
```

## Layout

### Card
```html
<div class="rounded-lg border border-gray-200 bg-white shadow-sm">
  <div class="flex flex-col space-y-1.5 p-6">
    <h3 class="text-lg font-semibold leading-none tracking-tight">{{Title}}</h3>
    <p class="text-sm text-gray-500">{{Description}}</p>
  </div>
  <div class="p-6 pt-0">
    {{Body content}}
  </div>
</div>
```

### Tabs
```html
<div class="w-full">
  <div class="inline-flex h-10 items-center justify-center rounded-md bg-gray-100 p-1">
    <button class="inline-flex items-center justify-center whitespace-nowrap rounded-sm px-3 py-1.5 text-sm font-medium bg-white shadow-sm">Tab 1</button>
    <button class="inline-flex items-center justify-center whitespace-nowrap rounded-sm px-3 py-1.5 text-sm font-medium text-gray-600">Tab 2</button>
  </div>
  <div class="mt-2 p-4 border rounded-md">
    Tab content
  </div>
</div>
```

### Accordion
```html
<details class="border-b border-gray-200">
  <summary class="flex flex-1 items-center justify-between py-4 font-medium hover:underline cursor-pointer">
    Section title
    <svg class="w-4 h-4 transition-transform" data-lucide="chevron-down"></svg>
  </summary>
  <div class="pb-4 pt-0 text-sm">
    Content here
  </div>
</details>
```

## Feedback

### Badge
```html
<span class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium bg-green-100 text-green-800">
  Approved
</span>
```

Status colors:
- Approved: `bg-green-100 text-green-800`
- Pending: `bg-yellow-100 text-yellow-800`
- Rejected / Error: `bg-red-100 text-red-800`
- Draft / Neutral: `bg-gray-100 text-gray-800`
- Info: `bg-blue-100 text-blue-800`

### Alert
```html
<div class="relative w-full rounded-lg border border-yellow-200 bg-yellow-50 p-4 text-yellow-900">
  <div class="flex gap-2">
    <svg class="w-5 h-5 mt-0.5 text-yellow-600" data-lucide="alert-triangle"></svg>
    <div>
      <h5 class="mb-1 font-medium leading-none tracking-tight">Cảnh báo</h5>
      <div class="text-sm">OTP sẽ hết hạn trong 60s.</div>
    </div>
  </div>
</div>
```

### Toast (notification)
```html
<div class="fixed bottom-4 right-4 grid w-full max-w-sm gap-1 rounded-lg border bg-white p-4 shadow-lg">
  <div class="text-sm font-semibold">Đã lưu</div>
  <div class="text-sm text-gray-500">Giao dịch đã được tạo.</div>
</div>
```

## Navigation

### Breadcrumb
```html
<nav class="flex" aria-label="Breadcrumb">
  <ol class="flex items-center space-x-2 text-sm text-gray-600">
    <li><a href="#" class="hover:text-gray-900">Home</a></li>
    <li><span class="text-gray-400">/</span></li>
    <li><a href="#" class="hover:text-gray-900">Transactions</a></li>
    <li><span class="text-gray-400">/</span></li>
    <li class="text-gray-900 font-medium">Approve</li>
  </ol>
</nav>
```

### Pagination
```html
<nav class="flex items-center justify-between">
  <button class="px-3 py-1 border rounded text-sm hover:bg-gray-50">Trước</button>
  <div class="flex gap-1">
    <button class="w-8 h-8 rounded text-sm bg-blue-600 text-white">1</button>
    <button class="w-8 h-8 rounded text-sm hover:bg-gray-100">2</button>
    <button class="w-8 h-8 rounded text-sm hover:bg-gray-100">3</button>
  </div>
  <button class="px-3 py-1 border rounded text-sm hover:bg-gray-50">Sau</button>
</nav>
```

## Data Display

### Table
```html
<div class="rounded-md border border-gray-200">
  <table class="w-full caption-bottom text-sm">
    <thead class="border-b bg-gray-50">
      <tr>
        <th class="h-12 px-4 text-left align-middle font-medium text-gray-700">Column</th>
      </tr>
    </thead>
    <tbody>
      <tr class="border-b transition-colors hover:bg-gray-50">
        <td class="p-4 align-middle">Cell</td>
      </tr>
    </tbody>
  </table>
</div>
```

### Avatar
```html
<div class="relative flex h-10 w-10 shrink-0 overflow-hidden rounded-full bg-gray-200 items-center justify-center">
  <span class="text-sm font-medium text-gray-700">PA</span>
</div>
```

## Icon usage (Lucide)

```html
<!-- Load Lucide -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/lucide/0.344.0/umd/lucide.min.js"></script>

<!-- Use icon -->
<i data-lucide="save"></i>
<i data-lucide="x"></i>
<i data-lucide="user"></i>

<script>lucide.createIcons();</script>
```

Common icons (per design.md aliases):
- Save → `save`
- Cancel → `x`
- Edit → `pencil`
- Delete → `trash-2`
- Search → `search`
- Filter → `filter`
- Add → `plus`
- Settings → `settings`

## Reference

- shadcn/ui official: https://ui.shadcn.com/docs/components
- Lucide icons: https://lucide.dev/icons/
- Skill HTML mode: `references/mode-html.md`
