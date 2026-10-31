/**
 * Runtime theming API.
 *
 * A theme is a flat map of token name -> CSS value. `applyMfTheme` writes
 * each entry as a `--mf-*` custom property, which is exactly what the
 * components and the Tailwind `@theme inline` layer consume, so a theme
 * switch never re-renders component CSS.
 */

export type MfThemeMode = 'light' | 'dark'

export type MfThemeTokenName =
    | 'color-primary'
    | 'color-primary-hover'
    | 'color-primary-active'
    | 'color-on-primary'
    | 'color-success'
    | 'color-success-subtle'
    | 'color-warning'
    | 'color-warning-subtle'
    | 'color-danger'
    | 'color-danger-subtle'
    | 'color-info'
    | 'color-data-complex'
    | 'color-info-subtle'
    | 'color-code-bg'
    | 'color-code-header'
    | 'color-code-fg'
    | 'color-code-muted'
    | 'color-code-keyword'
    | 'color-code-type'
    | 'color-code-string'
    | 'color-code-identifier'
    | 'color-code-number'
    | 'color-page'
    | 'color-surface'
    | 'color-surface-hover'
    | 'color-surface-active'
    | 'color-content'
    | 'color-content-secondary'
    | 'color-content-tertiary'
    | 'color-content-disabled'
    | 'color-content-placeholder'
    | 'color-content-inverse'
    | 'color-line'
    | 'color-line-strong'
    | 'color-overlay'
    | 'radius-sm'
    | 'radius-md'
    | 'radius-lg'
    | 'radius-full'
    | 'shadow-sm'
    | 'shadow-md'
    | 'shadow-lg'
    | 'duration-fast'
    | 'duration-base'
    | 'duration-slow'
    | 'easing'
    | 'font-family'
    | 'font-size-sm'
    | 'font-size-md'
    | 'font-size-lg'

export type MfThemeTokens = Partial<Record<MfThemeTokenName, string>>

export const mfLightPreset: MfThemeTokens = {
    'color-primary': '#0A8F67',
    'color-primary-hover': '#087A59',
    'color-primary-active': '#086247',
    'color-on-primary': '#ffffff',
    'color-success': '#16A66A',
    'color-success-subtle': '#EAF8F1',
    'color-warning': '#E5A11A',
    'color-warning-subtle': '#FFF8E6',
    'color-danger': '#DC5656',
    'color-danger-subtle': '#FFF1F1',
    'color-info': '#3B82F6',
    'color-data-complex': '#7955DF',
    'color-info-subtle': '#EFF6FF',
    'color-code-bg': '#1F2733',
    'color-code-header': '#2A3340',
    'color-code-fg': '#E2E8F0',
    'color-code-muted': '#94A3B8',
    'color-code-keyword': '#5EB4F6',
    'color-code-type': '#D58CF5',
    'color-code-string': '#9BD86F',
    'color-code-identifier': '#F3D274',
    'color-code-number': '#F5A962',
    'color-page': '#F6FAF9',
    'color-surface': '#ffffff',
    'color-surface-hover': '#F0F7F4',
    'color-surface-active': '#DDF5EC',
    'color-content': '#14201C',
    'color-content-secondary': '#465A52',
    'color-content-tertiary': '#7D8D87',
    'color-content-disabled': '#BDC6C2',
    'color-content-placeholder': '#A8B3AF',
    'color-content-inverse': '#ffffff',
    'color-line': '#E3EAE7',
    'color-line-strong': '#C9D8D2',
    'color-overlay': 'rgb(20 32 28 / 0.45)',
    'radius-sm': '4px',
    'radius-md': '8px',
    'radius-lg': '12px',
    'radius-full': '9999px',
    'shadow-sm': '0 1px 2px rgb(15 23 42 / 0.04)',
    'shadow-md': '0 1px 2px rgb(15 23 42 / 0.02), 0 4px 16px rgb(15 23 42 / 0.035)',
    'shadow-lg': '0 4px 10px rgb(15 23 42 / 0.03), 0 12px 30px rgb(15 23 42 / 0.05)',
    'duration-fast': '120ms',
    'duration-base': '180ms',
    'duration-slow': '280ms',
    'easing': 'cubic-bezier(0.4, 0, 0.2, 1)',
    'font-family': "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif",
    'font-size-sm': '12px',
    'font-size-md': '14px',
    'font-size-lg': '16px',
}

export const mfDarkPreset: MfThemeTokens = {
    'color-primary': '#18A779',
    'color-primary-hover': '#49C399',
    'color-primary-active': '#12805E',
    'color-on-primary': '#ffffff',
    'color-success': '#3FBF8A',
    'color-success-subtle': 'rgb(63 191 138 / 0.14)',
    'color-warning': '#E8B84A',
    'color-warning-subtle': 'rgb(232 184 74 / 0.14)',
    'color-danger': '#E57272',
    'color-danger-subtle': 'rgb(229 114 114 / 0.14)',
    'color-info': '#6CA5F8',
    'color-data-complex': '#B39AFA',
    'color-info-subtle': 'rgb(108 165 248 / 0.14)',
    'color-code-bg': '#0F1720',
    'color-code-header': '#18212C',
    'color-code-fg': '#E2E8F0',
    'color-code-muted': '#94A3B8',
    'color-code-keyword': '#5EB4F6',
    'color-code-type': '#D58CF5',
    'color-code-string': '#9BD86F',
    'color-code-identifier': '#F3D274',
    'color-code-number': '#F5A962',
    'color-page': '#0C1412',
    'color-surface': '#111B18',
    'color-surface-hover': '#1A2823',
    'color-surface-active': '#1F3A30',
    'color-content': '#E8F0ED',
    'color-content-secondary': '#9FB2AB',
    'color-content-tertiary': '#6F817A',
    'color-content-disabled': '#4C5A55',
    'color-content-placeholder': '#6F817A',
    'color-content-inverse': '#0C1412',
    'color-line': '#22302B',
    'color-line-strong': '#33473F',
    'color-overlay': 'rgb(0 8 5 / 0.6)',
    'shadow-sm': '0 1px 2px 0 rgb(0 0 0 / 0.4)',
    'shadow-md': '0 4px 6px -1px rgb(0 0 0 / 0.45), 0 2px 4px -2px rgb(0 0 0 / 0.35)',
    'shadow-lg': '0 10px 15px -3px rgb(0 0 0 / 0.5), 0 4px 6px -4px rgb(0 0 0 / 0.4)',
}

/** Build a theme from the light preset plus user overrides. */
export function createMfTheme(overrides: MfThemeTokens = {}): MfThemeTokens {
    return { ...mfLightPreset, ...overrides }
}

export interface MfApplyThemeOptions {
    /** Also set the `data-theme` attribute (drives `[data-theme='dark']`). */
    mode?: MfThemeMode
    /** Element receiving the CSS variables. Defaults to `document.documentElement`. */
    target?: HTMLElement
}

/** Write theme tokens as `--mf-*` custom properties on the target element. */
export function applyMfTheme(tokens: MfThemeTokens, options: MfApplyThemeOptions = {}): void {
    const target = options.target ?? document.documentElement
    for (const [name, value] of Object.entries(tokens)) {
        if (value !== undefined) {
            target.style.setProperty(`--mf-${name}`, value)
        }
    }
    if (options.mode) {
        setMfThemeMode(options.mode, target)
    }
}

/** Switch light/dark by toggling the `data-theme` attribute. */
export function setMfThemeMode(mode: MfThemeMode, target: HTMLElement = document.documentElement): void {
    target.dataset.theme = mode
}
