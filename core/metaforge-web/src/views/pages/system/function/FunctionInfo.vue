<template>
  <MfDrawer v-model:open="visible" :title="title" width="40%">
    <div v-if="loading" class="flex items-center justify-center py-8">
      <MfSpinner size="lg"/>
    </div>
    <MfForm v-else-if="formState" id="function-info-form" :model="formState" layout="vertical" @finish="onSubmit">
      <MfFormItem name="name"
                  :label="$t('common.name')"
                  :rules="[{ required: true, message: $t('common.name') }]">
        <MfInput v-model="formState.name"/>
      </MfFormItem>

      <MfFormItem name="plugin"
                  :label="$t('common.plugin')"
                  :rules="[{ required: true, message: $t('common.plugin') }]">
        <MfSelect v-model="formState.plugin" multiple :options="plugins"/>
      </MfFormItem>

      <MfFormItem name="content"
                  :label="$t('common.content')"
                  :rules="[{ required: true, message: $t('common.content') }]">
        <MfTextarea v-model="formState.content"/>
      </MfFormItem>

      <MfFormItem name="description"
                  :label="$t('common.description')"
                  :rules="[{ required: true, message: $t('common.description') }]">
        <MfTextarea v-model="formState.description"/>
      </MfFormItem>

      <MfFormItem name="type"
                  :label="$t('common.type')"
                  :rules="[{ required: true, message: $t('common.type') }]">
        <MfSelect v-model="formState.type"
                  :placeholder="$t('function.tip.selectTypeHolder')"
                  :options="types"/>
      </MfFormItem>

    </MfForm>

    <template #footer>
      <MfButton @click="visible = false">
        {{ $t('common.cancel') }}
      </MfButton>
      <MfButton variant="primary" type="submit" form="function-info-form" :loading="saving" :disabled="saving">
        {{ $t('common.save') }}
      </MfButton>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { cloneDeep, omit } from 'lodash'
import { useHeaders } from '@/views/pages/system/function/FunctionUtils'
import { FunctionModel } from '@/model/function'
import FunctionService from '@/services/function'
import PluginService from '@/services/plugin'

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
    formState.value = cloneDeep(omit(props.info, ['createTime', 'updateTime'])) as FunctionModel
    title.value = `${ t('function.common.modify').replace('$NAME', props.info.name as string) }`
  }
  else {
    formState.value = {
      name: undefined,
      plugin: undefined,
      content: undefined,
      description: undefined,
      type: undefined,
      example: undefined
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
                     MfMessage.success('Save successfully')
                   }
                   else {
                     MfMessage.error(response.message)
                   }
                 })
                 .finally(() => (saving.value = false))
}

handlerInitialize()
</script>
