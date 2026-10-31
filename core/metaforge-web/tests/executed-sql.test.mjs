import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/executed-sql.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { formatSql, joinStatements, tokenizeSql, formatTimestamp } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('tokens keep quoted text whole and classify keywords and types', () => {
  const tokens = tokenizeSql("ALTER TABLE `a``b` ADD COLUMN `c` varchar(255) COMMENT 'it''s -- not a comment' -- trailing")
    .filter(token => token.kind !== 'space')
  assert.deepEqual(tokens.map(token => token.kind), ['keyword', 'keyword', 'identifier', 'keyword', 'keyword', 'identifier', 'type', 'plain', 'number', 'plain', 'keyword', 'string', 'comment'])
  assert.equal(tokens[2].text, '`a``b`')
  assert.equal(tokens[11].text, "'it''s -- not a comment'")
})

test('DDL is laid out one clause per line like the prototype', () => {
  const sql = "ALTER TABLE `metaforge_workflow` ADD COLUMN `remark` varchar(255) NULL DEFAULT NULL COMMENT '备注信息' AFTER `created_by`;"
  assert.equal(formatSql(sql), [
    'ALTER TABLE `metaforge_workflow`',
    '  ADD COLUMN `remark` varchar(255)',
    '  NULL DEFAULT NULL',
    "  COMMENT '备注信息'",
    '  AFTER `created_by`;',
  ].join('\n'))
})

test('DML keeps value lists inline and separates statements', () => {
  assert.equal(formatSql("INSERT INTO `t` (`a`, `b`) VALUES (1, 'x, y');UPDATE `t` SET `a` = 2 WHERE `a` = 1 AND `b` IS NULL;"), [
    'INSERT INTO `t` (`a`, `b`)', "  VALUES (1, 'x, y');", '', 'UPDATE `t`', '  SET `a` = 2', '  WHERE `a` = 1', '  AND `b` IS NULL;',
  ].join('\n'))
  assert.equal(joinStatements(['SELECT 1', ' SELECT 2; ', '']), 'SELECT 1;\n\nSELECT 2;')
  assert.equal(formatTimestamp(new Date(2026, 9, 29, 10, 49, 5)), '2026-10-29 10:49:05')
})
