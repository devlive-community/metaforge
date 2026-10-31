import assert from 'node:assert/strict'
import test from 'node:test'
import { effectScope, nextTick, ref } from 'vue'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const { useTableCreateIndexes } = await sourceModule('../../metaforge-web/src/composables/useTableCreateIndexes.ts')

function draft(initial = [{ name: 'id' }, { name: 'email' }]) {
  const columns = ref(initial)
  const scope = effectScope()
  const indexes = scope.run(() => useTableCreateIndexes(() => columns.value, () => 'users'))
  return { columns, indexes, stop: () => scope.stop() }
}

function add(indexes, kind = 'normal') {
  indexes.indexTab.value = kind
  indexes.onAddIndex()
  return indexes.indexRows.value.at(-1)
}

test('index choices come from defined columns and no draft indexes are prefilled', () => {
  const state = draft([{ name: ' id ' }, { name: '' }, { name: 'email' }, { name: 'email' }])
  try {
    assert.deepEqual(state.indexes.columnOptions.value, [{ label: 'id', value: 'id' }, { label: 'email', value: 'email' }])
    assert.deepEqual(state.indexes.indexRows.value, [])
  }
  finally { state.stop() }
})

test('live ordinary and unique indexes are serialized without confirming individual rows', () => {
  const state = draft()
  try {
    const ordinary = add(state.indexes)
    ordinary.name = ' idx_lookup '
    ordinary.columns = ['email', 'id']
    ordinary.comment = 'lookup'
    const unique = add(state.indexes, 'unique')
    unique.columns = ['email']
    assert.equal(state.indexes.firstInvalidIndex(), undefined)
    assert.deepEqual(state.indexes.indexDefinitions.value, [
      { name: 'idx_lookup', columns: ['email', 'id'], unique: false, comment: 'lookup' },
      { name: unique.name, columns: ['email'], unique: true, comment: undefined }
    ])
    ordinary.name = 'idx_changed'
    ordinary.columns = ['id']
    assert.equal(state.indexes.indexDefinitions.value[0].name, 'idx_changed')
    assert.deepEqual(state.indexes.indexDefinitions.value[0].columns, ['id'])
  }
  finally { state.stop() }
})

test('incomplete indexes are found even when another category is visible', () => {
  const state = draft()
  try {
    const incomplete = add(state.indexes)
    state.indexes.indexTab.value = 'unique'
    assert.equal(state.indexes.firstInvalidIndex(), incomplete)
    incomplete.columns = ['missing_column']
    assert.equal(state.indexes.firstInvalidIndex(), incomplete)
    incomplete.columns = ['id']
    incomplete.name = '  '
    assert.equal(state.indexes.firstInvalidIndex(), incomplete)
    incomplete.name = 'idx_id'
    assert.equal(state.indexes.firstInvalidIndex(), undefined)
  }
  finally { state.stop() }
})

test('renaming and deleting fields updates selected index columns without changing their order', async () => {
  const state = draft()
  try {
    const index = add(state.indexes)
    index.columns = ['email', 'id']
    const previousPayload = state.indexes.indexDefinitions.value
    state.columns.value[0].name = 'account_id'
    await nextTick()
    assert.deepEqual(index.columns, ['email', 'account_id'])
    assert.deepEqual(previousPayload[0].columns, ['email', 'id'])
    state.columns.value.splice(1, 1)
    await nextTick()
    assert.deepEqual(index.columns, ['account_id'])
    state.columns.value = []
    await nextTick()
    assert.deepEqual(index.columns, [])
    assert.equal(state.indexes.firstInvalidIndex(), index)
  }
  finally { state.stop() }
})

test('delete removes manual indexes and synchronizes generated indexes with field flags', () => {
  const state = draft([{ name: 'id', primaryKey: true }, { name: 'email', unique: true }])
  try {
    const normal = add(state.indexes)
    state.indexes.removeIndex(normal)
    assert.equal(state.indexes.indexCounts.value.normal, 0)
    state.indexes.indexTab.value = 'primary'
    state.indexes.removeIndex(state.indexes.visibleIndexRows.value[0])
    assert.equal(state.columns.value[0].primaryKey, false)
    assert.equal(state.indexes.indexCounts.value.primary, 0)
    state.indexes.indexTab.value = 'unique'
    state.indexes.removeIndex(state.indexes.visibleIndexRows.value[0])
    assert.equal(state.columns.value[1].unique, false)
    assert.equal(state.indexes.indexCounts.value.unique, 0)
    assert.deepEqual(state.indexes.indexDefinitions.value, [])
  }
  finally { state.stop() }
})
