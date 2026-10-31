<template>
  <MfModal v-model:open="visible" width="min(880px, 94vw)" body-class="px-5 py-4">
    <template #title>
      <span class="flex items-center gap-2.5">
        <span class="inline-flex size-9 shrink-0 items-center justify-center rounded-mf-lg bg-primary/10 text-primary">
          <Database :size="18"/>
        </span>
        <span class="flex min-w-0 flex-col">
          <span class="text-base font-semibold leading-6">{{ title }}</span>
          <span class="text-xs font-normal text-content-tertiary">{{ $t('function.info.subtitle') }}</span>
        </span>
      </span>
    </template>

    <div v-if="loading" class="flex items-center justify-center py-8">
      <MfSpinner size="lg"/>
    </div>
    <MfForm v-else-if="formState" id="function-info-form" :model="formState" layout="vertical" class="space-y-0" @finish="onSubmit">
      <div class="mb-3 flex items-start gap-3">
        <span class="inline-flex size-8 shrink-0 items-center justify-center rounded-mf-full bg-primary text-sm font-semibold text-on-primary">1</span>
        <div class="min-w-0">
          <div class="text-sm font-semibold text-content">{{ $t('function.info.basicInfo') }}</div>
          <div class="mt-0.5 text-xs text-content-tertiary">{{ $t('function.info.basicInfoDesc') }}</div>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-x-3">
        <MfFormItem name="name"
                    :label="$t('function.field.name')"
                    :rules="[{ required: true, message: $t('function.validator.name.required') }]">
          <MfInput v-model="formState.name" :placeholder="'COUNT'"/>
        </MfFormItem>
        <MfFormItem name="category" :label="$t('function.field.category')">
          <MfInput v-model="formState.category" :placeholder="$t('function.placeholder.category')"/>
        </MfFormItem>
      </div>

      <div class="grid grid-cols-2 gap-x-3">
        <MfFormItem name="type"
                    :label="$t('function.import.dataType')"
                    :rules="[{ required: true, message: $t('function.tip.selectTypeHolder') }]">
          <MfSelect v-model="formState.type"
                    :placeholder="$t('function.tip.selectTypeHolder')"
                    :options="types"/>
        </MfFormItem>
        <MfFormItem name="plugin"
                    :label="$t('common.plugin')"
                    :rules="[{ required: true, message: $t('common.plugin') }]">
          <MfSelect v-model="formState.plugin"
                    multiple
                    :placeholder="$t('function.tip.selectPluginHolder')"
                    :options="plugins"/>
        </MfFormItem>
      </div>

      <div class="mb-3 mt-5 flex items-start gap-3">
        <span class="inline-flex size-8 shrink-0 items-center justify-center rounded-mf-full bg-primary text-sm font-semibold text-on-primary">2</span>
        <div class="min-w-0">
          <div class="text-sm font-semibold text-content">{{ $t('function.info.knowledge') }}</div>
          <div class="mt-0.5 text-xs text-content-tertiary">{{ $t('function.info.knowledgeDesc') }}</div>
        </div>
      </div>

      <MfFormItem name="syntax"
                  :label="$t('function.field.syntax')"
                  :rules="[{ required: true, message: $t('function.field.syntax') }]">
        <AceEditor :value="formState.syntax" @update:value="formState.syntax = $event"/>
      </MfFormItem>

      <MfFormItem name="description" :label="$t('function.field.description')">
        <MfTextarea v-model="formState.description" :rows="2"/>
      </MfFormItem>

      <MfFormItem name="examples" :label="$t('function.field.examples')">
        <AceEditor :value="formState.examples" :height="'110px'" @update:value="formState.examples = $event"/>
      </MfFormItem>

      <div class="grid grid-cols-2 gap-x-3">
        <MfFormItem name="parameters" :label="$t('function.field.parameters')">
          <MfTextarea v-model="formState.parameters" :rows="2" :placeholder="$t('common.optional')"/>
        </MfFormItem>
        <MfFormItem name="returnType" :label="$t('function.field.return_type')">
          <MfInput v-model="formState.returnType" :placeholder="$t('common.optional')"/>
        </MfFormItem>
      </div>

      <div class="grid grid-cols-2 gap-x-3">
        <MfFormItem name="notes" :label="$t('function.field.notes')">
          <MfTextarea v-model="formState.notes" :rows="2" :placeholder="$t('common.optional')"/>
        </MfFormItem>
        <MfFormItem name="version" :label="$t('function.field.version')">
          <MfInput v-model="formState.version" :placeholder="$t('common.optional')"/>
        </MfFormItem>
      </div>
    </MfForm>

    <template #footer>
      <MfButton @click="visible = false">
        {{ $t('common.cancel') }}
      </MfButton>
      <MfButton variant="primary" type="submit" form="function-info-form" :loading="saving" :disabled="saving">
        {{ $t('common.save') }}
      </MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { Database } from '@lucide/vue'
import { cloneDeep, omit } from 'lodash'
import { useHeaders } from '@/views/pages/system/function/FunctionUtils'
import { FunctionModel, FunctionType } from '@/model/function'
import FunctionService from '@/services/function'
import PluginService from '@/services/plugin'
import AceEditor from '@/views/components/editor/AceEditor.vue'

defineOptions({ name: 'FunctionInfo' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: FunctionModel | null }>(), {
  isVisible: false,
  info: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()
const { typeHeaders: types } = useHeaders()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const formState = ref<FunctionModel>(null as unknown as FunctionModel)
const loading = ref(false)
const saving = ref(false)
const title = ref<string | null>(null)
const plugins = ref<any[]>([])

const handlerInitialize = () => {
  title.value = `${ t('function.common.create') }`
  if (props.info) {
    formState.value = cloneDeep(omit(props.info, ['createTime', 'updateTime', 'content', 'example'])) as FunctionModel
    title.value = `${ t('function.common.modify').replace('$NAME', props.info.name as string) }`
  }
  else {
    formState.value = {
      name: undefined,
      category: undefined,
      type: FunctionType.FUNCTION,
      plugin: undefined,
      syntax: undefined,
      description: undefined,
      examples: undefined,
      parameters: undefined,
      returnType: undefined,
      notes: undefined,
      version: undefined
    } as unknown as FunctionModel
  }

  loading.value = true
  PluginService.getPlugins()
               .then(response => {
                 if (response.status) {
                   plugins.value = response.data
                                           .filter((value: { type: string }) => value.type === 'CONNECTOR')
                                           .map((value: { name: string }) => ({ label: value.name, value: value.name }))
                 }
               })
               .finally(() => (loading.value = false))
}

const onSubmit = () => {
  saving.value = true
  FunctionService.saveOrUpdate(formState.value)
                 .then((response) => {
                   if (response.status) {
                     visible.value = false
                     MfMessage.success(t('function.tip.saveSuccess').replace('$VALUE', String(formState.value.name ?? '')))
                   }
                   else {
                     MfMessage.error(response.message)
                   }
                 })
                 .finally(() => (saving.value = false))
}

handlerInitialize()
</script>
