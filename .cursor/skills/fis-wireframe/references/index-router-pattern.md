# index.html — central screen router

Skeleton for the central router that all screens are reached from.

## Markup

```html
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8" />
  <title>WF-NNNN — <Feature Name></title>
  <script src="https://cdn.tailwindcss.com?plugins=forms,typography"></script>
  <link rel="stylesheet" href="shared/styles.css" />
  <style>
    body { display: grid; grid-template-columns: 240px 1fr; grid-template-rows: 48px 1fr; height: 100vh; }
    header { grid-column: 1 / -1; }
    aside { overflow-y: auto; border-right: 1px solid #e5e7eb; }
    main { overflow: hidden; }
    iframe { width: 100%; height: 100%; border: 0; }
    .screen-link.active { background: #1F4E79; color: white; }
  </style>
</head>
<body class="bg-slate-50 text-slate-900">
  <header class="bg-white border-b flex items-center px-4 gap-4">
    <span class="font-semibold">WF-NNNN — <Feature Name></span>
    <span class="text-xs text-slate-500">v0.1 · <YYYY-MM-DD></span>
  </header>
  <aside class="bg-white p-2">
    <nav class="space-y-1">
      <a href="#" data-src="screen-01-login.html" class="screen-link block rounded px-3 py-2 text-sm hover:bg-slate-100">01 · Login</a>
      <a href="#" data-src="screen-02-dashboard.html" class="screen-link block rounded px-3 py-2 text-sm hover:bg-slate-100">02 · Dashboard</a>
      <!-- Repeat per screen -->
    </nav>
  </aside>
  <main>
    <iframe id="stage" src="screen-01-login.html" title="Active screen"></iframe>
  </main>
  <script>
    const stage = document.getElementById('stage');
    document.querySelectorAll('.screen-link').forEach(link => {
      link.addEventListener('click', e => {
        e.preventDefault();
        document.querySelectorAll('.screen-link').forEach(l => l.classList.remove('active'));
        link.classList.add('active');
        stage.src = link.dataset.src;
      });
    });
  </script>
</body>
</html>
```

## Notes

- Each screen file in the `data-src` attribute must exist as a sibling file.
- The first link is marked `active` by default.
- Sidebar widths: 240px desktop, collapse to drawer on mobile (optional v2).
- Cross-screen navigation: screens can use `parent.location` if standalone, or `parent.postMessage({ go: 'screen-03-detail.html' })` from inside iframe to drive the router.
