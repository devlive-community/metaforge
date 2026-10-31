import { ref } from 'vue'

export type DropSide = 'before' | 'after'

/** Row classes for the drop target: a tinted row plus an insertion line on the side the item lands. */
export const DROP_SIDE_CLASS: Record<DropSide, string> = {
  before: 'bg-primary/10 shadow-[inset_0_2px_0_var(--mf-color-primary)]',
  after: 'bg-primary/10 shadow-[inset_0_-2px_0_var(--mf-color-primary)]',
}

/** The same marker for table rows, drawn on the cells because `<tr>` does not paint box shadows reliably. */
export const DROP_ROW_CLASS: Record<DropSide, string> = {
  before: '[&>td]:bg-primary/10 [&>td]:shadow-[inset_0_2px_0_var(--mf-color-primary)]',
  after: '[&>td]:bg-primary/10 [&>td]:shadow-[inset_0_-2px_0_var(--mf-color-primary)]',
}

/** Tracks a drag inside a sortable list so the row under the pointer can show where the dragged item will land. */
export function useDragSort(order: () => string[]) {
  const dragging = ref<string | null>(null)
  const over = ref<string | null>(null)
  function start(name: string, event: DragEvent) {
    dragging.value = name
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move'
      event.dataTransfer.setData('text/plain', name)
    }
  }
  function enter(name: string) {
    if (dragging.value) over.value = name
  }
  /** Clears the target once the pointer leaves the list itself, not when it crosses between rows. */
  function leave(event: DragEvent) {
    if (!(event.currentTarget as Node).contains(event.relatedTarget as Node | null)) over.value = null
  }
  function end() {
    dragging.value = null
    over.value = null
  }
  /** The side of `name` the dragged item lands on, matching a move to that row's index. */
  function dropSide(name: string): DropSide | null {
    if (!dragging.value || over.value !== name || name === dragging.value) return null
    const names = order()
    return names.indexOf(dragging.value) < names.indexOf(name) ? 'after' : 'before'
  }
  /** Ends the drag and returns the dragged name when it was dropped on another row. */
  function drop(target: string): string | null {
    const name = dragging.value
    end()
    return name && name !== target ? name : null
  }
  return { dragging, over, start, enter, leave, end, dropSide, drop }
}
