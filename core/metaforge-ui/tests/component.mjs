import { readFile } from 'node:fs/promises'
import { extname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { compileScript, parse } from 'vue/compiler-sfc'
import ts from 'typescript'

const modules = new Map()

async function moduleUrl(url) {
    const filename = fileURLToPath(url)
    if (!modules.has(filename)) {
        modules.set(filename, (async () => {
            let source = await readFile(url, 'utf8')
            if (filename.endsWith('.vue')) {
                const { descriptor } = parse(source, { filename })
                source = compileScript(descriptor, { id: filename, inlineTemplate: true }).content
            }
            const { outputText } = ts.transpileModule(source, {
                compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.ESNext },
            })
            const imports = [...outputText.matchAll(/from (['"])([^'"]+)\1/g)]
            const resolved = new Map()
            for (const [, , specifier] of imports) {
                if (resolved.has(specifier)) continue
                const target = specifier.startsWith('.')
                    ? await moduleUrl(new URL(extname(specifier) ? specifier : `${specifier}.ts`, url))
                    : specifier === 'vue'
                        ? new URL('./vue-runtime.mjs', import.meta.url).href
                        : import.meta.resolve(specifier)
                resolved.set(specifier, target)
            }
            const code = outputText.replace(/from (['"])([^'"]+)\1/g, (_match, _quote, specifier) => `from ${JSON.stringify(resolved.get(specifier))}`)
            return `data:text/javascript;base64,${Buffer.from(code).toString('base64')}`
        })())
    }
    return modules.get(filename)
}

export async function sourceModule(path) {
    return import(await moduleUrl(new URL(path, import.meta.url)))
}

export async function component(name) {
    return (await sourceModule(`../src/components/${name}.vue`)).default
}
