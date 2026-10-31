export const SOURCE_LAYOUT_DEFAULTS = { left: 254, right: 324 }

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value))

export function readSourceWorkspaceLayout(raw: string | null) {
  try {
    const parsed = JSON.parse(raw || '{}')
    return {
      left: typeof parsed?.left === 'number' && Number.isFinite(parsed.left) ? clamp(parsed.left, 180, 480) : SOURCE_LAYOUT_DEFAULTS.left,
      right: typeof parsed?.right === 'number' && Number.isFinite(parsed.right) ? clamp(parsed.right, 280, 560) : SOURCE_LAYOUT_DEFAULTS.right,
    }
  } catch { return { ...SOURCE_LAYOUT_DEFAULTS } }
}

/** Preserve a usable center pane even when the viewport or both side panes change. */
export function fitSourceWorkspaceLayout(width: number, preferredLeft: number, preferredRight: number, rightOpen: boolean) {
  // Each query-style handle occupies 8px plus 4px margins on either side.
  const available = Math.max(0, width - (rightOpen ? 32 : 16))
  const centerMin = Math.min(360, available * .35)
  const leftMin = Math.min(180, available * .22)
  const rightMin = rightOpen ? Math.min(280, available * .32) : 0
  let left = clamp(Number.isFinite(preferredLeft) ? preferredLeft : SOURCE_LAYOUT_DEFAULTS.left, leftMin, 480)
  let right = rightOpen ? clamp(Number.isFinite(preferredRight) ? preferredRight : SOURCE_LAYOUT_DEFAULTS.right, rightMin, 560) : 0
  const extra = Math.max(0, left + right - (available - centerMin))
  if (extra) {
    const leftRoom = left - leftMin
    const rightRoom = right - rightMin
    const room = leftRoom + rightRoom
    if (room > 0) {
      left -= extra * leftRoom / room
      right -= extra * rightRoom / room
    }
  }
  left = Math.floor(left)
  right = Math.floor(right)
  return {
    left, right,
    leftMin: Math.floor(leftMin), rightMin: Math.floor(rightMin),
    leftMax: Math.floor(Math.min(480, available - centerMin - right)),
    rightMax: Math.floor(Math.min(560, available - centerMin - left)),
  }
}
