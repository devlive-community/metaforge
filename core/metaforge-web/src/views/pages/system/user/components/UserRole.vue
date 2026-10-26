<template>
  <MfModal v-model:open="visible" :title="title">
    <div class="relative">
      <div v-if="loading" class="flex items-center justify-center py-8">
        <MfSpinner size="lg"/>
      </div>
      <MfForm v-else-if="formState" :model="formState" @finish="onSubmit" @finish-failed="onError">
        <MfFormItem name="roles"
                    :rules="[
                      { required: true, message: 'Please check the role', validator: (value: unknown) => Array.isArray(value) && value.length > 0 }
                  ]">
          <MfCheckboxGroup v-model="formState.roles" :options="roleOptions"/>
        </MfFormItem>

        <div class="flex justify-end">
          <MfButton variant="primary" type="submit" :loading="submitted" :disabled="submitted">
            {{ title }}
          </MfButton>
        </div>
      </MfForm>
    </div>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { StringUtils } from '@/utils/string'
import { UserModel, UserRoleModel } from '@/model/user'
import { FilterModel } from '@/model/filter'
import RoleService from '@/services/role'
import UserService from '@/services/user'
import { RoleModel } from '@/model/role'

defineOptions({ name: 'UserRole' })

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
const submitted = ref(false)
const title = ref<string | null>(null)
const data = ref<RoleModel[]>([])
const formState = ref<UserRoleModel | null>(null)

// 迁移至 MfCheckboxGroup：选项为数组形式
const roleOptions = computed(() => data.value.map((item: RoleModel) => ({ label: item.name, value: item.code })))

const handlerInitialize = () => {
  if (props.info) {
    title.value = `${ StringUtils.replace(t('role.common.assignRole'), '$NAME', props.info.username as string) }`
    formState.value = {
      roles: props.info.roles?.map(v => v.code) as string[],
      userId: props.info.id as number
    }
    loading.value = true
    const filter: FilterModel = new FilterModel()
    filter.size = 1000
    RoleService.getAll(filter)
               .then((response) => {
                 if (response.status) {
                   data.value = response.data.content
                 }
               })
               .finally(() => (loading.value = false))
  }
}

const onClose = () => {
  visible.value = false
}

const onSubmit = () => {
  if (props.info) {
    submitted.value = true
    UserService.assignRole(formState.value as UserRoleModel)
               .then((response) => {
                 if (response.status) {
                   MfMessage.success(t('user.tip.assignRoleSuccess'))
                   onClose()
                 }
                 else {
                   MfMessage.error(response.message)
                 }
               })
               .finally(() => (submitted.value = false))
  }
  else {
    MfMessage.error(t('common.invalidParam'))
  }
}

const onError = (error: any) => {
  const names = (error?.errorFields || []).map((field: any) => (Array.isArray(field.name) ? field.name.join('.') : field.name))
  MfMessage.error(`Validation error field: [ ${ names.join(', ') } ]`)
}

handlerInitialize()
</script>
