<template>
  <MfModal v-model:open="visible" :title="title">
    <MfSpace direction="vertical" fill :style="{ width: '100%' }">
      <MfAlert variant="danger" :title="$t('user.tip.deleteAlert1')"/>
      <MfAlert variant="danger" :title="$t('user.tip.deleteAlert2')"/>
      <MfAlert variant="danger" :title="$t('user.tip.deleteAlert3').replace('$VALUE', info?.username as string)"/>
    </MfSpace>

    <MfForm :model="formState" class="mt-3" @finish="onSubmit" @finish-failed="onError">
      <MfFormItem name="username"
                  :rules="[
                    { required: true, message: $t('user.validator.username.required') },
                    { validator: validateMatch }
                ]">
        <MfInput v-model="formState.username" :placeholder="$t('user.placeholder.username')"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfSpace>
          <MfButton @click="onCancel">
            {{ $t('common.cancel') }}
          </MfButton>
          <MfButton variant="danger" type="submit" :loading="loading">
            {{ $t('user.common.delete') }}
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
import { UserModel } from '@/model/user'
import UserService from '@/services/user'

defineOptions({ name: 'UserDelete' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: UserModel | null }>(), {
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
const title = computed(() => t('user.common.deleteInfo').replace('$VALUE', String(props.info?.username ?? '')))
const formState = ref<{ username: string }>({ username: '' })

const validateMatch = (value: string) => {
  if (value !== String(props.info?.username)) {
    return t('user.validator.username.match')
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
    UserService.deleteByCode(props.info.code!)
               .then((response) => {
                 if (response.status) {
                   MfMessage.success(t('user.tip.deleteSuccess').replace('$VALUE', String(props.info?.username ?? '')))
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
