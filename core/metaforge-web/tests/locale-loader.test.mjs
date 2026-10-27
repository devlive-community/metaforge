import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
import { createI18n } from 'vue-i18n'

const source = await readFile(new URL('../src/i18n/locale-loader.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, {
    compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.ESNext },
})
const { createLocaleLoader, normalizeLocale } = await import(`data:text/javascript,${encodeURIComponent(outputText)}`)

test('normalizes browser and stored locale aliases', () => {
    for (const locale of ['zh', 'zh-CN', 'zh_cn']) assert.equal(normalizeLocale(locale), 'zh-cn')
    for (const locale of ['en', 'en-US', 'en_GB', 'fr-FR']) assert.equal(normalizeLocale(locale), 'en')
})

test('fetches real packs despite built-in common/setup messages and preserves fallback keys', async () => {
    const i18n = createI18n({
        legacy: false,
        flatJson: true,
        locale: 'en',
        messages: {
            en: { common: { loading: 'Loading' }, setup: { title: 'Install' } },
            'zh-cn': { common: { loading: 'Loading' }, setup: { title: '安装' } },
        },
    })
    const calls = []
    const load = createLocaleLoader(async locale => {
        calls.push(locale)
        return { status: true, data: { 'common.home': locale === 'en' ? 'Home' : '首页', 'common.loading': 'Server loading' } }
    }, (locale, messages) => i18n.global.mergeLocaleMessage(locale, messages))

    for (const [locale, home, setup] of [['en', 'Home', 'Install'], ['zh_cn', '首页', '安装']]) {
        i18n.global.locale.value = await load(locale)
        assert.equal(i18n.global.t('common.home'), home)
        assert.equal(i18n.global.t('common.loading'), 'Server loading')
        assert.equal(i18n.global.t('setup.title'), setup)
    }
    await load('zh-CN')
    await load('en-US')
    assert.deepEqual(calls, ['en', 'zh-cn'])
})

test('retries a failed request instead of caching fallback messages as success', async () => {
    let calls = 0
    let applied = 0
    const load = createLocaleLoader(async () => {
        if (++calls === 1) throw new Error('Connection lost')
        return { status: true, data: { 'common.home': 'Home' } }
    }, () => applied++)
    await assert.rejects(load('en'), /Connection lost/)
    await load('en')
    assert.equal(calls, 2)
    assert.equal(applied, 1)
})

test('rejects empty or invalid packs and allows retry', async () => {
    for (const invalid of [{}, [], ['message'], null, 'message']) {
        let calls = 0
        const load = createLocaleLoader(async () => ({ status: true, message: 'Success', data: ++calls === 1 ? invalid : { title: 'Home' } }), () => {})
        await assert.rejects(load('en'), /Invalid locale data/)
        await load('en')
        assert.equal(calls, 2)
    }
})

test('rejects business failures without applying or caching their payload', async () => {
    let applied = false
    const load = createLocaleLoader(async () => ({ status: false, message: 'Unavailable', data: { title: 'Home' } }), () => { applied = true })
    await assert.rejects(load('en'), /Unavailable/)
    assert.equal(applied, false)
})

test('shares simultaneous requests for the same normalized locale', async () => {
    let resolve
    let calls = 0
    let applied = 0
    const response = new Promise(done => { resolve = done })
    const load = createLocaleLoader(() => { calls++; return response }, () => applied++)
    const first = load('zh_cn')
    const second = load('zh-CN')
    resolve({ status: true, data: { 'common.home': '首页' } })
    assert.deepEqual(await Promise.all([first, second]), ['zh-cn', 'zh-cn'])
    assert.equal(calls, 1)
    assert.equal(applied, 1)
})
