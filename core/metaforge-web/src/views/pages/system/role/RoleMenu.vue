<template>
  <MfModal v-model:open="visible" :title="title">
    <div class="relative">
      <div v-if="loading" class="flex items-center justify-center py-8">
        <MfSpinner size="lg"/>
      </div>
      <template v-else>
        <!-- a-tree checkable 等价实现：MfCheckbox + 扁平化缩进渲染（级联勾选） -->
        <div class="flex flex-col gap-1 max-h-[50vh] overflow-y-auto pr-1">
          <div v-for="node in flatNodes"
               :key="node.key"
               class="flex items-center gap-1"
               :style="{ paddingLeft: `${ node.depth * 20 }px` }">
            <button v-if="node.hasChildren"
                    type="button"
                    class="inline-flex size-5 shrink-0 items-center justify-center rounded-mf-sm text-content-tertiary hover:bg-surface-hover hover:text-content"
                    @click="toggleExpand(node.key)">
              <RightOutlined :class="['transition-transform duration-200', expandedNodes.includes(node.key) && 'rotate-90']" :style="{ fontSize: '12px' }"/>
            </button>
            <span v-else class="inline-block size-5 shrink-0"/>
            <MfCheckbox :model-value="isChecked(node.key)"
                        :indeterminate="isIndeterminate(node.key)"
                        @update:model-value="(checked: boolean) => onCheck(node, checked)">
              {{ node.label }}
            </MfCheckbox>
          </div>
        </div>

        <div class="flex justify-end">
          <MfButton variant="primary" :loading="saving" :disabled="saving" @click="onSubmit()">
            {{ title }}
          </MfButton>
        </div>
      </template>
    </div>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { RightOutlined } from '@ant-design/icons-vue'
import { RoleModel } from '@/model/role'
import UserService from '@/services/user'
import RoleService from '@/services/role'
import CommonUtils from '@/utils/common'

defineOptions({ name: 'RoleMenu' })

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
const saving = ref(false)
const title = ref<string | null>(null)
const data = ref<any[]>([])
const menus = ref<any[]>([])

const handleData = (source: any[]): any[] => {
  return source.map(item => {
    const node: { value: any; label: any; children: any[] } = {
      value: item.id,
      label: item.title,
      children: []
    }
    if (item.children) {
      node.children = handleData(item.children)
    }
    return node
  })
}

const extractSelectedItems = (source: any): any[] => {
  let selectedItems = [] as any[]
  source.forEach((item: { selected: boolean; children: any[] }) => {
    if (item.selected) {
      selectedItems.push(item)
    }
    if (item.children) {
      selectedItems = selectedItems.concat(extractSelectedItems(item.children))
    }
  })
  return selectedItems
}

// ---- a-tree checkable 等价实现：索引 / 扁平化 / 级联勾选 ----
interface MenuFlatNode
{
  key: any
  label: string
  depth: number
  hasChildren: boolean
}

const expandedNodes = ref<any[]>([])

const menuIndex = computed(() => {
  const childKeys = new Map<any, any[]>()
  const parentOf = new Map<any, any>()
  const walk = (nodes: any[], parent: any) => {
    nodes.forEach((node: any) => {
      childKeys.set(node.value, (node.children || []).map((child: any) => child.value))
      if (parent !== undefined) {
        parentOf.set(node.value, parent)
      }
      if (node.children) {
        walk(node.children, node.value)
      }
    })
  }
  walk(data.value, undefined)
  return { childKeys, parentOf }
})

const toFlatNodes = (nodes: any[], depth: number): MenuFlatNode[] => {
  return nodes.flatMap((node: any) => {
    const hasChildren = !!(node.children && node.children.length)
    const row: MenuFlatNode = { key: node.value, label: node.label, depth, hasChildren }
    return [row, ...(hasChildren && expandedNodes.value.includes(node.value) ? toFlatNodes(node.children, depth + 1) : [])]
  })
}

const flatNodes = computed(() => toFlatNodes(data.value, 0))

// 与 antd 级联语义一致：父选中则子孙选中；子全选则父选中
const checkedKeySet = computed(() => {
  const set = new Set<any>(menus.value)
  const addDown = (nodes: any[]) => {
    nodes.forEach((node: any) => {
      if (set.has(node.value) && node.children?.length) {
        node.children.forEach((child: any) => set.add(child.value))
        addDown(node.children)
      }
    })
  }
  addDown(data.value)
  const markUp = (nodes: any[]) => {
    nodes.forEach((node: any) => {
      if (node.children?.length) {
        markUp(node.children)
        if (node.children.every((child: any) => set.has(child.value))) {
          set.add(node.value)
        }
      }
    })
  }
  markUp(data.value)
  return set
})

const isChecked = (key: any) => checkedKeySet.value.has(key)

const isIndeterminate = (key: any): boolean => {
  const kids = menuIndex.value.childKeys.get(key) || []
  if (!kids.length) {
    return false
  }
  return kids.some(kid => checkedKeySet.value.has(kid) || isIndeterminate(kid))
}

const toggleExpand = (key: any) => {
  expandedNodes.value = expandedNodes.value.includes(key)
                            ? expandedNodes.value.filter(item => item !== key)
                            : [...expandedNodes.value, key]
}

const onCheck = (node: MenuFlatNode, checked: boolean) => {
  const set = new Set<any>(menus.value)
  const { childKeys, parentOf } = menuIndex.value
  const addWith = (key: any) => {
    set.add(key)
    ;(childKeys.get(key) || []).forEach(addWith)
  }
  const removeWith = (key: any) => {
    set.delete(key)
    ;(childKeys.get(key) || []).forEach(removeWith)
  }
  if (checked) {
    addWith(node.key)
    let parent = parentOf.get(node.key)
    while (parent !== undefined) {
      const kids = childKeys.get(parent) || []
      if (kids.every(kid => set.has(kid))) {
        set.add(parent)
      }
      parent = parentOf.get(parent)
    }
  }
  else {
    removeWith(node.key)
    let parent = parentOf.get(node.key)
    while (parent !== undefined) {
      set.delete(parent)
      parent = parentOf.get(parent)
    }
  }
  menus.value = Array.from(set)
}

const handlerInitialize = () => {
  loading.value = true
  if (props.info) {
    title.value = t('role.common.assignMenu').replace('$NAME', props.info.name as string)
    RoleService.getAllMenuById(props.info?.id as number)
               .then((response) => {
                 if (response.status) {
                   const source = response.data
                   data.value = handleData(source)
                   menus.value = extractSelectedItems(source).map(item => item.id)
                 }
               })
               .finally(() => (loading.value = false))
  }
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  saving.value = true
  RoleService.saveMenu(props.info?.id as number, menus.value)
             .then((response) => {
               if (response.status) {
                 UserService.getMenus()
                            .then(res => {
                              if (res.status) {
                                localStorage.setItem(CommonUtils.menu, JSON.stringify(res.data))
                              }
                            })
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
