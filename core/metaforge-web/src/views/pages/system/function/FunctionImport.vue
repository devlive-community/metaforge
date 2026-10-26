<template>
  <MfDrawer v-model:open="visible" :title="$t('function.common.import')" width="40%">
    <MfForm v-if="formState" :model="formState" layout="vertical">
      <MfTabs v-model:active-key="formState.mode">
        <MfTabPane :item-key="FunctionMode.txt" :tab="$t('common.content')">
          <MfFormItem name="content" :label="$t('common.content')">
            <MfTextarea v-model="formState.content" :placeholder="$t('common.content')"/>
          </MfFormItem>
        </MfTabPane>

        <MfTabPane :item-key="FunctionMode.url" :tab="$t('function.common.importFromUrl')">
          <MfSpace direction="vertical" fill :style="{ width: '100%' }">
            <MfAlert variant="warning"
                     :title="$t('function.common.keyword')">
              (http|https)://metaforge.devlive.org/resources/functions/plugin/keywords.txt
            </MfAlert>
            <MfAlert variant="warning"
                     :title="$t('function.common.operator')">
              (http|https)://metaforge.devlive.org/resources/functions/plugin/operators.txt
            </MfAlert>
            <MfAlert variant="warning"
                     :title="$t('function.common.function')">
              (http|https)://metaforge.devlive.org/resources/functions/plugin/functions.txt
            </MfAlert>
          </MfSpace>

          <MfFormItem name="url" :label="$t('common.url')" class="mt-2">
            <MfInput v-model="formState.content"/>
          </MfFormItem>
        </MfTabPane>
      </MfTabs>

      <MfFormItem name="plugin"
                  :label="$t('common.plugin')"
                  :rules="[{ required: true, message: $t('common.plugin') }]">
        <MfSelect v-model="formState.plugin" multiple :options="plugins"/>
      </MfFormItem>

      <MfFormItem name="type"
                  :label="$t('common.type')"
                  :rules="[{ required: true, message: $t('common.type') }]">
        <MfSelect v-model="formState.type"
                  :placeholder="$t('function.tip.selectTypeHolder')"
                  :options="types"/>
      </MfFormItem>
    </MfForm>

    <div class="flex justify-end mt-2">
      <MfSpace>
        <MfButton variant="danger" :loading="loading" :disabled="loading" @click="onCancel()">
          {{ $t('common.cancel') }}
        </MfButton>
        <MfButton variant="primary" :loading="loading" :disabled="loading" @click="onSubmit()">
          {{ $t('function.common.import') }}
        </MfButton>
      </MfSpace>
    </div>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { MfMessage } from 'metaforge-ui'
import { useHeaders } from '@/views/pages/system/function/FunctionUtils'
import { FunctionImportModel, FunctionMode } from '@/model/function'
import FunctionService from '@/services/function'
import SourceService from '@/services/source'

defineOptions({ name: 'FunctionImport' })

const props = withDefaults(defineProps<{ isVisible?: boolean }>(), {
  isVisible: false
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { typeHeaders: types } = useHeaders()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const formState = ref<FunctionImportModel>(null as unknown as FunctionImportModel)
const loading = ref(false)
const plugins = ref<any[]>([])

const handlerInitialize = () => {
  formState.value = {
    mode: FunctionMode.txt,
    plugin: undefined,
    content: undefined,
    type: undefined
  } as unknown as FunctionImportModel

  SourceService.getPlugins()
               .then(response => {
                 if (response.status) {
                   plugins.value = Array.from(
                       new Set(
                           (Object.values(response.data)
                                  .reduce((acc: any, curr) => acc.concat(curr), []) as any[])
                               .map((value: { name: string }) => ({ label: value.name, value: value.name }))
                       )
                   )
                 }
               })
}

const onSubmit = () => {
  loading.value = true
  FunctionService.import(formState.value)
                 .then((response) => {
                   if (response.status) {
                     MfMessage.success('Create successfully')
                     onCancel()
                   }
                   else {
                     MfMessage.error(response.message)
                   }
                 })
                 .finally(() => (loading.value = false))
}

const onCancel = () => {
  emit('close', false)
}

handlerInitialize()
</script>
