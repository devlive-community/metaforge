import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
import { createMemoryHistory, createRouter } from 'vue-router'

const moduleUrl = source => `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
const compile = source => ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.ESNext } }).outputText
const navigationUrl = moduleUrl(compile(await readFile(new URL('../src/router/navigation.ts', import.meta.url), 'utf8')))
const { canonicalPagePath, normalizeNavigation, pageAccess } = await import(navigationUrl)
let routes = compile(await readFile(new URL('../src/router/default.ts', import.meta.url), 'utf8'))
const manageUrl = moduleUrl(compile(await readFile(new URL('../src/model/source-manage.ts', import.meta.url), 'utf8')))
const layoutUrl = moduleUrl('export default {render(){return null}}')
// The home route asks the user store for the user's chosen home page; tests set it on globalThis.
const storeUrl = moduleUrl('export const useUserStore = () => ({ resolveHomePage: async () => globalThis.__homePage || "" })')
const stubs = { './navigation': navigationUrl, '@/model/source-manage': manageUrl, '@/stores/user': storeUrl }
routes = routes.replace(/from (['"])([^'"]+)\1/g, (_match, _quote, name) => `from ${JSON.stringify(stubs[name] || layoutUrl)}`)
routes = routes.replace(/import\((['"])@\/views\/[^'"]+\1\)/g, 'Promise.resolve({render(){return null}})')
const { createDefaultRouter } = await import(moduleUrl(routes))
const user = { token: 'test', roles: ['USER'] }
const admin = { token: 'test', roles: ['ADMIN'] }

function routerFor(actor) {
    const router = createRouter({ history: createMemoryHistory(), routes: [
        { path: '/common/403', component: { render: () => null } },
        { path: '/auth/signin', component: { render: () => null } },
    ] })
    createDefaultRouter(router)
    router.beforeEach(to => pageAccess(to, actor))
    return router
}

test('business and account pages are available at root paths without admin requirements', async () => {
    const router = routerFor(user)
    for (const path of ['/query', '/source', '/dataset', '/workflow', '/snippet', '/history', '/dashboard', '/report', '/store', '/account/profile']) {
        await router.push(path)
        assert.equal(router.currentRoute.value.path, path)
        assert.equal(router.currentRoute.value.meta.requiresAuth, true)
        assert.ok(!router.currentRoute.value.meta.requiresAdmin)
    }
})

test('management pages require ADMIN and the account route does not collide with admin users', async () => {
    for (const path of ['/admin/user', '/admin/role', '/admin/menu', '/admin/schedule', '/admin/function', '/admin/settings']) {
        const ordinary = routerFor(user)
        await ordinary.push(path)
        assert.equal(ordinary.currentRoute.value.path, '/common/403')
        const privileged = routerFor(admin)
        await privileged.push(path)
        assert.equal(privileged.currentRoute.value.path, path)
        assert.equal(privileged.currentRoute.value.meta.requiresAdmin, true)
    }
    // Runtime settings moved into System settings; the old page opens its tab.
    const privileged = routerFor(admin)
    await privileged.push('/admin/configure')
    assert.equal(privileged.currentRoute.value.path, '/admin/settings')
    assert.equal(privileged.currentRoute.value.query.tab, 'runtime')
    const ordinary = routerFor(user)
    await ordinary.push('/admin/user/profile')
    assert.equal(ordinary.currentRoute.value.path, '/account/profile')
})

test('legacy deep links preserve parameters, queries and hashes and normalize only once', async () => {
    const cases = [
        ['/admin/query/history/AbC?sql=select%201#editor', '/query/history/AbC'],
        ['/admin/source/SRC/d/MyDB/t/data/MyTable?limit=1#rows', '/manage'],
        ['/admin/dataset/info/source/SourceCode?tempId=3', '/dataset/info/source/SourceCode'],
        ['/system/role?tab=members#list', '/admin/role'],
        ['/SYSTEM/ROLE', '/admin/role'],
        ['/admin/user/assistant', '/account/assistant'],
    ]
    for (const [old, expected] of cases) {
        const router = routerFor(admin)
        await router.push(old)
        assert.equal(router.currentRoute.value.path, expected)
        assert.equal(canonicalPagePath(expected), expected)
        if (expected === '/manage') assert.deepEqual({type:router.currentRoute.value.query.type,id:router.currentRoute.value.query.id,database:router.currentRoute.value.query.database,table:router.currentRoute.value.query.table,tab:router.currentRoute.value.query.tab},{type:'source',id:'SRC',database:'MyDB',table:'MyTable',tab:'data'})
        const original = new URL(old, 'https://example.test')
        assert.equal(router.currentRoute.value.hash, original.hash)
        for (const [key, value] of original.searchParams) assert.equal(router.currentRoute.value.query[key], value)
    }
})

test('anonymous deep links carry their full address to the login page', async () => {
    const router = routerFor(undefined)
    await router.push('/query?source=ABC')
    assert.equal(router.currentRoute.value.path, '/auth/signin')
    assert.equal(router.currentRoute.value.query.redirect, '/query?source=ABC')
})

test('cached menus keep business links, hide admin links from users and retain leaves with empty children', () => {
    const old = [
        { code: 'MANAGEMENT', url: '/admin', children: [{ url: '/admin/source', children: [] }, { url: '/system/function' }] },
        { code: 'SYSTEM', url: '/system', children: [{ url: '/system/role' }] },
    ]
    const ordinary = normalizeNavigation(old, user)
    assert.equal(ordinary.length, 1)
    assert.equal(ordinary[0].url, '/workspace')
    assert.deepEqual(ordinary[0].children.map(item => item.url), ['/source'])
    assert.equal(ordinary[0].children[0].children, undefined)
    const privileged = normalizeNavigation(old, admin)
    assert.equal(privileged[1].url, '/admin')
    assert.equal(privileged[1].children[0].url, '/admin/role')
    assert.equal(old[0].children[0].url, '/admin/source')
    const customized = [{ code: 'MANAGEMENT', url: '/workspace', i18nKey: 'common.source', children: [{ url: '/source' }] }]
    assert.equal(normalizeNavigation(customized, user)[0].i18nKey, 'common.source')
})

test('menu rows of retired pages are dropped even from a cached menu', () => {
    const menus = normalizeNavigation([{ code: 'SYSTEM', url: '/admin', i18nKey: 'common.system', children: [
        { code: 'USERS', url: '/admin/user' },
        { code: 'RUNTIME_CONFIGURE', url: '/admin/configure', i18nKey: 'configure.runtime.title' },
        { code: 'SYSTEM_SETTINGS', url: '/admin/settings' },
    ] }], admin)
    assert.deepEqual(menus[0].children.map(menu => menu.code), ['USERS', 'SYSTEM_SETTINGS'])
})

test('a chosen data screen opens as the home page unless the default home is asked for', async () => {
    try {
        globalThis.__homePage = ''
        const plain = routerFor(user)
        await plain.push('/home')
        assert.equal(plain.currentRoute.value.path, '/home')

        globalThis.__homePage = 'SCREEN'
        const chosen = routerFor(user)
        await chosen.push('/home')
        assert.equal(chosen.currentRoute.value.path, '/home/screen/SCREEN')
        assert.equal(chosen.currentRoute.value.meta.workflowWorkspace, true)
        await chosen.push('/home?default=1')
        assert.equal(chosen.currentRoute.value.path, '/home')
    }
    finally {
        delete globalThis.__homePage
    }
})
