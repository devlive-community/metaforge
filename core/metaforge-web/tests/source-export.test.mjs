import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/source-export.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { exportBody, exportFormats, emptyExportForm, indentXml, previewTokens, readablePreview } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('installed convert plugins become formats, known ones first and raw rows left out', () => {
  const formats = exportFormats([
    { type: 'CONVERT', name: 'XmlConvert', key: 'metaforge-convert-xml' }, { type: 'CONVERT', name: 'NoneConvert' }, { type: 'CONNECTOR', name: 'MySQL' },
    { type: 'CONVERT', name: 'ParquetConvert', key: 'metaforge-convert-parquet' }, { type: 'CONVERT', name: 'JsonConvert', key: 'metaforge-convert-json' },
  ])
  assert.deepEqual(formats.map(format => [format.name, format.label, format.known]), [['JsonConvert', 'JSON', 'json'], ['XmlConvert', 'XML', 'xml'], ['ParquetConvert', 'PARQUET', null]])
})

test('the request turns the 1-based start row into an offset and keeps counts positive', () => {
  assert.deepEqual(exportBody({ ...emptyExportForm(), format: 'CsvConvert', scope: 'range', start: 11, count: 0 }),
    { format: 'CsvConvert', scope: 'range', offset: 10, limit: 1, includeStructure: true, compress: false, excludeNull: false })
})

test('previews are indented for reading and coloured by token', () => {
  assert.equal(readablePreview('json', '[{"id":1}]'), '[\n  {\n    "id": 1\n  }\n]')
  assert.equal(readablePreview('csv', 'id\n1'), 'id\n1')
  assert.equal(indentXml('<Root><Columns><Column name="id"/></Columns><Node id="1"/></Root>'),
    '<Root>\n  <Columns>\n    <Column name="id"/>\n  </Columns>\n  <Node id="1"/>\n</Root>')
  assert.deepEqual(previewTokens('json', '  "id": 1, "ok": true, "name": "a"')[0].filter(token => token.kind !== 'plain').map(token => [token.kind, token.text]),
    [['key', '"id"'], ['number', '1'], ['key', '"ok"'], ['literal', 'true'], ['key', '"name"'], ['string', '"a"']])
  assert.deepEqual(previewTokens('xml', '<Node id="1"/>')[0].map(token => token.kind), ['tag', 'key', 'string', 'tag'])
})
