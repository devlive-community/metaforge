<template>
  <div class="relative overflow-hidden rounded-mf-md bg-code-bg" :class="fill && 'flex min-h-0 flex-1 flex-col'">
    <button v-if="copyable && code" type="button" class="absolute right-2 top-2 z-10 inline-flex items-center gap-1.5 rounded-mf-sm bg-code-header px-2 py-1 text-xs text-code-fg hover:bg-code-bg"
            @click="copy"><Copy :size="13"/>{{ $t('source.executed.copy') }}</button>
    <!-- One row per source line, so a wrapped line keeps its number beside it. -->
    <div v-if="code" class="overflow-auto py-2.5 font-mono leading-5" :class="[compact ? 'text-[11px]' : 'text-xs', fill ? 'min-h-0 flex-1' : maxHeight]">
      <div v-for="(line, number) in lines" :key="number" class="flex">
        <span class="w-9 shrink-0 select-none border-r border-code-header pr-2 text-right text-code-muted" aria-hidden="true">{{ number + 1 }}</span>
        <pre class="m-0 min-w-0 flex-1 whitespace-pre-wrap break-words pl-3 pr-16 text-code-fg"><span v-for="(token, index) in line" :key="index" :class="TOKEN_CLASS[token.kind]">{{ token.text }}</span>{{ line.length ? '' : ' ' }}</pre>
      </div>
    </div>
    <div v-else-if="fill" class="flex min-h-0 flex-1 items-center justify-center p-3">
      <MfEmpty :title="placeholder || '-'" size="sm">
        <template #title><span class="text-code-muted">{{ placeholder || '-' }}</span></template>
      </MfEmpty>
    </div>
    <p v-else class="m-0 px-3 py-2.5 text-xs text-code-muted">{{ placeholder || '-' }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Copy } from '@lucide/vue'
import { MfEmpty, MfMessage } from 'metaforge-ui'
import { ObjectUtils } from '@/utils/object'
import { tokenizeSql, type SqlToken, type SqlTokenKind } from '@/model/executed-sql'

defineOptions({ name: 'SqlCodeBlock' })
// Workspace mode fills the available height; compact previews keep their existing maximum height.
const props = withDefaults(defineProps<{ code: string; placeholder?: string; copyable?: boolean; compact?: boolean; maxHeight?: string; fill?: boolean }>(), {
  placeholder: '', copyable: true, compact: false, maxHeight: 'max-h-80', fill: false,
})
const { t } = useI18n()
const TOKEN_CLASS: Record<SqlTokenKind, string> = {
  keyword: 'text-code-keyword', type: 'text-code-type', string: 'text-code-string', identifier: 'text-code-identifier',
  number: 'text-code-number', comment: 'text-code-muted italic', space: '', plain: '',
}
/** Tokens split into source lines; tokens spanning lines (block comments) are cut at each break. */
const lines = computed(() => {
  const rows: SqlToken[][] = [[]]
  for (const token of tokenizeSql(props.code)) {
    token.text.split('\n').forEach((part, index) => {
      if (index > 0) rows.push([])
      if (part) rows[rows.length - 1].push({ kind: token.kind, text: part })
    })
  }
  return rows
})
function copy() {
  ObjectUtils.copy(props.code)
  MfMessage.success(t('query.tip.copySuccess'))
}
</script>
