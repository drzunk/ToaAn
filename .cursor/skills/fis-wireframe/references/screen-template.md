# Per-screen template

Boilerplate for one `screen-NN-<slug>.html`. Self-contained.

## Markup

```html
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8" />
  <title><Screen name></title>
  <script src="https://cdn.tailwindcss.com?plugins=forms,typography"></script>
  <script src="https://unpkg.com/lucide@latest"></script>
  <link rel="stylesheet" href="shared/styles.css" />
</head>
<body class="bg-slate-50 text-slate-900">
  <header class="bg-white border-b">
    <div class="container mx-auto px-4 py-3 flex items-center gap-3">
      <i data-lucide="building-2" class="w-5 h-5 text-slate-600"></i>
      <span class="font-semibold">FIS · <Project name></span>
      <nav class="ml-auto text-sm space-x-3">
        <a href="screen-01-login.html" class="hover:underline">Login</a>
        <a href="screen-02-dashboard.html" class="hover:underline">Dashboard</a>
      </nav>
    </div>
  </header>

  <main class="container mx-auto px-4 py-6">
    <h1 class="text-2xl font-bold mb-4"><Screen heading></h1>
    <!-- Screen content here. Use shadcn-style component patterns from references/shadcn-component-map.md -->
  </main>

  <footer class="border-t bg-white text-xs text-slate-500 py-3 text-center">
    Wireframe · click-through prototype · WF-NNNN screen NN
  </footer>

  <script>
    if (window.lucide) lucide.createIcons();
  </script>
</body>
</html>
```

## Conventions

- Always set `lang="vi"` for Vietnamese content; `lang="en"` for English.
- Use `container mx-auto` + responsive padding for consistent layout.
- Lucide for icons (lightweight, MIT-licensed) — call `lucide.createIcons()` after DOM ready.
- Forms use Tailwind Forms plugin via `?plugins=forms` query param.
- Each screen self-contained: opens directly without `index.html`.
