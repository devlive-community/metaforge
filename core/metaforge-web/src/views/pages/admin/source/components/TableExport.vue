<template>
  <MfModal v-model:open="visible" width="40%" :title="title">
    <div class="px-4 py-4">
      <MfSkeleton v-if="loading"/>

      <MfForm v-else-if="!loading && formState" :model="formState" layout="vertical" @finish="onSubmit">
        <MfFormItem name="format" :label="$t('source.common.exportDataFormat')">
          <MfRadioGroup v-model="formState.format" variant="button">
            <MfRadio v-for="item in formats" :key="item.name" :value="item.name">{{ item.name }}</MfRadio>
          </MfRadioGroup>
        </MfFormItem>

        <div class="grid grid-cols-2 gap-4">
          <MfFormItem name="count" :label="$t('source.common.exportDataCount')">
            <MfInputNumber v-model="formState.pagination.size" :style="{ width: '100%' }"/>
          </MfFormItem>
        </div>

        <MfFormItem name="path" :label="$t('source.common.downloadPath')">
          <div class="flex items-center space-x-1">
            <MfInput v-model="formState.path" disabled/>
            <MfButton :disabled="!formState.path" @click="onDownload()">
              {{ $t('source.common.downloadFile') }}
            </MfButton>
          </div>
        </MfFormItem>

        <MfSpace>
          <MfButton @click="onCancel">{{ $t('common.cancel') }}</MfButton>

          <MfButton variant="primary" type="submit" :loading="submitting" :disabled="submitting">
            {{ $t('source.common.generateData') }}
          </MfButton>
        </MfSpace>
      </MfForm>
    </div>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import PluginService from '@/services/plugin'
import MetadataService from '@/services/metadata'

defineOptions({ name: 'TableExport' })

const props = withDefaults(defineProps<{ isVisible?: boolean }>(), { isVisible: false })
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const route = useRoute()
const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const submitting = ref(false)
const title = ref<string | null>(null)
const formats = ref<any[]>([])
const formState = ref<any>({
  format: 'JsonConvert',
  pagination: {
    page: 1,
    size: 5000
  },
  path: null
})

const onSubmit = () => {
  const code = route.params?.source as string
  const database = route.params?.database as string
  const table = route.params?.table as string

  if (code && database && table) {
    submitting.value = true
    MetadataService.exportData(code, database, table, formState.value)
                   .then((response) => {
                     if (response.status) {
                       formState.value.path = response.data
                     }
                     else {
                       MfMessage.error(response.message)
                     }
                   })
                   .finally(() => (submitting.value = false))
  }
}

const onDownload = () => {
  if (formState.value) {
    window.open(formState.value.path, '_target')
  }
}

const onCancel = () => {
  visible.value = false
}

const table = route.params?.table as string
if (table) {
  title.value = t('source.common.exportDataTable').replace('$VALUE', table)

  loading.value = true
  PluginService.getPlugins()
               .then((response) => {
                 if (response.status && response.data) {
                   formats.value = response.data.filter((item: any) => item.type === 'CONVERT')
                 }
               })
               .finally(() => (loading.value = false))
}
</script>
