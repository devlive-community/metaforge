<template>
  <MfDrawer :open="open" inline :container="container || undefined" :width="width + 'px'" :title="title"
            :closable="!saving" @update:open="value => !value && emit('close')">
    <MfTabs :active-key="tab" class="row-editor-tabs" @update:active-key="key => emit('tab', key as 'form' | 'json')">
      <MfTabPane item-key="form" :tab="$t('source.data.formTab')" :disabled="formDisabled">
        <form class="flex flex-col gap-4 pt-1" @submit.prevent="emit('save')">
          <div v-for="field in fields" :key="field.name" class="flex flex-col gap-1.5">
            <label :for="fieldId(field)" class="flex min-w-0 items-center gap-1 text-sm text-content">
              <span class="truncate" :title="field.name">{{ field.name }}</span>
              <span v-if="field.required" class="text-danger" aria-hidden="true">*</span>
            </label>
            <MfSelect v-if="field.kind === 'enum'" :id="fieldId(field)" :model-value="values[field.name] ?? ''" size="sm" :disabled="saving" :invalid="!!issues[field.name]"
                      :clearable="field.nullable" :placeholder="placeholder(field)" :options="field.options.map(value => ({ label: value, value }))"
                      @update:model-value="value => setValue(field, value === '' ? null : String(value))"/>
            <MfSelect v-else-if="field.kind === 'set'" :id="fieldId(field)" :model-value="splitSet(values[field.name])" size="sm" multiple :disabled="saving"
                      :invalid="!!issues[field.name]" :placeholder="placeholder(field)" :options="field.options.map(value => ({ label: value, value }))"
                      @update:model-value="value => setValue(field, (value as string[]).length ? (value as string[]).join(',') : null)"/>
            <MfRadioGroup v-else-if="field.kind === 'boolean'" :model-value="values[field.name] ?? ''" :disabled="saving" :name="fieldId(field)"
                          @update:model-value="value => setValue(field, String(value))">
              <MfRadio value="0" label="0"/>
              <MfRadio value="1" label="1"/>
            </MfRadioGroup>
            <MfTextarea v-else-if="field.kind === 'json' || field.kind === 'longtext'" :id="fieldId(field)" :model-value="values[field.name] ?? ''" :rows="field.kind === 'json' ? 5 : 3"
                        :class="field.kind === 'json' && 'font-mono text-xs'" :disabled="saving" :placeholder="placeholder(field)"
                        @update:model-value="value => setValue(field, String(value))"/>
            <MfDatePicker v-else-if="field.kind === 'date'" :model-value="values[field.name] ?? ''" :locale="locale" :label="field.name" :placeholder="placeholder(field)"
                          :disabled="saving" @update:model-value="value => setValue(field, value || null)"/>
            <MfInput v-else :id="fieldId(field)" :model-value="values[field.name] ?? ''" size="sm" :disabled="saving" :invalid="!!issues[field.name]"
                     :inputmode="field.kind === 'number' ? 'decimal' : undefined" :placeholder="placeholder(field)"
                     @update:model-value="value => setValue(field, String(value))">
              <template v-if="field.kind === 'datetime' || field.kind === 'time'" #prefix><CalendarClock :size="14"/></template>
            </MfInput>
            <p v-if="issues[field.name]" role="alert" class="m-0 text-xs text-danger">{{ $t(`source.data.issue.${issues[field.name]}`) }}</p>
          </div>
        </form>
      </MfTabPane>
      <MfTabPane item-key="json" :tab="$t('source.data.jsonTab')">
        <div class="flex flex-col gap-1.5 pt-1">
          <MfTextarea v-model="json" :rows="22" class="font-mono text-xs" spellcheck="false" :disabled="saving" :aria-label="$t('source.data.jsonTab')"/>
          <p v-if="jsonError" role="alert" class="m-0 break-words text-xs text-danger">{{ jsonError }}</p>
          <p v-else class="m-0 text-xs text-content-tertiary">{{ $t(mode === 'edit' ? 'source.data.jsonEditHint' : 'source.data.jsonAddHint') }}</p>
        </div>
      </MfTabPane>
    </MfTabs>
    <template #footer>
      <MfCheckbox v-if="mode === 'add'" v-model="continueAdding" class="mr-auto self-center" :disabled="saving">{{ $t('source.data.continueAdding') }}</MfCheckbox>
      <MfButton size="sm" :disabled="saving" @click="emit('close')">{{ $t('common.cancel') }}</MfButton>
      <MfButton size="sm" variant="primary" :loading="saving" @click="emit('save')">{{ $t('source.prototype.confirm') }}</MfButton>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance } from 'vue'
import { useI18n } from 'vue-i18n'
import { CalendarClock } from '@lucide/vue'
import type { DataField, FieldIssue, FormValues } from '@/model/source-data'
import type { RowEditorMode } from '@/composables/useSourceTableData'
const values = defineModel<FormValues>({ required: true })
const json = defineModel<string>('json', { required: true })
const continueAdding = defineModel<boolean>('continueAdding', { default: false })
const props = defineProps<{
  open: boolean; mode: RowEditorMode; tab: 'form' | 'json'; fields: DataField[]; issues: Record<string, FieldIssue>; jsonError: string
  saving: boolean; width: number; container: HTMLElement | null; formDisabled?: boolean
}>()
const emit = defineEmits<{ close: []; save: []; tab: [tab: 'form' | 'json'] }>()
const { t, locale } = useI18n()
const uid = getCurrentInstance()!.uid
const title = computed(() => t({ add: 'source.data.addRow', edit: 'source.data.editRow', copy: 'source.data.copyRow' }[props.mode]))
const fieldId = (field: DataField) => `source-row-${uid}-${field.name}`
const splitSet = (value: string | null) => value ? value.split(',') : []
const placeholder = (field: DataField) => field.autoIncrement ? t('source.data.autoGenerated')
    : field.hasDefault ? t('source.data.columnDefault') : field.nullable ? 'NULL' : ''
function setValue(field: DataField, value: string | null) {
  values.value = { ...values.value, [field.name]: value }
}
</script>

<style scoped>
.row-editor-tabs :deep([role='tabpanel']) { padding-top: 12px; }
</style>
