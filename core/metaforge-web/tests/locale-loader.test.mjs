import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { h } from 'vue'
import ts from 'typescript'
import { mountHost } from '../../metaforge-ui/tests/interaction-host.mjs'

// The app bundles its locale packs now; exercise that implementation instead
// of the removed HTTP locale-loader module.
const moduleUrl = source => `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
const packs = Object.fromEntries(await Promise.all(['en','zh-cn'].map(async locale => [locale,JSON.parse(await readFile(new URL(`../src/i18n/locales/${locale}.json`,import.meta.url),'utf8'))])))
const storage = new Map([['locale','zh-CN']])
globalThis.localStorage = { getItem:key=>storage.get(key) || null, setItem:(key,value)=>storage.set(key,value) }
const source = await readFile(new URL('../src/i18n/I18n.ts',import.meta.url),'utf8')
let {outputText} = ts.transpileModule(source,{compilerOptions:{target:ts.ScriptTarget.ES2020,module:ts.ModuleKind.ESNext}})
const imports = {'./locales/en.json':moduleUrl('export default '+JSON.stringify(packs.en)),'./locales/zh-cn.json':moduleUrl('export default '+JSON.stringify(packs['zh-cn']))}
outputText=outputText.replace(/from (['"])([^'"]+)\1/g,(_m,_q,name)=>`from ${JSON.stringify(imports[name] || import.meta.resolve(name))}`)
const {default:i18n,provideI18nHandler,loadingState,errorState} = await import(moduleUrl(outputText))
let handler
const host=mountHost(h({setup(){handler=provideI18nHandler();return ()=>h('div')}}))
test.after(()=>host.unmount())

test('bundled messages are available immediately, including setup fallbacks',()=>{
  assert.equal(i18n.global.locale.value,'zh-cn')
  assert.equal(i18n.global.t('common.home'),'首页')
  assert.equal(i18n.global.t('setup.title'),'安装 MetaForge')
})

test('stored and browser-style aliases normalize consistently',async()=>{
  for(const locale of ['zh','zh-CN','zh_cn']){
    assert.equal(await handler.loadLocale(locale),true)
    assert.equal(i18n.global.locale.value,'zh-cn')
    assert.equal(storage.get('locale'),'zh-cn')
  }
  for(const locale of ['en','en-US','en_GB','fr-FR']){
    await handler.loadLocale(locale)
    assert.equal(i18n.global.locale.value,'en')
    assert.equal(storage.get('locale'),'en')
  }
})

test('switching locales works without a server or network request',async()=>{
  const original=globalThis.fetch
  globalThis.fetch=()=>{throw new Error('Locale switching must remain local')}
  try{
    for(const [locale,label] of [['en','Home'],['zh-CN','首页'],['en-US','Home']]){
      await handler.loadLocale(locale)
      assert.equal(i18n.global.t('common.home'),label)
      assert.equal(loadingState.value,false)
      assert.equal(errorState.value,false)
    }
  }finally{globalThis.fetch=original}
})

test('source prototype translations exist in both packs and interpolate named values',async()=>{
  const keys=Object.keys(packs.en).filter(key=>key.startsWith('source.prototype.'))
  assert.ok(keys.length>0)
  for(const locale of ['en','zh-cn']){
    await handler.loadLocale(locale)
    for(const key of keys)assert.ok(packs[locale][key],`${locale} is missing ${key}`)
    assert.match(i18n.global.t('source.prototype.indexFailed',{message:'DENIED'}),/DENIED/)
    assert.match(i18n.global.t('source.structure.commentCounter',{count:12}),/12\/500/)
  }
})
