<template>
  <MfModal :open="open" :width="dependencies.length ? '620px' : '500px'" :close-disabled="saving" :close-on-overlay="!saving" :close-on-esc="!saving"
           @update:open="value => !value && emit('close')">
    <template #title>
      <span class="flex items-center gap-3">
        <span class="inline-flex size-10 shrink-0 items-center justify-center rounded-mf-md bg-danger/10 text-danger"><Trash2 :size="20"/></span>
        <span>{{ $t('source.drop.columnTitle') }}</span>
      </span>
    </template>
    <div class="flex flex-col gap-4 pl-13">
      <div>
        <p class="m-0 text-base font-semibold text-content">{{ $t(dependencies.length ? 'source.drop.columnDependQuestion' : 'source.drop.columnQuestion', { name: quoted }) }}</p>
        <p class="m-0 mt-1.5 text-sm text-content-tertiary">{{ $t(dependencies.length ? 'source.drop.columnDependHint' : 'source.drop.columnHint') }}</p>
      </div>
      <template v-if="!dependencies.length">
        <div class="rounded-mf-md bg-danger/5 px-4 py-3 text-sm">
          <p class="m-0 inline-flex items-center gap-2 font-medium text-danger"><CircleAlert :size="16"/>{{ $t('source.drop.impactTitle') }}</p>
          <ul class="m-0 mt-2 flex list-disc flex-col gap-1 pl-10 text-content-secondary">
            <li>{{ $t('source.drop.impactData', { name: quoted }) }}</li>
            <li>{{ $t('source.drop.impactConfig') }}</li>
            <li>{{ $t('source.drop.impactIrreversible') }}</li>
          </ul>
        </div>
      </template>
      <template v-else>
        <div class="-ml-13 flex flex-col gap-3">
          <div role="tablist" class="flex gap-1 border-b border-line">
            <button v-for="type in DEPENDENCY_TYPES" :key="type" type="button" role="tab" :aria-selected="tab === type"
                    class="-mb-px border-b-2 px-4 py-2 text-sm" :class="tab === type ? 'border-primary font-medium text-primary' : 'border-transparent text-content-secondary hover:text-content'"
                    @click="tab = type">{{ $t(`source.drop.type.${type}`) }} <span class="ml-1 tabular-nums">{{ counts[type] }}</span></button>
          </div>
          <div class="max-h-56 overflow-auto rounded-mf-md border border-line">
            <MfTable v-if="current.length" :columns="headers" :data-source="current" row-key="key" density="compact" :bordered="false"/>
            <MfEmpty v-else size="sm" :title="$t('source.drop.noneOfType')"/>
          </div>
          <div class="rounded-mf-md bg-warning/10 px-4 py-3 text-sm">
            <p class="m-0 inline-flex items-center gap-2 font-medium text-warning"><CircleAlert :size="16"/>{{ $t('source.drop.adviceTitle') }}</p>
            <ul class="m-0 mt-2 flex list-disc flex-col gap-1 pl-10 text-content-secondary">
              <li>{{ $t('source.drop.adviceFirst') }}</li>
              <li>{{ $t('source.drop.adviceForce') }}</li>
            </ul>
          </div>
          <MfCheckbox v-model="force" :disabled="saving">{{ $t('source.drop.forceConfirm') }}</MfCheckbox>
        </div>
      </template>
    </div>
    <template #footer>
      <MfButton :disabled="saving" @click="emit('close')">{{ $t('common.cancel') }}</MfButton>
      <MfButton variant="danger" :loading="saving" :disabled="!!dependencies.length && !force" @click="emit('confirm', !!dependencies.length)">
        {{ $t(dependencies.length ? 'source.drop.dropField' : 'common.delete') }}
      </MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { CircleAlert, Trash2 } from '@lucide/vue'
import { countByType, DEPENDENCY_TYPES, type ColumnDependency, type DependencyType } from '@/model/column-dependency'

const props = defineProps<{ open: boolean; names: string[]; dependencies: ColumnDependency[]; saving: boolean }>()
const emit = defineEmits<{ close: []; confirm: [force: boolean] }>()
const { t, te } = useI18n()
const force = ref(false)
const tab = ref<DependencyType>('index')
const counts = computed(() => countByType(props.dependencies))
// Start on the first kind that has something to show.
watch(() => [props.open, props.dependencies] as const, ([open]) => {
  if (!open) return
  force.value = false
  tab.value = DEPENDENCY_TYPES.find(type => counts.value[type] > 0) || 'index'
}, { immediate: true })
const quoted = computed(() => props.names.map(name => `「${name}」`).join('、'))
const kindLabel = (dependency: ColumnDependency) => {
  const key = `source.drop.kind.${dependency.kind.replace(/\s+/g, '_')}`
  return te(key) ? t(key) : dependency.kind
}
const headers = computed(() => {
  const name = { title: t(`source.drop.name.${tab.value}`), dataIndex: 'name' }
  const kind = { title: t(`source.drop.kindHeader.${tab.value}`), dataIndex: 'kindLabel' }
  if (tab.value === 'index') return [name, kind, { title: t('source.drop.columns'), dataIndex: 'columns' }]
  if (tab.value === 'foreign_key') return [name, kind, { title: t('source.drop.table'), dataIndex: 'owner' }, { title: t('source.drop.references'), dataIndex: 'detail' }]
  if (tab.value === 'other') return [name, kind, { title: t('source.drop.expression'), dataIndex: 'detail' }]
  return [name, kind]
})
const current = computed(() => props.dependencies.filter(dependency => dependency.type === tab.value).map((dependency, index) => ({
  ...dependency, key: `${dependency.schema}.${dependency.table}.${dependency.name}.${index}`, kindLabel: kindLabel(dependency), owner: `${dependency.schema}.${dependency.table}`,
})))
</script>
