<template>
  <MfModal v-model:open="visible" :title="title">
    <div class="relative">
      <MfForm v-if="formState" :model="formState" layout="vertical" @finish="onSubmit">
        <MfFormItem name="name"
                    :label="$t('role.common.name')"
                    :rules="[
                      { required: true, message: $t('role.tip.name') },
                      { min: 2, message: $t('role.validate.nameSize') },
                      { max: 20, message: $t('role.validate.nameSize') }
                  ]">
          <MfInput v-model="formState.name" :placeholder="$t('role.tip.name')"/>
        </MfFormItem>

        <MfFormItem name="description"
                    :label="$t('role.common.description')"
                    :rules="[
                      { required: true, message: $t('role.tip.description') },
                      { min: 3, message: $t('role.validate.descriptionSize') },
                      { max: 50, message: $t('role.validate.descriptionSize') }
                  ]">
          <MfTextarea v-model="formState.description" :placeholder="$t('role.tip.description')"/>
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
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { cloneDeep } from 'lodash'
import { RoleModel, RoleRequest } from '@/model/role'
import { StringUtils } from '@/utils/string'
import RoleService from '@/services/role'

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

const onSubmit = () => {
  loading.value = true
  RoleService.saveOrUpdate(formState.value)
             .then(response => {
               if (response.status) {
                 MfMessage.success(`${ title.value } ${ t('common.successfully') }`)
                 onCancel()
               }
               else {
                 MfMessage.error(response.message)
               }
             })
             .finally(() => (loading.value = false))
}

handlerInitialize()
</script>
