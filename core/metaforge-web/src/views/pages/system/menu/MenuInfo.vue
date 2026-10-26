<template>
  <MfDrawer v-model:open="visible" :title="title" width="40%">
    <div v-if="loading" class="flex items-center justify-center py-8">
      <MfSpinner size="lg"/>
    </div>
    <MfForm v-else-if="formState" :model="formState" layout="vertical" @finish="onSubmit">
      <div class="grid grid-cols-2 gap-4">
        <div>
          <MfFormItem name="name"
                      :label="$t('common.name')"
                      :rules="[{ required: true, message: $t('common.name') }]">
            <MfInput v-model="formState.name"/>
          </MfFormItem>
        </div>

        <div>
          <MfFormItem name="icon" :label="$t('menu.common.icon')">
            <MfInput v-model="formState.icon"/>
          </MfFormItem>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <MfFormItem name="url"
                      :label="$t('common.url')"
                      :rules="[{ required: true, message: $t('common.url') }]">
            <MfInput v-model="formState.url"/>
          </MfFormItem>
        </div>

        <div>
          <MfFormItem name="group" :label="$t('common.group')">
            <MfInput v-model="formState.group"/>
          </MfFormItem>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <MfFormItem name="sorted" :label="$t('common.sorted')" :rules="[{ required: true, message: $t('common.sorted') }]">
            <MfInputNumber v-model="formState.sorted" :style="{ width: '100%' }"/>
          </MfFormItem>
        </div>

        <div>
          <MfFormItem name="type"
                      :label="$t('common.type')"
                      :rules="[{ required: true, message: $t('common.type') }]">
            <MfSelect v-model="formState.type" :placeholder="$t('menu.tip.selectType')" :options="[{ label: 'VIEW', value: 'VIEW' }]"/>
          </MfFormItem>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <MfFormItem :label="$t('common.parent')">
            <MfSelect v-model="formState.parent" :placeholder="$t('menu.tip.selectParent')" clearable :options="menuOptions"/>
          </MfFormItem>
        </div>

        <div>
          <MfFormItem :label="$t('menu.common.redirect')">
            <MfSelect v-model="formState.redirect" :placeholder="$t('menu.tip.selectRedirect')" clearable :options="menuOptions"/>
          </MfFormItem>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <MfFormItem name="new" :label="$t('menu.common.new')">
            <MfSwitch v-model="formState.new"/>
          </MfFormItem>
        </div>

        <div>
          <MfFormItem name="i18nKey" :label="$t('menu.common.i18nKey')" :rules="[{ required: true, message: $t('menu.common.i18nKey') }]">
            <MfInput v-model="formState.i18nKey"/>
          </MfFormItem>
        </div>
      </div>

      <MfFormItem name="description" :label="$t('common.description')">
        <MfTextarea v-model="formState.description"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfButton variant="primary" type="submit" :loading="loading" :disabled="loading">
          {{ $t('common.save') }}
        </MfButton>
      </div>
    </MfForm>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { cloneDeep } from 'lodash'
import { MenuModel, MenuRequest } from '@/model/menu'
import MenuService from '@/services/menu'
import { FilterModel } from '@/model/filter'

defineOptions({ name: 'MenuInfo' })

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

const title = ref<string | null>(null)
const formState = ref<MenuModel>(null as unknown as MenuModel)
const loading = ref(false)
const fullMenus = ref<MenuModel[]>([])
const saving = ref(false)

// 迁移至 MfSelect：选项为数组形式
const menuOptions = computed(() => fullMenus.value.map((menu: MenuModel) => ({ label: menu.name, value: menu.id })))

const handlerInitialize = () => {
  title.value = `${ t('menu.common.create') }`
  if (props.info) {
    formState.value = cloneDeep(props.info)
    title.value = `${ t('menu.common.modify').replace('$NAME', props.info.name as string) }`
  }
  else {
    formState.value = MenuRequest.of()
  }

  const filter: FilterModel = new FilterModel()
  loading.value = true
  MenuService.getAll(filter)
             .then((response) => {
               if (response.status) {
                 fullMenus.value = response.data.content
               }
             })
             .finally(() => (loading.value = false))
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  saving.value = true
  MenuService.saveOrUpdate(formState.value)
             .then(response => {
               if (response.status) {
                 MfMessage.success('successful')
                 onCancel()
               }
               else {
                 MfMessage.error(response.message)
               }
             })
             .finally(() => (saving.value = false))
}

handlerInitialize()
</script>
