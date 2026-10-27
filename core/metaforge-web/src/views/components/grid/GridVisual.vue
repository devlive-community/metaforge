<template>
  <MfModal v-model:open="visible"
           width="80%"
           :title="$t('dataset.common.visual')"
           @close="onCancel">
    <MfAlert v-if="message" variant="danger" :title="message" class="mb-3"/>

    <MfForm :model="formState" layout="vertical" @finish="onSubmit">
      <MfFormItem name="name"
                  :label="$t('common.name')"
                  :rules="[
                        { required: true, message: $t('report.validator.name.required') }
                  ]">
        <MfInput v-model="formState.name" :placeholder="$t('report.placeholder.name')"/>
      </MfFormItem>

      <MfFormItem name="description" :label="$t('common.description')">
        <MfTextarea v-model="formState.description" :placeholder="$t('report.placeholder.description')"/>
      </MfFormItem>

      <MfFormItem>
        <VisualEditor :loading="loading" :configuration="configuration as any" @commitOptions="onCommitOptions"/>
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
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import VisualEditor from '@/views/components/visual/VisualEditor.vue'
import { Configuration } from '@/views/components/visual/Configuration.ts'
import { GridConfigure } from '@/views/components/grid/GridConfigure.ts'
import router from '@/router'
import ReportService from '@/services/report'

defineOptions({ name: 'GridVisual' })

const props = withDefaults(defineProps<{ isVisible?: boolean; configure?: GridConfigure }>(), {
  isVisible: false,
  configure: () => null as unknown as GridConfigure
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const configuration = ref<Configuration | null>(null)
const published = ref(false)
const message = ref<string | null>(null)
const formState = ref({
  name: '',
  realtime: true,
  type: 'QUERY',
  configure: null as string | null,
  query: props.configure?.query,
  description: '',
  source: {
    code: props.configure.code
  }
})

if (props.configure) {
  configuration.value = new Configuration()
  configuration.value.headers = props.configure?.headers as never[]
  configuration.value.columns = props.configure?.columns as never[]
}

const onCommitOptions = (value: any) => {
  formState.value.configure = JSON.stringify(value)
}

const validator = () => {
  if (!formState.value.name) {
    message.value = t('report.validator.name').toString()
  }
  else {
    message.value = null
  }
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  validator()
  if (!message.value) {
    published.value = true
    ReportService.saveOrUpdate(formState.value)
                 .then((response) => {
                   if (response.status) {
                     MfMessage.success(t('report.tip.publishSuccess').replace('$VALUE', formState.value.name))
                     router.push('/report')
                   }
                   else {
                     MfMessage.error(response.message)
                   }
                 })
                 .finally(() => (published.value = false))
  }
}
</script>
