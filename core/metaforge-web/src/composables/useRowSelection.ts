import { computed, ref } from 'vue'

/**
 * 表格行多选状态（Vue3 Composable）。
 *
 * 基于行键集合维护勾选，提供全选/半选与单个切换。
 */
export function useRowSelection<T = any>(rows: () => T[], keyOf: (row: T) => string)
{
  const selected = ref<Set<string>>(new Set())

  const allSelected = computed(() => rows().length > 0 && rows().every(row => selected.value.has(keyOf(row))))
  const partialSelected = computed(() => selected.value.size > 0 && !allSelected.value)

  const toggleAll = (checked: boolean) => {
    selected.value = checked ? new Set(rows().map(keyOf)) : new Set()
  }

  const toggle = (key: string, checked: boolean) => {
    const next = new Set(selected.value)
    if (checked) {
      next.add(key)
    }
    else {
      next.delete(key)
    }
    selected.value = next
  }

  const select = (keys: string[]) => {
    selected.value = new Set(keys)
  }

  /** 行变化后清掉已不存在的勾选。 */
  const prune = () => {
    const alive = new Set(rows().map(keyOf))
    selected.value = new Set([...selected.value].filter(key => alive.has(key)))
  }

  return { selected, allSelected, partialSelected, toggleAll, toggle, select, prune }
}
