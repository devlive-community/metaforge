import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/system-setting.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { agreements, defaultAuthSettings, defaultMailSettings, EMAIL_PATTERN, mailReady, USERNAME_PATTERN, waitingForMail } = await import(`data:text/javascript;base64,${ Buffer.from(outputText).toString('base64') }`)

test('mail counts as ready once a host and a sender are set', () => {
  assert.equal(mailReady(defaultMailSettings()), false)
  assert.equal(mailReady({ ...defaultMailSettings(), host: 'smtp.example.com' }), false)
  assert.equal(mailReady({ ...defaultMailSettings(), host: ' smtp.example.com ', from: 'noreply@example.com' }), true)
})

test('only the mail features that are on wait for a mail server', () => {
  const auth = { ...defaultAuthSettings(), emailVerification: true, emailSignin: true }
  assert.deepEqual(waitingForMail(auth, defaultMailSettings()), ['emailVerification'])
  assert.deepEqual(waitingForMail({ ...auth, passwordReset: true }, defaultMailSettings()), ['emailVerification', 'passwordReset'])
  assert.deepEqual(waitingForMail(auth, { ...defaultMailSettings(), host: 'smtp.example.com', from: 'a@example.com' }), [])
})

test('sign-up asks to agree only to the agreements with an address', () => {
  assert.deepEqual(agreements({ termsUrl: '', privacyUrl: '' }), [])
  assert.deepEqual(agreements({ termsUrl: 'https://example.com/terms', privacyUrl: 'not a url' }), [{ key: 'terms', url: 'https://example.com/terms' }])
  assert.deepEqual(agreements({ termsUrl: 'https://a.example/t', privacyUrl: 'http://a.example/p' }).map(item => item.key), ['terms', 'privacy'])
})

test('usernames and emails follow the sign-up rules', () => {
  for (const name of ['abc', 'user_01', 'A'.repeat(32)]) assert.equal(USERNAME_PATTERN.test(name), true, name)
  for (const name of ['ab', 'A'.repeat(33), 'with space', 'dash-name', '中文名']) assert.equal(USERNAME_PATTERN.test(name), false, name)
  assert.equal(EMAIL_PATTERN.test('a@example.com'), true)
  assert.equal(EMAIL_PATTERN.test('a@example'), false)
})
