<template>
  <MfModal :open="open" width="680px" @update:open="value => emit('update:open', value)">
    <template #title>
      <span v-if="executed" class="flex items-center gap-3">
        <span class="inline-flex size-10 shrink-0 items-center justify-center rounded-mf-md" :class="executed.success ? 'bg-success/10 text-success' : 'bg-danger/10 text-danger'">
          <CircleCheck v-if="executed.success" :size="22"/><CircleX v-else :size="22"/>
        </span>
        <span class="flex min-w-0 flex-col">
          <span class="truncate">{{ $t('source.executed.title') }}</span>
          <span class="mt-0.5 truncate text-xs font-normal text-content-tertiary">{{ $t('source.executed.subtitle') }}</span>
        </span>
      </span>
    </template>
    <div v-if="executed" class="flex flex-col gap-4">
      <div class="flex items-start justify-between gap-4 rounded-mf-md px-5 py-4" :class="executed.success ? 'bg-success/5' : 'bg-danger/5'">
        <dl class="m-0 grid grid-cols-[auto_minmax(0,1fr)] gap-x-8 gap-y-2 text-sm">
          <dt class="text-content-secondary">{{ $t('source.executed.operation') }}</dt><dd class="m-0 text-content">{{ executed.operation }}</dd>
          <dt class="text-content-secondary">{{ $t('source.executed.table') }}</dt><dd class="m-0 break-all text-content">{{ executed.table }}</dd>
          <template v-if="executed.target"><dt class="text-content-secondary">{{ executed.target.label }}</dt><dd class="m-0 break-all text-content">{{ executed.target.value }}</dd></template>
          <dt class="text-content-secondary">{{ $t('source.executed.time') }}</dt><dd class="m-0 tabular-nums text-content">{{ executed.executedAt }}</dd>
        </dl>
        <div class="flex shrink-0 flex-col items-end gap-1.5">
          <span class="inline-flex items-center gap-1.5 rounded-mf-md px-2.5 py-1 text-sm font-medium" :class="executed.success ? 'bg-success/10 text-success' : 'bg-danger/10 text-danger'">
            <CircleCheck v-if="executed.success" :size="15"/><CircleX v-else :size="15"/>{{ $t(executed.success ? 'source.executed.success' : 'source.executed.failed') }}
          </span>
          <span class="text-xs tabular-nums" :class="executed.success ? 'text-success' : 'text-danger'">{{ $t('source.executed.elapsed', { ms: executed.elapsed }) }}</span>
        </div>
      </div>
      <MfTabs v-model:active-key="tab" class="executed-tabs">
        <MfTabPane item-key="sql" :tab="$t('source.executed.sqlTab')"/>
        <MfTabPane item-key="result" :tab="$t('source.executed.resultTab')"/>
        <MfTabPane item-key="formatted" :tab="$t('source.executed.formattedTab')"/>
      </MfTabs>
      <div v-if="tab !== 'result'" class="overflow-hidden rounded-mf-md bg-code-bg">
        <div class="flex items-center justify-between bg-code-header px-3 py-2">
          <span class="rounded-mf-sm bg-code-bg px-2 py-0.5 text-xs font-medium text-code-muted">SQL</span>
          <button type="button" class="inline-flex items-center gap-1.5 rounded-mf-sm px-2 py-1 text-xs text-code-fg hover:bg-code-bg" :disabled="!text" @click="copy"><Copy :size="13"/>{{ $t('source.executed.copy') }}</button>
        </div>
        <pre v-if="text" class="m-0 max-h-80 overflow-auto whitespace-pre-wrap break-words px-4 py-3 font-mono text-[13px] leading-6 text-code-fg"><span v-for="(token, index) in tokens" :key="index" :class="TOKEN_CLASS[token.kind]">{{ token.text }}</span></pre>
        <p v-else class="m-0 px-4 py-6 text-center text-sm text-code-muted">{{ $t('source.executed.noSql') }}</p>
      </div>
      <div v-else class="flex flex-col gap-3">
        <MfAlert :variant="executed.success ? 'success' : 'danger'" :title="executed.message || $t(executed.success ? 'source.executed.noResult' : 'source.executed.failed')"/>
        <div v-if="executed.result" class="max-h-72 overflow-auto rounded-mf-md border border-line">
          <MfTable :columns="executed.result.headers.map(name => ({ title: name, dataIndex: name }))" :data-source="executed.result.rows" :row-key="'__row'"
                   density="compact" :bordered="false" :empty-text="$t('common.noData')"/>
        </div>
      </div>
    </div>
    <template #footer>
      <MfButton :disabled="!text" @click="download"><Download :size="14"/>{{ $t('source.executed.download') }}</MfButton>
      <MfButton variant="primary" @click="emit('update:open', false)">{{ $t('source.executed.close') }}</MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { CircleCheck, CircleX, Copy, Download } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import { ObjectUtils } from '@/utils/object'
import { formatSql, joinStatements, tokenizeSql, type ExecutedSql, type SqlTokenKind } from '@/model/executed-sql'

defineOptions({ name: 'ExecutedSqlDialog' })
const props = defineProps<{ open: boolean; executed: ExecutedSql | null }>()
const emit = defineEmits<{ 'update:open': [value: boolean] }>()
const { t } = useI18n()
const TOKEN_CLASS: Record<SqlTokenKind, string> = {
  keyword: 'text-code-keyword', type: 'text-code-type', string: 'text-code-string', identifier: 'text-code-identifier',
  number: 'text-code-number', comment: 'text-code-muted italic', space: '', plain: '',
}
const tab = ref('sql')
watch(() => props.open, open => { if (open) tab.value = 'sql' })
const raw = computed(() => joinStatements(props.executed?.statements || []))
const text = computed(() => tab.value === 'formatted' ? formatSql(raw.value) : raw.value)
const tokens = computed(() => tokenizeSql(text.value))
function copy() {
  ObjectUtils.copy(text.value)
  MfMessage.success(t('query.tip.copySuccess'))
}
function download() {
  const url = URL.createObjectURL(new Blob([raw.value + '\n'], { type: 'application/sql;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `${props.executed?.table || 'statement'}-${(props.executed?.executedAt || '').replace(/[: ]/g, '-')}.sql`
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
/* The tabs only switch the view below them; their empty panes would add a gap. */
.executed-tabs :deep([role='tabpanel']) { display: none; }
</style>
