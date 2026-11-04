import assert from 'node:assert/strict'
import test from 'node:test'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const { sharedReportPath, routeSharedReportPath } = await sourceModule('../../metaforge-web/src/router/shared.ts')
const { formatShareTime, isShareableReport, isShareExpired, localDate, reportEmbedCode, reportShareUrl, SHARE_ORIGIN, shareExpiresAt } = await sourceModule('../../metaforge-web/src/views/pages/admin/report/ReportShareUtils.ts')

const TOKEN = 'abcdefghijklmnopqrstuvwxyz012-_A'

test('only well-formed share and embed paths open the public report', () => {
  assert.deepEqual(sharedReportPath(`/share/report/${TOKEN}`), { token: TOKEN, embed: false })
  assert.deepEqual(sharedReportPath(`/embed/report/${TOKEN}/`), { token: TOKEN, embed: true })
  assert.equal(sharedReportPath('/embed/report/short'), null)
  assert.equal(sharedReportPath(`/embed/report/${TOKEN}/../x`), null)
  assert.equal(sharedReportPath('/'), null)
})

test('the shared path keeps the address and adds the hash route', () => {
  const calls = []
  const history = { state: null, replaceState: (_state, _title, url) => calls.push(url) }
  routeSharedReportPath({ pathname: `/embed/report/${TOKEN}`, search: '?title=0', hash: '' }, history)
  assert.deepEqual(calls, [`/embed/report/${TOKEN}?title=0#/public/report/${TOKEN}?embed=1`])
  routeSharedReportPath({ pathname: `/embed/report/${TOKEN}`, search: '', hash: `#/public/report/${TOKEN}?embed=1` }, history)
  routeSharedReportPath({ pathname: '/', search: '', hash: '#/report' }, history)
  assert.equal(calls.length, 1, 'already routed or ordinary pages are left alone')
})

test('dataset reports and query charts are shareable, earlier query reports are not', () => {
  assert.equal(isShareableReport({ type: 'DATASET', configure: '{}' }), true)
  assert.equal(isShareableReport({ type: 'QUERY', configure: '{"queryChart":{"version":1}}' }), true)
  assert.equal(isShareableReport({ type: 'QUERY', configure: '{"type":"LINE"}' }), false)
  assert.equal(isShareableReport({ type: 'QUERY', configure: 'not json' }), false)
})

test('links and embed code', () => {
  assert.equal(reportShareUrl('https://bi.example.com', TOKEN), `https://bi.example.com/share/report/${TOKEN}`)
  const code = reportEmbedCode('https://bi.example.com', TOKEN, 'Sales "Q1" <script>')
  assert.match(code, new RegExp(`src="https://bi.example.com/embed/report/${TOKEN}"`))
  assert.match(code, /title="Sales &quot;Q1&quot; &lt;script>"/)
  for (const origin of ['https://example.com', 'http://localhost:8080', 'https://*.example.com']) assert.equal(SHARE_ORIGIN.test(origin), true, origin)
  for (const origin of ['example.com', 'javascript:alert(1)', 'https://a.com/path', 'https://a.com?x=1']) assert.equal(SHARE_ORIGIN.test(origin), false, origin)
})

test('expiry choices become concrete moments', () => {
  const now = new Date(2026, 10, 4, 10, 30).getTime()
  assert.equal(shareExpiresAt('never', '', now), null)
  assert.equal(shareExpiresAt('7d', '', now), now + 7 * 24 * 60 * 60 * 1000)
  assert.equal(shareExpiresAt('date', '2026-11-30', now), new Date(2026, 10, 30, 23, 59, 59, 999).getTime(), 'a date lasts the whole day')
  assert.equal(shareExpiresAt('date', '', now), null)
  assert.equal(localDate(new Date(2026, 0, 5, 23, 0).getTime()), '2026-01-05')
  assert.equal(isShareExpired(now - 1, now), true)
  assert.equal(isShareExpired(now + 1, now), false)
  assert.equal(isShareExpired(null, now), false)
})

test('expiry times use dashes, not the browser locale', () => {
  assert.equal(formatShareTime(new Date(2026, 10, 5, 10, 30, 18).getTime()), '2026-11-05 10:30:18')
  assert.equal(formatShareTime(new Date(2026, 0, 2, 3, 4, 5).getTime()), '2026-01-02 03:04:05')
})

test('the shared path mapping runs before the router module is evaluated', async () => {
  const { readFile } = await import('node:fs/promises')
  const main = await readFile(new URL('../src/main.ts', import.meta.url), 'utf8')
  const imports = [...main.matchAll(/^import .*$/gm)].map(match => match[0])
  assert.equal(imports[0], "import './shared-entry'", 'the hash router captures the address when created')
})
