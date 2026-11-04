#!/usr/bin/env node
// Builds the plugin store catalogue (metadata.json) from every plugin module's
// plugin.json, and optionally the ForgeStats release catalog. plugin.json sits
// next to capabilities.json in src/main/resources/org/devlive/metaforge/plugin,
// so it also ships inside the plugin jar and the server can show it offline.
//
//   node configure/publish/build-metadata.mjs                       # refresh configure/metadata.json
//   node configure/publish/build-metadata.mjs --dist dist/plugins \
//        --out dist/metadata.json --catalog dist/release-catalog.json \
//        --commit "$GITHUB_SHA" --generation "$GITHUB_RUN_NUMBER" --released now --strict
//
// plugin.json holds what only people know: label,
// description, logo, type, author, supported data source versions (supportVersion),
// the minimum MetaForge version (requires, defaults to the build version), extra
// features and the store order. The build adds the rest: version, released time,
// package size and SHA-256 from the built archive, download sources, and the
// feature list derived from the capabilities the plugin declares in code.
import { createHash } from 'node:crypto'
import { createReadStream, existsSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '../..')
const MODULE_ROOTS = ['plugin', 'convert', 'executor', 'fs', 'scheduler', 'notify', 'parser']
const DECLARATIONS = 'src/main/resources/org/devlive/metaforge/plugin'
const CAPABILITIES = `${DECLARATIONS}/capabilities.json`
const DESCRIPTOR = `${DECLARATIONS}/plugin.json`
const MAVEN_BASE = 'https://repo1.maven.org/maven2'
const CDN_BASE = 'https://cdn.north.devlive.org/applications/metaforge/plugins'
const GROUP_PATH = 'org/devlive/metaforge'

// Feature codes the store translates (store.feature.<code>). Each is offered
// when the plugin declares any of the listed management capabilities.
const FEATURES = [
  ['query', ['query:execute']],
  ['database', ['database:list', 'database:info', 'database:alter']],
  ['browse', ['table:list', 'table:info', 'table:read', 'tab:data']],
  ['schema', ['table:create', 'table:alter', 'table:drop', 'table:truncate', 'column:create', 'column:modify', 'column:drop', 'column:reorder']],
  ['ddl', ['table:statement', 'tab:statement']],
  ['index', ['index:read', 'index:create', 'index:drop']],
  ['foreignKey', ['foreign-key:read', 'foreign-key:create', 'foreign-key:drop']],
  ['trigger', ['trigger:read', 'trigger:create', 'trigger:drop']],
  ['dataEdit', ['data:insert', 'data:modify', 'data:delete']],
  ['export', ['table:export']],
  ['erDiagram', ['tab:er-diagram']],
  ['keyValue', ['namespace:list', 'key:list', 'key:read']],
  ['objectStorage', ['object:list', 'object:read', 'object:download']],
]

function args(argv) {
  const out = { out: join(ROOT, 'configure/metadata.json') }
  for (let i = 0; i < argv.length; i++) {
    const flag = argv[i]
    if (flag === '--strict') { out.strict = true; continue }
    const value = argv[++i]
    if (!flag.startsWith('--') || value === undefined) throw new Error(`Unknown or incomplete argument: ${flag}`)
    out[flag.slice(2)] = value
  }
  return out
}

function projectVersion() {
  const pom = readFileSync(join(ROOT, 'pom.xml'), 'utf8')
  const project = pom.slice(pom.indexOf('<artifactId>metaforge</artifactId>'))
  const match = project.match(/<version>([^<]+)<\/version>/)
  if (!match) throw new Error('Cannot read the project version from pom.xml')
  return match[1].trim()
}

// The CDN layout of configure/publish/publish-oss-artifact.sh.
function cdnDirectory(key) {
  for (const type of ['executor', 'convert', 'fs', 'parser', 'scheduler']) {
    if (key.startsWith(`metaforge-${type}-`)) return type
  }
  return 'plugin'
}

function features(moduleDir, extra) {
  const file = join(moduleDir, CAPABILITIES)
  const declared = new Set(existsSync(file) ? JSON.parse(readFileSync(file, 'utf8')).capabilities ?? [] : [])
  const codes = FEATURES.filter(([, caps]) => caps.some(cap => declared.has(cap))).map(([code]) => code)
  return [...codes, ...(Array.isArray(extra) ? extra.filter(item => typeof item === 'string' && item.trim()) : [])]
}

function sha256(file) {
  return new Promise((done, fail) => {
    const hash = createHash('sha256')
    createReadStream(file).on('data', chunk => hash.update(chunk)).on('error', fail).on('end', () => done(hash.digest('hex')))
  })
}

function modules() {
  const found = []
  for (const root of MODULE_ROOTS) {
    const base = join(ROOT, root)
    if (!existsSync(base)) continue
    for (const name of readdirSync(base)) {
      const dir = join(base, name)
      if (existsSync(join(dir, DESCRIPTOR)) && existsSync(join(dir, 'pom.xml'))) found.push({ key: name, dir })
    }
  }
  return found
}

async function main() {
  const opts = args(process.argv.slice(2))
  const version = opts.version ?? projectVersion()
  const released = opts.released === 'now' ? new Date().toISOString().replace('T', ' ').replace(/\.\d+Z$/, '') : opts.released
  const children = []
  const packages = []
  const missing = []
  for (const { key, dir } of modules()) {
    const config = JSON.parse(readFileSync(join(dir, DESCRIPTOR), 'utf8'))
    if (typeof config.label !== 'string' || !config.label.trim()) throw new Error(`${key}/${DESCRIPTOR} needs a label`)
    const mavenPath = `${GROUP_PATH}/${key}/${version}/${key}-${version}-bin.tar.gz`
    const cdnPath = `${version}/${cdnDirectory(key)}/${key}-bin.tar.gz`
    const entry = {
      key,
      label: config.label,
      description: config.description,
      i18nFormat: config.i18nFormat === true,
      type: config.type,
      version,
      requires: config.requires ?? version,
      author: config.author,
      logo: config.logo,
      released,
      supportVersion: Array.isArray(config.supportVersion) ? config.supportVersion : undefined,
      features: features(dir, config.features),
      // url stays the Maven Central package for MetaForge versions before the
      // multi-source store; newer servers pick from sources.
      url: `${MAVEN_BASE}/${mavenPath}`,
      sources: { maven: `${MAVEN_BASE}/${mavenPath}`, cdn: `${CDN_BASE}/${cdnPath}` },
      order: Number.isFinite(config.order) ? config.order : Number.MAX_SAFE_INTEGER,
    }
    if (opts.dist) {
      const archive = join(resolve(opts.dist), `${key}-bin.tar.gz`)
      if (existsSync(archive)) {
        entry.size = statSync(archive).size
        entry.sha256 = await sha256(archive)
        packages.push({ id: key, version, sha256: entry.sha256, size: entry.size, status: 'active', sources: { maven: mavenPath, cdn: cdnPath } })
      }
      else {
        missing.push(key)
      }
    }
    children.push(entry)
  }
  if (opts.strict && missing.length) throw new Error(`Missing built packages: ${missing.join(', ')}`)
  children.sort((a, b) => a.order - b.order || a.key.localeCompare(b.key))
  for (const entry of children) delete entry.order
  const metadata = {
    key: 'plugin',
    label: 'common.plugin.list.name',
    description: 'common.plugin.list.description',
    i18nFormat: true,
    version,
    children: JSON.parse(JSON.stringify(children)),
  }
  writeFileSync(resolve(opts.out), JSON.stringify(metadata, null, 2) + '\n')
  console.log(`metadata: ${children.length} plugin(s) -> ${opts.out}${missing.length ? ` (${missing.length} without built package)` : ''}`)
  if (opts.catalog) {
    if (!/^[0-9a-f]{40}$/.test(opts.commit ?? '') || !/^[1-9]\d*$/.test(opts.generation ?? '')) {
      throw new Error('--catalog needs --commit <40 hex> and --generation <positive integer>')
    }
    if (!packages.length) throw new Error('--catalog needs --dist with built packages')
    const catalog = { schemaVersion: 2, app: 'metaforge', sourceCommit: opts.commit, generation: Number(opts.generation), plugins: packages }
    writeFileSync(resolve(opts.catalog), JSON.stringify(catalog, null, 2) + '\n')
    console.log(`catalog: ${packages.length} package(s) -> ${opts.catalog}`)
  }
}

main().catch(error => {
  console.error(error.message)
  process.exit(1)
})
