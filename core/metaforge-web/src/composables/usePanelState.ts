import { ref } from 'vue'

/**
 * 弹窗/抽屉面板的通用开关状态（Vue3 Composable）。
 *
 * 覆盖「打开携带目标行、关闭后刷新列表」的固定套路：
 *   const editor = usePanelState<Row>(row => row && reload())
 *   <XxxEditor :is-visible="editor.visible.value" :info="editor.target.value" @close="editor.close()"/>
 */
export function usePanelState<T = any>(onClosed?: (item: T | null) => void)
{
  const visible = ref(false)
  const target = ref<T | null>(null)

  const open = (item: T | null = null) => {
    target.value = item
    visible.value = true
  }

  const close = () => {
    visible.value = false
    const item = target.value
    target.value = null
    onClosed?.(item)
  }

  return { visible, target, open, close }
}
