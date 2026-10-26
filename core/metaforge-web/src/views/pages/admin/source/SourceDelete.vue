<template>
  <MfModal v-model:open="visible" :title="title">
    <MfSpace direction="vertical" fill>
      <MfAlert variant="danger" :title="$t('source.tip.deleteAlert1')"/>
      <MfAlert variant="danger" :title="$t('source.tip.deleteAlert2')"/>
      <MfAlert :title="$t('source.tip.deleteAlert3').replace('$NAME', String(info?.name))"/>
    </MfSpace>

    <MfForm :model="formState" class="mt-3" @finish="onSubmit">
      <MfFormItem name="name"
                  :rules="[
                    { required: true, message: $t('source.validator.name.required') },
                    { validator: validateMatch }
                  ]">
        <MfInput v-model="formState.name" :placeholder="$t('source.placeholder.name')"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfSpace>
          <MfButton @click="onCancel">{{ $t('common.cancel') }}</MfButton>
          <MfButton variant="danger" type="submit" :loading="loading">{{ title }}</MfButton>
        </MfSpace>
      </div>
    </MfForm>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { SourceModel } from '@/model/source'
import SourceService from '@/services/source'

defineOptions({ name: 'SourceDelete' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: SourceModel | null }>(), {
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
  title.value = `${ t('source.common.delete').replace('$NAME', String(props.info.name)) }`
}

const validateMatch = (value: string) => {
  if (value !== String(props.info?.name)) {
    return t('source.validator.name.match').replace('$VALUE', String(props.info?.name))
  }
  return true
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  if (props.info) {
    loading.value = true
    SourceService.deleteByCode(props.info.code!)
                 .then((response) => {
                   if (response.status) {
                     MfMessage.success(t('source.tip.deleteSourceSuccess').replace('$NAME', String(props.info?.name)))
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
