<template>
  <MfDrawer :open="open" inline :container="container || undefined" :width="width + 'px'"
            :title="$t(mode === 'edit' ? 'source.structure.editField' : 'source.structure.addField')"
            :closable="!saving" @update:open="value => !value && emit('close')">
    <MfForm :id="formId" :model="form" :disabled="saving" @finish="emit('save')">
      <p v-if="mode === 'insert' && anchor" class="m-0 text-xs text-content-secondary">{{ $t('source.structure.insertAfter').replace('$VALUE', anchor) }}</p>
      <MfFormItem name="name" :label="$t('source.structure.fieldName')" :rules="[
        { required: true, message: $t('source.validator.columnName.required') },
        { max: 64, message: $t('source.prototype.invalidName') },
        { validator: value => !!String(value || '').trim() && !/[`\r\n\u0000]/.test(String(value)), message: $t('source.prototype.invalidName') },
        { validator: value => mode === 'edit' || !columns.some(column => column.name === String(value).trim()), message: $t('source.prototype.duplicateName') }
      ]">
        <div class="w-full"><MfInput v-model="form.name" size="sm" :disabled="saving || mode === 'edit'" :invalid="!!issues.name"/>
        <p v-if="issues.name" role="alert" class="mt-1 text-xs text-danger">{{ issues.name }}</p></div>
      </MfFormItem>
      <MfFormItem name="type" :label="$t('source.prototype.fieldType')" :rules="[{ required: true, message: $t('source.validator.columnType.required') }, { validator: value => supportedTypes.includes(String(value)), message: $t('source.prototype.unsupportedType') }]">
        <MfSelect v-model="form.type" size="sm" :options="typeOptions" :loading="typesLoading" :disabled="saving || indexOnly"/>
      </MfFormItem>
      <MfFormItem name="length" :label="$t('source.structure.lengthPrecision')" :rules="[{ validator: value => validLength(value), message: $t('source.prototype.invalidLength') }]">
        <MfInput v-model="form.length" size="sm" :disabled="saving || indexOnly"/>
      </MfFormItem>
      <div class="grid grid-cols-2 gap-3">
        <MfCheckbox v-model="form.nullable" :disabled="saving || indexOnly || form.autoIncrement">{{ $t('source.structure.nullable') }}</MfCheckbox>
        <MfCheckbox v-model="form.autoIncrement" :disabled="saving || indexOnly">{{ $t('source.prototype.autoIncrement') }}</MfCheckbox>
      </div>
      <MfFormItem name="defaultValue" :label="$t('source.common.defaultValue')">
        <div class="grid w-full grid-cols-2 gap-3">
          <MfSelect v-model="form.defaultMode" size="sm" class="min-w-0" :options="defaultModeOptions" :disabled="saving || indexOnly || form.autoIncrement"/>
          <MfInput v-model="form.defaultValue" size="sm" class="min-w-0" :disabled="saving || indexOnly || form.defaultMode !== 'constant' || form.autoIncrement"/>
        </div>
      </MfFormItem>
      <MfFormItem name="comment" :label="$t('source.structure.comment')" :rules="[{ max: 500 }]">
        <div class="relative w-full">
          <MfTextarea v-model="form.comment" :rows="3" :maxlength="500" class="pb-6!" :disabled="saving || indexOnly"/>
          <span class="pointer-events-none absolute bottom-2 right-3 text-[11px] text-content-tertiary">{{ $t('source.structure.commentCounter', { count: form.comment.length }) }}</span>
        </div>
      </MfFormItem>
      <div class="border-t border-line pt-3">
        <button type="button" class="flex w-full items-center justify-between text-sm font-medium text-info" :aria-expanded="more" @click="emit('update:more', !more)">
          <span class="inline-flex items-center gap-1.5"><ListFilter :size="14"/>{{ $t('source.structure.moreOptions') }}</span>
          <ChevronDown :size="13" :class="more && 'rotate-180'"/>
        </button>
        <div v-show="more" class="mt-4 flex flex-col gap-3">
          <MfFormItem name="charset" :label="$t('source.structure.charset')"><MfSelect v-model="form.charset" size="sm" :options="charsetOptions" clearable :disabled="saving || indexOnly"/></MfFormItem>
          <MfFormItem name="collate" :label="$t('source.common.collation')"><MfSelect v-model="form.collate" size="sm" :options="collateOptions" clearable :disabled="saving || indexOnly"/></MfFormItem>
          <MfCheckbox v-if="canCreateIndex" v-model="form.createIndex" :disabled="saving">{{ $t('source.prototype.addIndex') }}</MfCheckbox>
          <div v-if="canCreateIndex && form.createIndex" class="flex flex-col gap-3 pl-6">
            <div class="grid grid-cols-2 gap-3">
              <MfFormItem name="indexType" :label="$t('source.index.indexType')"><MfSelect v-model="form.indexType" size="sm" :options="indexTypeOptions" :disabled="saving"/></MfFormItem>
              <MfFormItem name="indexName" :label="$t('source.index.indexName')" :rules="[{ required: true, message: $t('source.prototype.indexNameRequired') }, { max: 64, message: $t('source.prototype.invalidName') }, { validator: value => !/[`\r\n\u0000]/.test(String(value)), message: $t('source.prototype.invalidName') }]">
                <div class="w-full"><MfInput v-model="form.indexName" size="sm" :disabled="saving" :invalid="!!issues.indexName"/>
                <p v-if="issues.indexName" role="alert" class="mt-1 break-words text-xs text-danger">{{ issues.indexName }}</p></div>
              </MfFormItem>
            </div>
            <MfCheckbox :model-value="form.indexType === 'unique'" :disabled="saving" @update:model-value="value => form.indexType = value ? 'unique' : 'normal'">{{ $t('source.prototype.uniqueIndex') }}</MfCheckbox>
            <MfCheckbox v-model="form.compositeIndex" :disabled="saving">{{ $t('source.prototype.compositeIndex') }}</MfCheckbox>
            <MfFormItem v-if="form.compositeIndex" name="compositeColumns" :label="$t('source.prototype.indexColumns')" :rules="[{ required: true, type: 'array', message: $t('source.prototype.indexColumnsRequired') }]">
              <MfSelect v-model="form.compositeColumns" size="sm" multiple :options="compositeColumnOptions" :disabled="saving"/>
            </MfFormItem>
          </div>
        </div>
      </div>
    </MfForm>
    <template #footer>
      <MfButton size="sm" :disabled="saving" @click="emit('close')">{{ $t('common.cancel') }}</MfButton>
      <MfButton size="sm" variant="primary" type="submit" :form="formId" :loading="saving">{{ $t('source.prototype.confirm') }}</MfButton>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { getCurrentInstance, watch } from 'vue'
import { ChevronDown, ListFilter } from '@lucide/vue'
import type { SourceColumn, SourceColumnForm } from '@/model/source-column'
const form = defineModel<SourceColumnForm>({ required: true })
const props = defineProps<{
  open: boolean; width: number; canCreateIndex: boolean; mode: 'add' | 'edit' | 'insert'; anchor: string | null; more: boolean; saving: boolean;
  indexOnly: boolean; container: HTMLElement | null; issues: Record<string, string>; columns: SourceColumn[];
  supportedTypes: string[]; typesLoading: boolean;
  typeOptions: { label: string; value: string }[]; defaultModeOptions: { label: string; value: string }[];
  charsetOptions: { label: string; value: string }[]; collateOptions: { label: string; value: string }[];
  indexTypeOptions: { label: string; value: string }[]; compositeColumnOptions: { label: string; value: string }[];
}>()
const emit = defineEmits<{ close: []; save: []; 'update:more': [value: boolean] }>()
const formId = 'source-column-form-' + getCurrentInstance()!.uid
function validLength(value: unknown): boolean {
  const text = String(value ?? '').trim()
  if (!text) return !['VARCHAR', 'CHAR'].includes(form.value.type)
  if (!/^\d+(?:,\d+)?$/.test(text)) return false
  const [length, scale] = text.split(',').map(Number)
  return length > 0 && (scale === undefined || (form.value.type === 'DECIMAL' && scale <= length))
}
watch(() => form.value.autoIncrement, value => {
  if (value && props.open) { form.value.nullable = false; form.value.defaultMode = 'none'; form.value.defaultValue = '' }
})
</script>
