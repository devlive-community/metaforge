<template>
  <MfForm v-if="formState" :model="formState" layout="vertical" @finish="onSubmit" @finish-failed="onError">
    <MfFormItem name="username"
                :label="$t('user.common.username')"
                :rules="[
                  { required: true, message: $t('user.auth.usernameTip') },
                  { min: 3, message: $t('user.auth.usernameSizeTip') },
                  { max: 20, message: $t('user.auth.usernameSizeTip') }
              ]">
      <MfInput v-model="formState.username"/>
    </MfFormItem>

    <MfFormItem v-if="!props.info" name="password"
                :label="$t('user.common.password')"
                :rules="[
                  { required: true, message: $t('user.auth.passwordTip') },
                  { min: 6, message: $t('user.auth.passwordSizeTip') },
                  { max: 20, message: $t('user.auth.passwordSizeTip') }
              ]">
      <MfInput v-model="formState.password"
               type="password"
               show-password
               :placeholder="$t('user.auth.passwordTip')"/>
    </MfFormItem>

    <MfFormItem v-if="!props.info" name="confirmPassword"
                :label="$t('user.common.confirmPassword')"
                :rules="[
                  { required: true, message: $t('user.auth.passwordTip') },
                  { validator: validatePassword }
              ]">
      <MfInput v-model="formState.confirmPassword"
               type="password"
               show-password
               :placeholder="$t('user.auth.confirmPasswordTip')"/>
    </MfFormItem>

    <MfFormItem name="roles" :label="$t('user.common.assignRole')">
      <div v-if="rolesLoading" class="flex items-center justify-center py-4">
        <MfSpinner size="md"/>
      </div>
      <MfCheckboxGroup v-else v-model="selectedRoleCodes" :options="roleOptions"/>
    </MfFormItem>

    <div class="flex justify-end">
      <MfButton variant="primary" type="submit" :loading="loading" :disabled="loading">
        {{ $t('common.save') }}
      </MfButton>
    </div>
  </MfForm>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { UserModel } from '@/model/user'
import UserService from '@/services/user'
import RoleService from '@/services/role'
import { RoleModel } from '@/model/role'
import { FilterModel } from '@/model/filter'

defineOptions({ name: 'UserForm' })

const props = defineProps<{ info?: UserModel }>()
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()

const loading = ref(false)
const rolesLoading = ref(false)
const roleData = ref<RoleModel[]>([])
const selectedRoleCodes = ref<string[]>([])
const formState = ref<UserModel>({
  code: undefined,
  username: undefined,
  password: undefined,
  confirmPassword: undefined
} as UserModel)

if (props.info) {
  const { code, username } = props.info as UserModel
  formState.value = { code, username } as UserModel
}

// roles ride along with the save payload; an empty set on edit clears the assignment
const roleOptions = computed(() => roleData.value.map((item: RoleModel) => ({ label: item.name, value: item.code })))

const handlerInitializeRoles = () => {
  rolesLoading.value = true
  const filter: FilterModel = new FilterModel()
  filter.size = 1000
  RoleService.getAll(filter)
             .then((response) => {
               if (response.status) {
                 roleData.value = response.data.content
               }
             })
             .finally(() => (rolesLoading.value = false))
}

const validatePassword = (value: string) => {
  if (value !== formState.value.password) {
    return t('user.auth.passwordNotMatchTip')
  }
  return true
}

const onError = (error: any) => {
  const names = (error?.errorFields || []).map((field: any) => (Array.isArray(field.name) ? field.name.join('.') : field.name))
  MfMessage.error(`Validation error field: [ ${ names.join(', ') } ]`)
}

const onSubmit = () => {
  loading.value = true
  const payload: UserModel = {
    ...formState.value,
    roles: selectedRoleCodes.value.map(code => ({ code }) as RoleModel)
  }
  UserService.saveOrUpdate(payload)
             .then((response) => {
               if (response.status) {
                 emit('close', true)
               }
               else {
                 MfMessage.error(response.message)
               }
             })
             .finally(() => (loading.value = false))
}

onMounted(() => {
  if (props.info?.roles?.length) {
    selectedRoleCodes.value = props.info.roles.map(role => role.code!) ?? []
  }
  handlerInitializeRoles()
})
</script>
