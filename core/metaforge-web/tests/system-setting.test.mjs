import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/system-setting.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const {
  agreements, defaultAuthSettings, defaultMailSettings, defaultOAuthGroup, defaultOAuthSettings, EMAIL_PATTERN, incompleteProviders, knownProviders, mailReady,
  oauthCallbackUrl, oauthConfigured, oauthError, USERNAME_PATTERN, waitingForMail,
} = await import(`data:text/javascript;base64,${ Buffer.from(outputText).toString('base64') }`)

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

test('a provider counts as configured once its client, and for OpenID Connect its endpoints, are set', () => {
  const client = { ...defaultOAuthSettings(), clientId: 'id', clientSecret: 'secret' }
  assert.equal(oauthConfigured('github', defaultOAuthSettings()), false)
  assert.equal(oauthConfigured('github', client), true)
  // A stored secret counts although the page never receives it.
  assert.equal(oauthConfigured('google', { ...client, clientSecret: '', secretSet: true }), true)
  assert.equal(oauthConfigured('oidc', client), false)
  const endpoints = { authorizationUrl: 'https://sso.example.com/a', tokenUrl: 'https://sso.example.com/t', userinfoUrl: 'https://sso.example.com/u' }
  assert.equal(oauthConfigured('oidc', { ...client, ...endpoints }), true)
  assert.equal(oauthConfigured('oidc', { ...client, ...endpoints, tokenUrl: 'sso.example.com/t' }), false)
})

test('only providers that are on but unfinished block saving', () => {
  const group = defaultOAuthGroup()
  assert.deepEqual(incompleteProviders(group), [])
  group.oidc = { ...group.oidc, enabled: true, clientId: 'id', clientSecret: 'secret' }
  group.github = { ...group.github, enabled: true }
  assert.deepEqual(incompleteProviders(group), ['github', 'oidc'])
})

test('the callback address prefers the site address', () => {
  assert.equal(oauthCallbackUrl('', 'http://localhost:9096', 'github'), 'http://localhost:9096/api/auth/oauth/github/callback')
  assert.equal(oauthCallbackUrl(' https://mf.example.com/ ', 'http://localhost:9096', 'oidc'), 'https://mf.example.com/api/auth/oauth/oidc/callback')
})

test('the sign-in page shows only known providers and known errors', () => {
  assert.deepEqual(knownProviders(undefined), [])
  assert.deepEqual(knownProviders([{ id: 'github', name: 'GitHub' }, { id: 'gitlab', name: 'GitLab' }, 'oidc', null]), [{ id: 'github', name: 'GitHub' }])
  assert.equal(oauthError('registration'), 'registration')
  assert.equal(oauthError('<script>'), 'failed')
  assert.equal(oauthError(undefined), 'failed')
})
