<template>
  <MfModal v-model:open="localVisible" :title="$t('dataset.adhoc.saveReport')" :close-disabled="published" :close-on-overlay="!published" @close="onCancel">
    <MfForm :model="formState" :disabled="published" layout="vertical" @finish="onSubmit">
      <MfAlert v-if="failure" variant="danger" :title="failure"/>
      <MfFormItem name="name"
                  :label="$t('common.name')"
                  :rules="[{ required: true, validator: value => !!String(value || '').trim(), message: $t('report.validator.name.required') }]">
        <MfInput v-model="formState.name"/>
      </MfFormItem>

      <MfFormItem name="description" :label="$t('common.description')">
        <MfTextarea v-model="formState.description"/>
      </MfFormItem>

      <MfFormItem name="build" :label="$t('dataset.adhoc.viewReportsAfterSave')">
        <MfSwitch v-model="formState.build"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfSpace>
          <MfButton :disabled="published" @click="onCancel">
            {{ $t('common.cancel') }}
          </MfButton>
          <MfButton variant="primary" type="submit" :disabled="published" :loading="published">
            {{ $t('common.save') }}
          </MfButton>
        </MfSpace>
      </div>
    </MfForm>
  </MfModal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import router from '@/router'
import ReportService from '@/services/report.ts'

defineOptions({ name: 'DatasetReport' })

const props = defineProps<{
  visible: boolean
  info?: { name: string; description: string; build?: boolean }
  code?: string
  datasetCode: string
  commitOptions?: any
  configure?: any
}>()

const emit = defineEmits<{ close: [value: boolean]; saved: [report: { code: string; name: string; description: string }] }>()

const { t } = useI18n()

const localVisible = ref(props.visible)
const published = ref(false)
const failure = ref('')
const formState = ref<{ name?: string; description?: string; build: boolean }>(
  { name: props.info?.name, description: props.info?.description, build: props.info?.build || false }
)

const onCancel = () => {
  if (!published.value) emit('close', false)
}

const onSubmit = () => {
  if (published.value) return
  published.value = true
  failure.value = ''
  const configure = {
    name: formState.value.name,
    code: props.code,
    realtime: true,
    type: 'DATASET',
    configure: JSON.stringify(props.commitOptions),
    dataset: {
      code: props.datasetCode
    },
    query: JSON.stringify(props.configure),
    description: formState.value.description
  }
  ReportService.saveOrUpdate(configure)
               .then(response => {
                 if (response.status) {
                   MfMessage.success(t('report.tip.publishSuccess').replace('$VALUE', String(formState.value.name)))
                   emit('saved', { code: response.data?.code || props.code || '', name: formState.value.name || '', description: formState.value.description || '' })
                   if (formState.value.build) {
                     router.push('/report')
                   }
                   emit('close', false)
                 }
                 else failure.value = response.message
               })
               .catch(error => { failure.value = error instanceof Error ? error.message : String(error) })
               .finally(() => (published.value = false))
}
</script>
