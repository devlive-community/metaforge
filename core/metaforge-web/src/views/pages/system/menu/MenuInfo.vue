<template>
  <MfDrawer v-model:open="visible" :title="title" width="40%">
    <div v-if="loading" class="flex items-center justify-center py-8">
      <MfSpinner size="lg"/>
    </div>
    <MfForm v-else-if="formState" id="menu-info-form" :model="formState" layout="vertical" @finish="onSubmit">
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
            <div ref="iconPickerRoot" class="relative inline-flex w-full">
              <div class="mf-invalidable inline-flex h-9 w-full cursor-pointer items-center gap-2 rounded-mf-md border bg-surface pl-3 pr-2.5 text-sm transition"
                   :class="iconPickerVisible ? 'border-primary' : 'border-line'"
                   @click="iconPickerVisible = !iconPickerVisible">
                <span class="flex min-w-0 flex-1 items-center gap-2 text-left">
                  <component :size="15" v-if="formState.icon && MENU_ICONS[formState.icon]"
                             :is="MENU_ICONS[formState.icon]" class="shrink-0 text-primary" />
                  <span class="truncate" :class="formState.icon ? 'text-content' : 'text-content-placeholder'">
                    {{ formState.icon || $t('menu.tip.selectIcon') }}
                  </span>
                </span>
                <span v-if="formState.icon"
                      class="inline-flex size-4 shrink-0 items-center justify-center rounded-mf-full text-content-tertiary hover:bg-surface-hover hover:text-content"
                      @click.stop="clearIcon">
                  <svg class="size-2.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"/></svg>
                </span>
                <svg class="size-3.5 shrink-0 text-content-tertiary transition-transform duration-(--mf-duration-base) ease-(--mf-easing)"
                     :class="iconPickerVisible && 'rotate-180'"
                     viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <path d="M6 9l6 6 6-6"/>
                </svg>
              </div>

              <div v-if="iconPickerVisible"
                   class="absolute left-0 top-full z-40 mt-1.5 w-full rounded-mf-lg border border-line bg-surface p-1 shadow-mf-lg">
                <div class="p-1 pb-2">
                  <MfInput v-model="iconSearch" :placeholder="$t('menu.tip.searchIcon')">
                    <template #prefix>
                      <Search :size="13" :style="{ color: 'var(--dc-text-secondary)' }"/>
                    </template>
                  </MfInput>
                </div>
                <div class="max-h-64 overflow-y-auto">
                  <div class="grid grid-cols-6 gap-1">
                    <button v-for="(component, name) in filteredIcons"
                            :key="name"
                            type="button"
                            :title="name"
                            class="flex size-10 cursor-pointer items-center justify-center rounded-mf-md transition"
                            :class="formState.icon === name
                              ? 'bg-surface-active font-medium text-primary'
                              : 'text-content-secondary hover:bg-surface-hover hover:text-content'"
                            @click="selectIcon(name)">
                      <component :size="16" :is="component" />
                    </button>
                  </div>
                  <div v-if="Object.keys(filteredIcons).length === 0" class="py-6 text-center text-sm text-content-tertiary">
                    {{ $t('common.noData') }}
                  </div>
                </div>
              </div>
            </div>
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
            <MfSelect v-model="formState.parent" :placeholder="$t('menu.tip.selectParent')" clearable searchable :options="menuOptions"/>
          </MfFormItem>
        </div>

        <div>
          <MfFormItem :label="$t('menu.common.redirect')">
            <MfSelect v-model="formState.redirect" :placeholder="$t('menu.tip.selectRedirect')" clearable searchable :options="menuOptions"/>
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
            <MfSelect v-model="formState.i18nKey" :placeholder="$t('menu.common.i18nKey')" searchable :options="i18nKeyOptions"/>
          </MfFormItem>
        </div>
      </div>

      <MfFormItem name="description" :label="$t('common.description')">
        <MfTextarea v-model="formState.description"/>
      </MfFormItem>
    </MfForm>

    <template #footer>
      <MfButton @click="onCancel">
        {{ $t('common.cancel') }}
      </MfButton>
      <MfButton variant="primary" type="submit" form="menu-info-form" :loading="saving" :disabled="saving || loading">
        {{ $t('common.save') }}
      </MfButton>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage, useDismissableLayer } from 'metaforge-ui'
import { cloneDeep } from 'lodash'
import { Search } from '@lucide/vue'
import { MenuModel, MenuRequest } from '@/model/menu'
import MenuService from '@/services/menu'
import { FilterModel } from '@/model/filter'
import { MENU_ICONS } from '@/config/menu-icons'

defineOptions({ name: 'MenuInfo' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: MenuModel | null }>(), {
  isVisible: false,
  info: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t, locale, getLocaleMessage } = useI18n()

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

// 国际化标识候选项：摊平当前语言包的全部 key，label 附带翻译值便于搜索
const flattenI18nKeys = (messages: Record<string, unknown>, prefix = '', out: string[] = []): string[] => {
  for (const [key, value] of Object.entries(messages ?? {})) {
    const path = prefix ? `${prefix}.${key}` : key
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      flattenI18nKeys(value as Record<string, unknown>, path, out)
    }
    else {
      out.push(path)
    }
  }
  return out
}

const i18nKeyOptions = computed(() => {
  return flattenI18nKeys(getLocaleMessage(locale.value) as Record<string, unknown>)
             .sort((a, b) => a.localeCompare(b))
             .map((key) => {
               const translated = t(key)
               return { value: key, label: translated !== key ? `${key} · ${translated}` : key }
             })
})

const iconPickerVisible = ref(false)
const iconPickerRoot = ref<HTMLElement>()
useDismissableLayer(iconPickerRoot, iconPickerVisible, () => {
  iconPickerVisible.value = false
})

const iconSearch = ref('')
const filteredIcons = computed(() => {
  const query = iconSearch.value.trim().toLowerCase()
  if (!query) {
    return MENU_ICONS
  }
  return Object.fromEntries(Object.entries(MENU_ICONS).filter(([name]) => name.toLowerCase().includes(query)))
})

const selectIcon = (name?: string) => {
  formState.value.icon = name
  iconPickerVisible.value = false
}

const clearIcon = () => {
  formState.value.icon = undefined
  iconSearch.value = ''
}

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
