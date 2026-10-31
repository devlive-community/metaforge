import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/source-trigger.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { copyName, emptyTriggerForm, formToTrigger, mapTriggers, triggerToForm, validateTriggerForm } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('trigger rows map to triggers with normalized timing, event and time', () => {
  const [trigger] = mapTriggers([{ trigger_name: 'trg_user_bi', timing: 'before', event: 'insert', body: 'BEGIN END', definer: 'root@%', created: '2026-10-12T10:22:18.45', sql_mode: 'STRICT_TRANS_TABLES', charset: 'utf8mb4' }, { body: 'x' }])
  assert.deepEqual(trigger, { name: 'trg_user_bi', timing: 'BEFORE', event: 'INSERT', body: 'BEGIN END', definer: 'root@%', created: '2026-10-12 10:22:18', sqlMode: 'STRICT_TRANS_TABLES', charset: 'utf8mb4' })
})

test('copies get a free name and edits carry the original name', () => {
  assert.equal(copyName('trg', ['trg', 'trg_copy', 'TRG_COPY2']), 'trg_copy3')
  const trigger = mapTriggers([{ trigger_name: 'trg', timing: 'AFTER', event: 'DELETE', body: ' BEGIN SET @a = 1; END ', definer: '' }])[0]
  assert.deepEqual(formToTrigger(triggerToForm(trigger), 'trg'), { name: 'trg', timing: 'AFTER', event: 'DELETE', definer: undefined, body: 'BEGIN SET @a = 1; END', originalName: 'trg' })
})

test('validation requires a free name and a real body', () => {
  assert.deepEqual(validateTriggerForm({ ...emptyTriggerForm('user'), name: '' }, []), { name: 'nameRequired', body: 'bodyRequired' })
  assert.deepEqual(validateTriggerForm({ ...emptyTriggerForm('user'), body: 'SET NEW.a = 1' }, ['TRG_USER_BI']), { name: 'nameTaken' })
  assert.deepEqual(validateTriggerForm({ ...emptyTriggerForm('user'), body: 'SET NEW.a = 1' }, ['trg_user_bi'], 'trg_user_bi'), {})
})
