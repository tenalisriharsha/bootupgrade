// Regenerates docs/screenshots by RUNNING the real tool, the real API and the real dashboard.
//   Terminal images: each command is executed for real in a scratch directory; its combined
//   stdout/stderr and exit code are captured as-is and rendered into terminal.html. Nothing is typed,
//   edited or shortened by hand, and the command shown is exactly the command that ran.
//   Dashboard images: the dashboard is started against the API, the CLI reports its real runs to that
//   API (--report-to), and Playwright screenshots what the dashboard then shows.
// Usage: cd scripts/capture && npm install && npm run capture
import { spawn, spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright-core';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '../..');
const shots = path.join(root, 'docs/screenshots');
const work = '/tmp/bootupgrade-demo';
fs.rmSync(work, { recursive: true, force: true });
fs.mkdirSync(work, { recursive: true });
const API = 'http://localhost:8080';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function sh(cmd, cwd, opts = {}) {
  const r = spawnSync('bash', ['-c', cmd], { cwd, encoding: 'utf8', ...opts });
  if (r.status !== 0 && !opts.allowFail) throw new Error(`failed: ${cmd}\n${r.stdout}\n${r.stderr}`);
  return r;
}

function findChromium() {
  const cache = path.join(os.homedir(), 'Library/Caches/ms-playwright');
  const linux = path.join(os.homedir(), '.cache/ms-playwright');
  for (const dir of [cache, linux]) {
    if (!fs.existsSync(dir)) continue;
    const found = fs.readdirSync(dir).filter((d) => d.startsWith('chromium-')).sort().reverse();
    for (const d of found) {
      for (const rel of ['chrome-mac/Chromium.app/Contents/MacOS/Chromium', 'chrome-mac-arm64/Chromium.app/Contents/MacOS/Chromium', 'chrome-linux/chrome']) {
        const p = path.join(dir, d, rel);
        if (fs.existsSync(p)) return p;
      }
    }
  }
  return undefined; // fall back to playwright's own default
}

/** Runs `cmd` for real; returns the combined output in arrival order and the exit code. */
function runCaptured(cmd, cwd) {
  return new Promise((resolve) => {
    const child = spawn('bash', ['-c', cmd], { cwd });
    let out = '';
    child.stdout.on('data', (d) => (out += d));
    child.stderr.on('data', (d) => (out += d));
    child.on('close', (code) => resolve({ out: out.replace(/\n$/, ''), code }));
  });
}

async function terminalShot(browser, name, title, cmd, cwd) {
  const { out, code } = await runCaptured(cmd, cwd);
  if (!name) { console.log(`ran (no image): ${cmd} -> exit ${code}`); return code; }
  const page = await browser.newPage({ viewport: { width: 1040, height: 400 }, deviceScaleFactor: 2 });
  await page.goto('file://' + path.join(here, 'terminal.html'));
  await page.evaluate(({ title, cmd, out, code }) => {
    document.getElementById('title').textContent = title;
    const ex = document.getElementById('exit');
    ex.textContent = 'exit code ' + code;
    ex.className = 'exit ' + (code === 0 ? 'ok' : 'bad');
    const body = document.getElementById('body');
    const p = document.createElement('span'); p.className = 'p'; p.textContent = '$ ';
    const c = document.createElement('span'); c.className = 'c'; c.textContent = cmd;
    body.append(p, c, document.createTextNode('\n' + out));
  }, { title, cmd, out, code });
  await page.locator('#win').screenshot({ path: path.join(shots, name + '.png') });
  await page.close();
  console.log(`captured ${name} (exit ${code})`);
  return code;
}

async function waitFor(url, label, tries = 120) {
  for (let i = 0; i < tries; i++) {
    try { const r = await fetch(url); if (r.ok) return; } catch { /* not up yet */ }
    await sleep(1000);
  }
  throw new Error(`${label} did not start at ${url}`);
}

const children = [];
function start(cmd, cwd, label) {
  const c = spawn('bash', ['-c', cmd], { cwd, detached: true, stdio: 'ignore' });
  children.push(c);
  console.log(`started ${label}`);
  return c;
}
function stopAll() { for (const c of children) { try { process.kill(-c.pid); } catch { /* gone */ } } }
process.on('exit', stopAll);

try {
  fs.mkdirSync(shots, { recursive: true });
  for (const f of fs.readdirSync(shots)) if (f.endsWith('.png')) fs.rmSync(path.join(shots, f));

  console.log('building the CLI jar and the API jar ...');
  sh('mvn -B -q package -DskipTests', path.join(root, 'core'));
  sh('mvn -B -q package -DskipTests', path.join(root, 'api'));
  fs.copyFileSync(path.join(root, 'core/target/bootupgrade.jar'), path.join(work, 'bootupgrade.jar'));
  for (const s of ['legacy-rest-service', 'legacy-jpa-service', 'legacy-security-service']) {
    sh(`rsync -a --exclude target --exclude node_modules '${path.join(root, 'samples', s)}' '${work}/'`, root);
  }
  sh('terraform -chdir=infra init -backend=false -input=false -no-color', root, { stdio: 'ignore' });

  start(`java -jar '${path.join(root, 'api/target/bootupgrade-api.jar')}'`, root, 'API on :8080');
  await waitFor(`${API}/api/runs`, 'API');
  start('npx ng serve --port 4200', path.join(root, 'dashboard'), 'dashboard on :4200');
  await waitFor('http://localhost:4200/', 'dashboard', 240);

  const browser = await chromium.launch({ executablePath: findChromium() });
  const jar = 'java -jar bootupgrade.jar';

  await terminalShot(browser, '01-cli-upgrade-succeeded', 'bootupgrade: the recipes alone are enough',
    `${jar} legacy-rest-service --out reports --report-to ${API}`, work);
  await terminalShot(browser, null, '', `${jar} legacy-jpa-service --out reports --report-to ${API}`, work);
  await terminalShot(browser, '02-cli-upgrade-partial', 'bootupgrade: build still red after the recipes',
    `${jar} legacy-security-service --out reports --report-to ${API}`, work);
  await terminalShot(browser, '03-pr-description', 'the generated pull request text',
    'cat reports/bootupgrade-legacy-rest-service-pr.md', work);
  await terminalShot(browser, '04-git-result', 'what landed on the upgrade branch',
    'git -C legacy-rest-service show --stat --oneline HEAD', work);
  await terminalShot(browser, '05-terraform-test', 'terraform test (mocked AWS provider, no AWS account needed)',
    'terraform -chdir=infra test -no-color', root);

  // The dashboard now shows the runs the CLI just reported.
  const page = await browser.newPage({ viewport: { width: 1280, height: 700 }, deviceScaleFactor: 2 });
  await page.goto('http://localhost:4200/');
  await page.waitForSelector('tr.run-row', { timeout: 30000 });
  await sleep(500);
  await page.screenshot({ path: path.join(shots, '06-dashboard-runs.png'), fullPage: true });
  console.log('captured 06-dashboard-runs');
  const rows = await page.locator('tr.run-row').count();
  for (let i = 0; i < rows; i++) {
    await page.goto('http://localhost:4200/');
    await page.waitForSelector('tr.run-row');
    const row = page.locator('tr.run-row').nth(i);
    const label = (await row.innerText()).replace(/\s+/g, ' ').trim();
    await row.click();
    await page.waitForSelector('text=Steps', { timeout: 15000 }).catch(() => {});
    await sleep(500);
    const slug = /legacy-(\w+)-service/.exec(label)?.[1] ?? `run${i}`;
    if (slug === 'jpa') continue; // same shape as the rest-service run
    await page.screenshot({ path: path.join(shots, `${slug === 'rest' ? '07' : '08'}-dashboard-run-${slug}.png`), fullPage: true });
    console.log(`captured dashboard run ${slug}`);
  }
  await browser.close();
} finally {
  stopAll();
  console.log(`scratch dir: ${work}`);
}
