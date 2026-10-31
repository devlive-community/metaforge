<template>
  <MfModal v-model:open="visible" :title="title">
    <MfSpace direction="vertical" fill :style="{ width: '100%' }">
      <MfAlert variant="danger" :title="$t('menu.tip.deleteAlert1')"/>
      <MfAlert variant="danger" :title="$t('menu.tip.deleteAlert2')"/>
      <MfAlert variant="danger" :title="$t('menu.tip.deleteAlert3').replace('$VALUE', info?.name as string)"/>
    </MfSpace>

    <MfForm :model="formState" class="mt-3" @finish="onSubmit" @finish-failed="onError">
      <MfFormItem name="name"
                  :rules="[
                    { required: true, message: $t('menu.validator.name.required') },
                    { validator: validateMatch }
                ]">
        <MfInput v-model="formState.name" :placeholder="$t('menu.placeholder.name')"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfSpace>
          <MfButton @click="onCancel">
            {{ $t('common.cancel') }}
          </MfButton>
          <MfButton variant="danger" type="submit" :loading="loading">
            {{ $t('menu.common.delete') }}
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
import { MenuModel } from '@/model/menu'
import MenuService from '@/services/menu'

defineOptions({ name: 'MenuDelete' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: MenuModel | null }>(), {
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
const title = computed(() => t('menu.common.deleteInfo').replace('$VALUE', String(props.info?.name ?? '')))
const formState = ref<{ name: string }>({ name: '' })

const validateMatch = (value: string) => {
  if (value !== String(props.info?.name)) {
    return t('menu.validator.name.match')
  }
  return true
}

const onError = (error: any) => {
  const names = (error?.errorFields || []).map((field: any) => (Array.isArray(field.name) ? field.name.join('.') : field.name))
  MfMessage.error(`Validation error field: [ ${ names.join(', ') } ]`)
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  if (props.info) {
    loading.value = true
    MenuService.deleteByCode(props.info.code!)
               .then((response) => {
                 if (response.status) {
                   MfMessage.success(t('menu.tip.deleteSuccess').replace('$VALUE', String(props.info?.name ?? '')))
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
