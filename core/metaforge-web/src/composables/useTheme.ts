import { ref, watch } from 'vue'
import { applyMfTheme, mfDarkPreset, mfLightPreset, setMfThemeMode } from 'metaforge-ui'

/**
 * 全局主题（明/暗 + 品牌色板）composable。
 *
 * - 单例状态：模块级 `isDark` / `brandKey`，全应用共享。
 * - 持久化：localStorage（metaforge-theme / metaforge-brand），刷新后保持。
 * - 作用于三处：
 *   1. <html> 上的 `dark` class 与 `data-theme` 属性（自定义 CSS 用）；
 *   2. <a-config-provider :theme> 的 antd 算法与品牌 token（antd 组件用）；
 *   3. src/styles/theme.css 里的 --dc-* CSS 变量（暗色通过 html.dark 覆盖，
 *      品牌变量如需多品牌在 styles/theme.css 中按 data-brand 覆盖）。
 *
 * 后续在用户偏好中支持主题设置：调用 setDark / setBrand 即可，
 * 无需改动任何组件。
 */
const DARK_KEY = 'metaforge-theme'
const BRAND_KEY = 'metaforge-brand'

/** 品牌色板注册表：后续新增主题在这里登记即可 */
export interface BrandTheme
{
    key: string
    label: string
}

const BRANDS: Record<string, BrandTheme> = {
    metaforge: {
        key: 'metaforge',
        label: 'MetaForge Green'
    }
}

const isDark = ref<boolean>(localStorage.getItem(DARK_KEY) === 'dark')
const brandKey = ref<string>(localStorage.getItem(BRAND_KEY) || 'metaforge')

function applyToDocument(dark: boolean): void {
    const el = document.documentElement
    if (dark) {
        el.classList.add('dark')
    }
    else {
        el.classList.remove('dark')
    }
    el.setAttribute('data-theme', dark ? 'dark' : 'light')
    // metaforge-ui token（Mf* 组件与语义工具类直接消费 --mf-* 变量）
    setMfThemeMode(dark ? 'dark' : 'light')
    applyMfTheme(dark ? mfDarkPreset : mfLightPreset)
}

function applyBrand(key: string): void {
    const el = document.documentElement
    if (BRANDS[key]) {
        el.setAttribute('data-brand', key)
    }
    else {
        el.removeAttribute('data-brand')
    }
}

// 首次加载即应用一次
applyToDocument(isDark.value)
applyBrand(brandKey.value)

watch(isDark, (value) => {
    localStorage.setItem(DARK_KEY, value ? 'dark' : 'light')
    applyToDocument(value)
})

watch(brandKey, (value) => {
    localStorage.setItem(BRAND_KEY, value)
    applyBrand(value)
})

export function useTheme() {
    const brand = ref<BrandTheme>(BRANDS[brandKey.value] || BRANDS.metaforge)


    const toggle = (): void => {
        isDark.value = !isDark.value
    }

    const setDark = (value: boolean): void => {
        isDark.value = value
    }

    /** 切换品牌色板（后续用户偏好的主题设置入口） */
    const setBrand = (key: string): void => {
        if (BRANDS[key]) {
            brandKey.value = key
        }
    }

    return {
        isDark,
        brand,
        brands: BRANDS,
        toggle,
        setDark,
        setBrand
    }
}
