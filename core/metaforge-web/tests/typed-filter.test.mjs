import assert from 'node:assert/strict'
import test from 'node:test'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const { isCompleteFilter, newFilterRule, operatorsByKind } = await sourceModule('../../metaforge-web/src/model/typed-filter.ts')

test('numeric rules reject text operators, nonnumbers and reversed ranges', () => {
  const rule = newFilterRule({ key: 'amount', label: 'Amount', kind: 'number' })
  assert.deepEqual(operatorsByKind.number, ['eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'between'])
  assert.equal(isCompleteFilter({ ...rule, operator: 'contains', value: '4' }), false)
  assert.equal(isCompleteFilter({ ...rule, operator: 'gte', value: 'no value' }), false)
  assert.equal(isCompleteFilter({ ...rule, operator: 'gte', value: '-3.5' }), true)
  assert.equal(isCompleteFilter({ ...rule, operator: 'between', value: '10', end: '2' }), false)
})

test('date and boolean rules require typed values while legacy snapshots still load', () => {
  assert.equal(isCompleteFilter({ field: 'created', kind: 'date', operator: 'eq', value: 'tomorrow', end: '' }), false)
  assert.equal(isCompleteFilter({ field: 'created', kind: 'date', operator: 'eq', value: '2026-11-03', end: '' }), true)
  assert.equal(isCompleteFilter({ field: 'active', kind: 'boolean', operator: 'eq', value: 'false', end: '' }), true)
  assert.equal(isCompleteFilter({ field: 'active', kind: 'boolean', operator: 'eq', value: 'maybe', end: '' }), false)
  assert.equal(isCompleteFilter({ field: 'amount', operator: 'contains', value: '4', end: '' }), true)
})
