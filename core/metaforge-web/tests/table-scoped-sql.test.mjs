import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/table-scoped-sql.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { checkTableScope } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)
const check = sql => checkTableScope(sql, 'app', 'orders')

test('the same statements as the server check are allowed', () => {
  for (const sql of [
    'SELECT * FROM orders',
    "select o.id from `app`.`orders` as o where o.state = 'x; FROM other' limit 10;",
    'SELECT * FROM orders a JOIN orders b ON a.parent = b.id',
    'SELECT * FROM orders WHERE id IN (SELECT parent FROM ORDERS) -- FROM other',
    'INSERT INTO orders (id) VALUES (1) ON DUPLICATE KEY UPDATE id = 2',
    "UPDATE orders SET state = 'from users' WHERE id = 1",
    'DELETE FROM orders WHERE id = 1',
    'SELECT * FROM orders o, orders p WHERE o.id = p.id',
  ]) assert.equal(check(sql).ok, true, sql)
})

test('other tables and statements are rejected with a reason', () => {
  const issue = sql => { const result = check(sql); return result.ok ? null : [result.issue, result.detail] }
  assert.deepEqual(issue('SELECT * FROM users'), ['otherTable', 'users'])
  assert.deepEqual(issue('SELECT * FROM other.orders'), ['otherTable', 'other.orders'])
  assert.deepEqual(issue('SELECT * FROM orders UNION SELECT * FROM users'), ['otherTable', 'users'])
  assert.deepEqual(issue('SELECT * FROM orders; DROP TABLE orders'), ['multiple', undefined])
  assert.deepEqual(issue('SHOW TABLES'), ['statement', undefined])
  assert.deepEqual(issue('SELECT 1'), ['noTable', undefined])
  assert.deepEqual(issue("SELECT * FROM orders INTO OUTFILE '/tmp/x'"), ['into', undefined])
  assert.deepEqual(issue("SELECT LOAD_FILE('/etc/passwd') FROM orders"), ['function', 'LOAD_FILE'])
  assert.deepEqual(issue('  -- nothing\n'), ['empty', undefined])
})

test('unqualified references are qualified with the database for reuse elsewhere', () => {
  const result = check('SELECT * FROM orders o /* keep */ WHERE id IN (SELECT id FROM `app`.orders) AND x IN (SELECT 1 FROM `orders`)')
  assert.equal(result.qualified, 'SELECT * FROM `app`.`orders` o /* keep */ WHERE id IN (SELECT id FROM `app`.orders) AND x IN (SELECT 1 FROM `app`.`orders`)')
})
