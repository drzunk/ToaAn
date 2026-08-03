#!/usr/bin/env node

const { execFileSync, spawn } = require('child_process');
const http = require('http');

const DEFAULT_PORT = 3456;
const PORT_CANDIDATES = [3456, 3457, 3458, 3459, 3460];
const DASHBOARD_URL = `http://localhost:${DEFAULT_PORT}/plans`;

function parseArgs(argv) {
  const args = {
    stop: false,
    deprecated: []
  };

  for (let i = 2; i < argv.length; i++) {
    const arg = argv[i];
    const next = argv[i + 1];

    if (arg === '--help' || arg === '-h') {
      args.help = true;
      continue;
    }

    if (arg === '--open') {
      args.deprecated.push({ flag: '--open', detail: 'Opening is now the default behavior.' });
      continue;
    }

    if (arg === '--stop') {
      args.stop = true;
      args.deprecated.push({
        flag: '--stop',
        detail: 'The launcher does not own the desktop dashboard process. Close it from the FIS AI Kit desktop application.'
      });
      continue;
    }

    if ((arg === '--dir' || arg === '--plans' || arg === '--port' || arg === '--host') && next) {
      args.deprecated.push({ flag: `${arg} ${next}`, detail: getDeprecatedDetail(arg) });
      i += 1;
      continue;
    }

    if (arg === '--background' || arg === '--foreground') {
      args.deprecated.push({ flag: arg, detail: getDeprecatedDetail(arg) });
      continue;
    }

    if (!arg.startsWith('--')) {
      args.deprecated.push({
        flag: arg,
        detail: 'Positional plans paths are no longer used here. This launcher opens the generic /plans route and does not choose a custom plan root.'
      });
      continue;
    }

    args.deprecated.push({
      flag: arg,
      detail: 'This option is no longer supported by plans-kanban. Configure the dashboard in the FIS AI Kit desktop application.'
    });
  }

  return args;
}

function getDeprecatedDetail(flag) {
  switch (flag) {
    case '--dir':
    case '--plans':
      return 'This launcher opens the generic /plans route. Directory selection is no longer handled here; use a scope-aware dashboard entry point or an explicit /plans?dir=... URL.';
    case '--port':
      return 'plans-kanban probes the desktop dashboard on ports 3456-3460.';
    case '--host':
      return 'Host configuration belongs to the FIS AI Kit desktop application.';
    case '--background':
    case '--foreground':
      return 'The launcher connects to an already running desktop dashboard and does not manage its process.';
    default:
      return 'This flag belonged to the retired standalone server flow.';
  }
}

function printHelp() {
  console.log('plans-kanban launcher');
  console.log('');
  console.log('Opens a running FIS AI Kit desktop dashboard at:');
  console.log(`  ${DASHBOARD_URL}`);
  console.log('');
  console.log('Start the desktop application before using this launcher.');
  console.log('Legacy plans-kanban server flags are accepted with warnings for compatibility.');
}

function printDeprecatedWarnings(entries) {
  if (entries.length === 0) {
    return;
  }

  console.warn('\x1b[33m[plans-kanban]\x1b[0m Standalone server flags are deprecated. Opening the desktop dashboard instead.');
  for (const entry of entries) {
    console.warn(`  - ${entry.flag}: ${entry.detail}`);
  }
  console.warn('');
}

async function isDashboardRunningOnPort(port) {
  return new Promise((resolve) => {
    const request = http.get(`http://localhost:${port}/api/health`, (response) => {
      response.resume();
      resolve(response.statusCode >= 200 && response.statusCode < 300);
    });

    request.setTimeout(1200, () => {
      request.destroy();
      resolve(false);
    });

    request.on('error', () => resolve(false));
  });
}

/**
 * Fetch the `features` array from /api/health.
 * Returns an empty array on any failure (timeout, parse error, missing field).
 */
async function fetchHealthFeatures(port) {
  return new Promise((resolve) => {
    const chunks = [];
    const request = http.get(`http://localhost:${port}/api/health`, (response) => {
      if (response.statusCode < 200 || response.statusCode >= 300) {
        response.resume();
        resolve([]);
        return;
      }
      response.on('data', (chunk) => chunks.push(chunk));
      response.on('end', () => {
        try {
          const body = JSON.parse(Buffer.concat(chunks).toString());
          const features = body && Array.isArray(body.features) ? body.features : [];
          resolve(features);
        } catch {
          resolve([]);
        }
      });
    });

    request.setTimeout(1500, () => {
      request.destroy();
      resolve([]);
    });

    request.on('error', () => resolve([]));
  });
}

/**
 * Probe /api/plans with a GET request.
 * Returns true if the endpoint responds with a 2xx status.
 * Used as a backward-compat fallback for desktop builds that shipped plan
 * routes before the `features` flag was added to /api/health.
 */
async function probePlansRoute(port) {
  return new Promise((resolve) => {
    const request = http.get(`http://localhost:${port}/api/plans`, (response) => {
      response.resume();
      resolve(response.statusCode >= 200 && response.statusCode < 300);
    });

    request.setTimeout(1500, () => {
      request.destroy();
      resolve(false);
    });

    request.on('error', () => resolve(false));
  });
}

/**
 * Check whether the running dashboard at `port` supports the plans route.
 * Strategy (in order):
 *   1. /api/health features array contains "plans-dashboard"
 *   2. /api/plans responds with 2xx (backward-compat for early dev builds)
 * Returns true if either probe succeeds.
 */
async function hasPlansDashboardSupport(port) {
  const features = await fetchHealthFeatures(port);
  if (features.includes('plans-dashboard')) {
    return true;
  }
  return probePlansRoute(port);
}

async function findCompatibleDashboard() {
  let incompatiblePort = null;
  for (const port of PORT_CANDIDATES) {
    if (!await isDashboardRunningOnPort(port)) {
      continue;
    }
    if (await hasPlansDashboardSupport(port)) {
      return { port, incompatiblePort: null };
    }
    incompatiblePort ??= port;
  }
  return { port: null, incompatiblePort };
}

function printPlansNotAvailable(port) {
  const dashboardBase = `http://localhost:${port}`;
  process.stderr.write('\x1b[33m[plans-kanban]\x1b[0m Plans dashboard is not available in this desktop build.\n');
  process.stderr.write('  Update the FIS AI Kit desktop application to a build exposing the plans-dashboard feature.\n');
  process.stderr.write(`  Dashboard is running at ${dashboardBase} — the plans route is not yet implemented there.\n`);
}

function openBrowser(url) {
  if (process.platform === 'darwin') {
    execFileSync('open', [url], { stdio: 'ignore' });
    return;
  }

  if (process.platform === 'win32') {
    const child = spawn('cmd', ['/c', 'start', '', url], {
      detached: true,
      stdio: 'ignore'
    });
    child.unref();
    return;
  }

  execFileSync('xdg-open', [url], { stdio: 'ignore' });
}

async function main() {
  const args = parseArgs(process.argv);

  if (args.help) {
    printHelp();
    return;
  }

  printDeprecatedWarnings(args.deprecated);

  if (args.stop) {
    console.log('The launcher does not manage the desktop dashboard process.');
    console.log('Close the dashboard from the FIS AI Kit desktop application.');
    return;
  }

  const { port: dashboardPort, incompatiblePort } = await findCompatibleDashboard();

  if (!dashboardPort) {
    if (incompatiblePort) {
      printPlansNotAvailable(incompatiblePort);
      process.exitCode = 1;
      return;
    }
    console.error(`No FIS AI Kit desktop dashboard found on ports ${PORT_CANDIDATES.join(', ')}.`);
    console.error(`Start the desktop application, then open ${DASHBOARD_URL}.`);
    process.exitCode = 1;
    return;
  }

  const dashboardUrl = `http://localhost:${dashboardPort}/plans`;
  console.log(`Opening ${dashboardUrl}`);
  try {
    openBrowser(dashboardUrl);
  } catch (error) {
    console.error(`Dashboard is running, but automatic browser open failed: ${error.message}`);
    console.error(`Open ${dashboardUrl} manually.`);
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(`plans-kanban launcher failed: ${error.message}`);
  process.exitCode = 1;
});
