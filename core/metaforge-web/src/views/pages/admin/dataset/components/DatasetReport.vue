<template>
  <MfModal v-model:open="localVisible" :title="$t('common.configure')" @close="onCancel">
    <MfForm :model="formState" layout="vertical" @finish="onSubmit" @finish-failed="(e: any) => console.log(e)">
      <MfFormItem name="name"
                  :label="$t('common.name')"
                  :rules="[{ required: true, message: $t('common.name') }]">
        <MfInput v-model="formState.name"/>
      </MfFormItem>

      <MfFormItem name="description" :label="$t('common.description')">
        <MfTextarea v-model="formState.description"/>
      </MfFormItem>

      <MfFormItem name="build" :label="$t('dataset.common.continuousBuild')">
        <MfSwitch v-model="formState.build"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfSpace>
          <MfButton @click="onCancel">
            {{ $t('common.cancel') }}
          </MfButton>
          <MfButton variant="primary" type="submit" :disabled="published" :loading="published">
            {{ $t('common.publish') }}
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
  info?: { name: string; description: string; build: boolean }
  code?: string
  dimension?: any
  commitOptions?: any
  configure?: any
}>()

const emit = defineEmits(['close'])

const { t } = useI18n()

const localVisible = ref(props.visible)
const published = ref(false)
const formState = ref<{ name?: string; description?: string; build: boolean }>(
  props.info || { name: undefined, description: undefined, build: false }
)

const onCancel = () => {
  emit('close', !props.visible)
}

const onSubmit = () => {
  published.value = true
  const obj = props.dimension
  const configure = {
    name: formState.value.name,
    code: props.code,
    realtime: true,
    type: 'DATASET',
    configure: JSON.stringify(props.commitOptions),
    dataset: {
      code: obj.dataset.code
    },
    query: JSON.stringify(props.configure),
    description: formState.value.description
  }
  ReportService.saveOrUpdate(configure)
               .then(response => {
                 if (response.status) {
                   MfMessage.success(t('report.tip.publishSuccess').replace('$VALUE', String(formState.value.name)))
                   if (formState.value.build) {
                     router.push('/report')
                   }
                   onCancel()
                 }
               })
               .finally(() => (published.value = false))
}
</script>
