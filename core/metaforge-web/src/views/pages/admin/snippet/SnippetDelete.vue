<template>
  <MfModal v-model:open="visible" :title="title">
    <MfSpace direction="vertical" fill :style="{ width: '100%' }">
      <MfAlert variant="danger" :title="$t('snippet.tip.deleteAlert1')"/>
      <MfAlert variant="danger" :title="$t('snippet.tip.deleteAlert2')"/>
      <MfAlert variant="danger" :title="$t('snippet.tip.deleteAlert3')"/>
      <MfAlert :title="$t('snippet.tip.deleteAlert4').replace('$VALUE', info?.name as string)"/>
    </MfSpace>

    <MfForm :model="formState" class="mt-3" @finish="onSubmit" @finish-failed="(e: any) => console.log(e)">
      <MfFormItem name="name"
                  :rules="[
                    { required: true, message: $t('snippet.validator.name.required') },
                    { validator: validateMatch }
                  ]">
        <MfInput v-model="formState.name" :placeholder="$t('snippet.placeholder.name')"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfSpace>
          <MfButton @click="onCancel">
            {{ $t('common.cancel') }}
          </MfButton>
          <MfButton variant="danger" type="submit" :loading="loading">
            {{ $t('snippet.common.delete') }}
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
import { SnippetModel } from '@/model/snippet'
import SnippetService from '@/services/snippet'

defineOptions({ name: 'SnippetDelete' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: SnippetModel | null }>(), {
  isVisible: false,
  info: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const title = ref<string | null>(null)
const formState = ref<{ name: string }>({ name: '' })

if (props.info) {
  title.value = t('snippet.common.deleteInfo').replace('$VALUE', props.info.name as string)
}

const validateMatch = (value: string) => {
  if (value !== String(props.info?.name)) {
    return t('snippet.validator.name.match').replace('$VALUE', String(props.info?.name))
  }
  return true
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  if (props.info) {
    loading.value = true
    SnippetService.deleteByCode(props.info.code!)
                  .then(response => {
                    if (response.status) {
                      MfMessage.success(t('snippet.tip.deleteSuccess').replace('$VALUE', props.info?.name as string))
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
