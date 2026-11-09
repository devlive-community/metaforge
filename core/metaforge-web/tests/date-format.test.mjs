import assert from 'node:assert/strict'
import test from 'node:test'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const { formatDate, formatDateTime, formatDay, formatClock } = await sourceModule('../../metaforge-web/src/utils/date.ts')

test('dates are always written with dashes and a 24-hour clock', () => {
  const value = new Date(2026, 10, 5, 9, 6, 1)
  assert.equal(formatDateTime(value), '2026-11-05 09:06:01')
  assert.equal(formatDay(value), '2026-11-05')
  assert.equal(formatClock(new Date(2026, 10, 5, 22, 56, 21)), '22:56:21')
  assert.equal(formatDateTime(value.getTime()), '2026-11-05 09:06:01')
  assert.equal(formatDateTime(String(value.getTime())), '2026-11-05 09:06:01')
  assert.equal(formatDate(value, 'YYYY-MM-DD HH:mm'), '2026-11-05 09:06')
})

test('time zones and missing values', () => {
  assert.equal(formatDateTime(Date.UTC(2026, 10, 5, 14, 56, 21), { timeZone: 'Asia/Shanghai' }), '2026-11-05 22:56:21')
  assert.equal(formatDateTime(Date.UTC(2026, 10, 5, 16, 0, 0), { timeZone: 'Asia/Shanghai' }), '2026-11-06 00:00:00')
  assert.equal(formatDateTime(null), '-')
  assert.equal(formatDateTime('not a date', { fallback: '—' }), '—')
})

test('no page formats dates with the browser locale', async () => {
  const { execSync } = await import('node:child_process')
  const offenders = execSync(`git grep -nE "toLocaleDateString|toLocaleTimeString|dateStyle|timeStyle|new Date\\([^)]*\\)\\.toLocaleString" -- src || true`, { cwd: new URL('..', import.meta.url), encoding: 'utf8' }).trim()
  assert.equal(offenders, '', 'use formatDateTime / formatDay / formatClock from utils/date')
})
