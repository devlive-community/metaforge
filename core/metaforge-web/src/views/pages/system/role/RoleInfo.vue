<template>
  <MfModal v-model:open="visible" :title="title">
    <div class="relative">
      <MfForm v-if="formState" :model="formState" layout="vertical" @finish="onSubmit">
        <MfFormItem name="code" :label="$t('common.code')">
          <div class="flex w-full flex-col gap-1">
            <MfInput v-model="formState.code" :disabled="isEditing || loading" :maxlength="64"
                     :invalid="codeState === 'error'" :placeholder="$t('role.tip.codePlaceholder')" @blur="checkCode"/>
            <p aria-live="polite" class="text-xs leading-5"
               :class="codeState === 'error' ? 'text-danger' : codeState === 'available' ? 'text-success' : 'text-content-tertiary'">
              {{ isEditing ? $t('role.tip.codeReadonly') : codeMessage || $t('role.tip.codeHint') }}
            </p>
          </div>
        </MfFormItem>

        <MfFormItem name="name"
                    :label="$t('role.common.name')"
                    :rules="[
                      { required: true, message: $t('role.tip.name') },
                      { min: 2, message: $t('role.validate.nameSize') },
                      { max: 20, message: $t('role.validate.nameSize') }
                  ]">
          <MfInput v-model="formState.name" :disabled="loading" :placeholder="$t('role.tip.name')"/>
        </MfFormItem>

        <MfFormItem name="description"
                    :label="$t('role.common.description')"
                    :rules="[
                      { required: true, message: $t('role.tip.description') },
                      { min: 3, message: $t('role.validate.descriptionSize') },
                      { max: 50, message: $t('role.validate.descriptionSize') }
                  ]">
          <MfTextarea v-model="formState.description" :disabled="loading" :placeholder="$t('role.tip.description')"/>
        </MfFormItem>

        <div class="flex justify-end">
          <MfButton variant="primary" type="submit" :loading="loading" :disabled="loading">
            {{ title }}
          </MfButton>
        </div>
      </MfForm>
    </div>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { cloneDeep } from 'lodash'
import { RoleModel, RoleRequest } from '@/model/role'
import { StringUtils } from '@/utils/string'
import RoleService from '@/services/role'
import { normalizeRoleCode, roleErrorKey, validRoleCode } from '@/utils/role-code'

defineOptions({ name: 'RoleInfo' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: RoleModel | null }>(), {
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
const formState = ref<RoleModel>(null as unknown as RoleModel)
const isEditing = computed(() => !!props.info)
const codeState = ref<'idle' | 'checking' | 'available' | 'error'>('idle')
const codeMessage = ref('')
let codeRequest = 0
let pendingCode = ''
let pendingCheck: Promise<boolean> | undefined

watch(() => formState.value?.code, () => {
  codeRequest++
  pendingCheck = undefined
  codeState.value = 'idle'
  codeMessage.value = ''
})
onBeforeUnmount(() => { codeRequest++ })

const checkCode = (): Promise<boolean> => {
  if (isEditing.value) return Promise.resolve(true)
  const code = normalizeRoleCode(formState.value?.code)
  if (pendingCheck && pendingCode === code) return pendingCheck
  const request = ++codeRequest
  if (!validRoleCode(code)) {
    codeState.value = 'error'
    codeMessage.value = t('role.validate.codeFormat')
    return Promise.resolve(false)
  }
  if (!code) {
    codeState.value = 'idle'
    codeMessage.value = ''
    return Promise.resolve(true)
  }
  codeState.value = 'checking'
  codeMessage.value = t('role.tip.codeChecking')
  pendingCode = code
  const task = RoleService.checkCode(code)
    .then(response => {
      if (request !== codeRequest) return false
      const available = response.status && response.data === true
      codeState.value = available ? 'available' : 'error'
      const key = roleErrorKey(response.code)
      codeMessage.value = available ? t('role.tip.codeAvailable') : response.status ? t('role.validate.codeExists') : t(key || 'role.validate.codeCheckFailed')
      return available
    })
    .catch(() => {
      if (request === codeRequest) {
        codeState.value = 'error'
        codeMessage.value = t('role.validate.codeCheckFailed')
      }
      return false
    })
    .finally(() => { if (pendingCheck === task) pendingCheck = undefined })
  pendingCheck = task
  return task
}

const handlerInitialize = () => {
  if (props.info) {
    title.value = `${ StringUtils.replace(t('role.common.edit'), '$NAME', props.info.name as string) }`
    formState.value = cloneDeep(props.info)
  }
  else {
    title.value = t('role.common.create')
    formState.value = RoleRequest.of()
  }
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = async () => {
  if (loading.value) return
  if (isEditing.value && !props.info?.code) {
    MfMessage.error(t('role.validate.identityMismatch'))
    return
  }
  loading.value = true
  const payload = {
    ...formState.value,
    code: isEditing.value ? props.info?.code : normalizeRoleCode(formState.value.code) || undefined
  }
  try {
    if (!await checkCode()) return
    const response = await RoleService.saveOrUpdate(payload, props.info?.code)
    if (response.status) {
      MfMessage.success(`${ title.value } ${ t('common.successfully') }`)
      onCancel()
    }
    else {
      const key = roleErrorKey(response.code)
      MfMessage.error(key ? t(key) : response.message)
      if (response.code === 4029) {
        codeState.value = 'error'
        codeMessage.value = t('role.validate.codeExists')
      }
    }
  }
  finally { loading.value = false }
}

handlerInitialize()
</script>
