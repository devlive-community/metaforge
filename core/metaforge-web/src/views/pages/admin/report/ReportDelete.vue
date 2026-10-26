<template>
  <MfModal v-model:open="visible" :title="title">
    <MfSpace direction="vertical" fill :style="{ width: '100%' }">
      <MfAlert variant="danger" :title="$t('report.tip.deleteAlert1')"/>
      <MfAlert variant="danger" :title="$t('report.tip.deleteAlert2')"/>
      <MfAlert variant="info" :title="$t('report.tip.deleteAlert3').replace('$VALUE', String(info?.name))"/>
    </MfSpace>

    <MfForm :model="formState" class="mt-3" @finish="onSubmit">
      <MfFormItem name="name"
                  :rules="[
                    { required: true, message: $t('report.validator.name.required') },
                    { validator: validateMatch }
                  ]">
        <MfInput v-model="formState.name" :placeholder="$t('report.placeholder.name')"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfSpace>
          <MfButton @click="onCancel">
            {{ $t('common.cancel') }}
          </MfButton>
          <MfButton variant="danger" type="submit" :loading="loading" :disabled="loading">
            {{ title }}
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
import ReportService from '@/services/report'
import { ReportModel } from '@/model/report'

defineOptions({ name: 'ReportDelete' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: ReportModel | null }>(), {
  isVisible: false,
  info: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const title = ref<string | null>(null)
const loading = ref(false)
const formState = ref<{ name: string }>({ name: '' })

if (props.info) {
  title.value = `${ t('report.common.deleteInfo').replace('$VALUE', String(props.info.name)) }`
}

const validateMatch = (value: string) => {
  if (value !== String(props.info?.name)) {
    return t('report.validator.name.match').replace('$VALUE', String(props.info?.name))
  }
  return true
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  if (props.info) {
    loading.value = true
    ReportService.deleteByCode(props.info.code!)
                 .then((response) => {
                   if (response.status) {
                     MfMessage.success(t('report.tip.deleteSuccess').replace('$VALUE', String(props.info?.name)))
                     onCancel()
                   }
                   else {
                     MfMessage.error(response.message)
                   }
                 })
                 .finally(() => (loading.value = false))
  }
}
</script>
