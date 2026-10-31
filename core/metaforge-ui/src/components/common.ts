/**
 * Shared class fragments. Components compose these so sizes, focus rings
 * and motion stay consistent across the library.
 */

export const CONTROL_SIZES = {
    sm: 'h-8 px-3 text-sm gap-1.5',
    md: 'h-9 px-3.5 text-sm gap-2',
    lg: 'h-11 px-5 text-base gap-2.5',
} as const

export type MfControlSize = keyof typeof CONTROL_SIZES

export const FOCUS_RING = 'outline-none focus-visible:ring-2 focus-visible:ring-primary/40 focus-visible:ring-offset-1 focus-visible:ring-offset-surface'

export const FIELD_FOCUS_RING = 'focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/15'

export const TRANSITION = 'transition-[color,background-color,border-color,box-shadow,opacity,transform] duration-(--mf-duration-base) ease-(--mf-easing)'

export const DISABLED = 'disabled:pointer-events-none disabled:opacity-50'

/**
 * Whether a caller's class sets the width (w-48, w-[12rem], !w-60, md:w-72 ...). Fields fill their
 * container by default; that default must give way, or w-full would win over the caller's width.
 */
export function hasWidthClass(value: unknown): boolean {
    return /(^|\s)([\w-]+:)*!?w-/.test(typeof value === 'string' ? value : Array.isArray(value) ? value.join(' ') : '')
}

export interface MfSelectOption {
    label: string
    value: string | number
    disabled?: boolean
    /** Group heading; options sharing it render under one label. */
    group?: string
}
