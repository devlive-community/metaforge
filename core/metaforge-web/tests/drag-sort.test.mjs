import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = (await readFile(new URL('../src/composables/useDragSort.ts', import.meta.url), 'utf8')).replace("from 'vue'", `from '${import.meta.resolve('vue')}'`)
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { useDragSort } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

const event = () => ({ dataTransfer: { setData() {}, effectAllowed: '' } })

test('the drop target shows the side the dragged row lands on', () => {
  const sort = useDragSort(() => ['a', 'b', 'c', 'd'])
  sort.start('b', event())
  sort.enter('d')
  assert.equal(sort.dropSide('d'), 'after')
  assert.equal(sort.dropSide('c'), null)
  sort.enter('a')
  assert.equal(sort.dropSide('a'), 'before')
  sort.enter('b')
  assert.equal(sort.dropSide('b'), null)
})

test('dropping ends the drag and ignores drops on the dragged row', () => {
  const sort = useDragSort(() => ['a', 'b'])
  sort.start('a', event())
  sort.enter('b')
  assert.equal(sort.drop('b'), 'a')
  assert.equal(sort.dragging.value, null)
  assert.equal(sort.over.value, null)
  sort.start('a', event())
  assert.equal(sort.drop('a'), null)
  sort.enter('b')
  assert.equal(sort.over.value, null)
})
