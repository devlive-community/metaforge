import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
import { createMemoryHistory, createRouter } from 'vue-router'

const moduleUrl = source => `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
const compile = source => ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.ESNext } }).outputText
const navigationUrl = moduleUrl(compile(await readFile(new URL('../src/router/navigation.ts', import.meta.url), 'utf8')))
const specifiers = { '@/router/navigation': navigationUrl, vue: import.meta.resolve('vue'), 'vue-router': import.meta.resolve('vue-router') }
const redirect = compile(await readFile(new URL('../src/composables/useRouteRedirect.ts', import.meta.url), 'utf8'))
    .replace(/from (['"])([^'"]+)\1/g, (_match, _quote, name) => `from ${JSON.stringify(specifiers[name] ?? name)}`)
const { currentLocation, interimLocation, interruptedTarget, isInterimPath, redirectQuery, resolveRedirect, trackPendingNavigation } = await import(moduleUrl(redirect))

const workbench = '/manage?type=source&id=ABC&database=app&table=workflow&tab=statement'

test('in-site addresses come back whole, including query and hash', () => {
    assert.equal(resolveRedirect(workbench), workbench)
    assert.equal(resolveRedirect('/query#sql'), '/query#sql')
    assert.equal(resolveRedirect([workbench, '/home']), workbench)
    assert.equal(resolveRedirect('/admin/query/history/AbC?source=ABC#sql'), '/query/history/AbC?source=ABC#sql')
})

test('external, malformed and interim addresses fall back instead of redirecting', () => {
    for (const value of [undefined, null, 1, '', 'manage', '//evil.example', 'https://evil.example', '/\\evil', '/\n/evil', '/\u007f',
        '/common/not_network', '/common/not_network?redirect=/query', '/common/404', '/common/403#x', '/auth/signin', '/AUTH/signup', '/setup', '/setup?redirect=/query']) {
        assert.equal(resolveRedirect(value), '/home', String(value))
        assert.equal(resolveRedirect(value, ''), '', String(value))
    }
    for (const path of ['/setup-guide', '/authority', '/common/4040', '/common']) assert.equal(isInterimPath(path), false, path)
})

test('interrupted pages hand their address on, interim pages pass along the one they carry', () => {
    const page = { path: '/manage', fullPath: workbench, query: { type: 'source' } }
    assert.equal(interruptedTarget(page), workbench)
    assert.equal(interruptedTarget({ path: '/auth/signin', fullPath: '/auth/signin?redirect=x', query: { redirect: workbench } }), workbench)
    assert.equal(interruptedTarget({ path: '/common/not_network', fullPath: '/common/not_network', query: {} }), '')
    assert.equal(interruptedTarget({ path: '/common/404', fullPath: '/common/404', query: { redirect: '//evil.example' } }), '')
    assert.equal(interruptedTarget(undefined), '')
    assert.deepEqual(interimLocation('/common/not_network', page), { path: '/common/not_network', query: { redirect: workbench } })
    assert.deepEqual(interimLocation('/auth/signin', { path: '/common/not_network', fullPath: '', query: { redirect: workbench } }), { path: '/auth/signin', query: { redirect: workbench } })
    assert.deepEqual(redirectQuery('/auth/signin'), {})
})

test('the pending deep link is reported while the first navigation is still in its guards', async () => {
    const component = { render: () => null }
    const router = createRouter({ history: createMemoryHistory(), routes: ['/home', '/manage', '/common/404', '/common/not_network'].map(path => ({ path, component })) })
    trackPendingNavigation(router)
    let seen
    router.beforeEach((to, from) => {
        seen = currentLocation(router).fullPath
        return to.matched.length ? true : interimLocation('/common/404', from.matched.length ? from : undefined)
    })
    await router.push(workbench)
    assert.equal(seen, workbench)
    assert.equal(currentLocation(router).fullPath, workbench)

    await router.push('/missing')
    assert.equal(router.currentRoute.value.path, '/common/404')
    assert.equal(router.currentRoute.value.query.redirect, workbench)

    await router.replace(interimLocation('/common/not_network', currentLocation(router)))
    assert.equal(router.currentRoute.value.query.redirect, workbench)
})
