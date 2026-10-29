<script setup lang="ts">
import { computed, ref } from 'vue'
import {
    applyMfTheme,
    createMfTheme,
    mfDarkPreset,
    mfLightPreset,
    setMfThemeMode,
    type MfThemeMode,
} from '../src/theme/theme'
import {
    MfAlert,
    MfAvatar,
    MfBadge,
    MfButton,
    MfCard,
    MfCheckbox,
    MfConfigProvider,
    MfDivider,
    MfDatePicker,
    MfEmpty,
    MfInput,
    MfLoadingIcon,
    MfMenu,
    MfMenuItem,
    MfModal,
    MfRadio,
    MfRadioGroup,
    MfSelect,
    MfSpace,
    MfSpinner,
    MfSubMenu,
    MfSwitch,
    MfTag,
    MfTableToolbar,
    MfTextarea,
    MfTooltip,
} from '../src/index'

const mode = ref<MfThemeMode>('light')
const menuMode = ref<'horizontal' | 'inline'>('horizontal')
const menuSelectedKeys = ref<(string | number)[]>(['/home'])
const menuClickCount = ref(0)
const navigation = [
    { key: '/home', label: '首页' },
    { key: '/dashboard', label: '仪表盘' },
    { key: '/dataset', label: '数据集' },
    { key: '/store', label: '商店' },
    { key: '/query', label: '查询' },
]
const primary = ref('#2563eb')
const brandLoading = ref(true)
const toolbarKeyword = ref('')
const toolbarCode = ref('')
const toolbarDate = ref('')
const toolbarSearches = ref(0)

const presets = {
    light: mfLightPreset,
    dark: mfDarkPreset,
} as const

function applyTheme(): void {
    const base = presets[mode.value]
    setMfThemeMode(mode.value)
    applyMfTheme(createMfTheme({
        ...base,
        'color-primary': primary.value,
        'color-primary-hover': primary.value,
    }))
}

function switchMode(next: MfThemeMode): void {
    mode.value = next
    applyTheme()
}

const primarySwatches = ['#2563eb', '#7c3aed', '#db2777', '#ea580c', '#059669']

// form state for the demo
const input = ref('MetaForge')
const textarea = ref('')
const select = ref('mysql')
const selectOptions = [
    { label: 'MySQL', value: 'mysql' },
    { label: 'ClickHouse', value: 'clickhouse' },
    { label: 'PostgreSQL', value: 'postgresql' },
    { label: 'Trino', value: 'trino', disabled: true },
]
const agree = ref(true)
const engines = ref<string[]>(['local'])
const engine = ref('local')
const darkMode = ref(false)
const modalOpen = ref(false)
const loading = ref(false)

const themeTokens = computed(() => ({
    'color-primary': primary.value,
    'color-primary-hover': primary.value,
}))

function simulateLoading(): void {
    loading.value = true
    setTimeout(() => {
        loading.value = false
    }, 1500)
}
</script>

<template>
    <MfConfigProvider :mode="mode" :tokens="themeTokens">
        <div class="min-h-screen bg-page p-8 font-(--mf-font-family) text-content">
            <header class="mx-auto mb-8 flex max-w-4xl flex-wrap items-center justify-between gap-4">
                <div>
                    <h1 class="text-2xl font-semibold">metaforge-ui</h1>
                    <p class="mt-1 text-sm text-content-secondary">
                        Pure Tailwind CSS components driven by design tokens.
                    </p>
                </div>
                <MfSpace>
                    <MfRadioGroup v-model="mode" name="mode" @change="switchMode($event as MfThemeMode)">
                        <MfRadio value="light" label="Light" />
                        <MfRadio value="dark" label="Dark" />
                    </MfRadioGroup>
                    <MfSpace size="sm">
                        <button
                            v-for="color in primarySwatches"
                            :key="color"
                            class="size-6 rounded-mf-full border-2 transition-[transform] duration-(--mf-duration-fast) hover:scale-110"
                            :class="primary === color ? 'border-content' : 'border-transparent'"
                            :style="{ backgroundColor: color }"
                            :aria-label="`primary ${color}`"
                            @click="primary = color"
                        />
                    </MfSpace>
                </MfSpace>
            </header>

            <main class="mx-auto grid max-w-4xl gap-6">
                <div>
                    <h2 class="mb-3 font-semibold text-content">数据列表功能区</h2>
                    <MfTableToolbar search-text="查询" reset-text="重置" more-text="更多筛选" less-text="收起筛选"
                                    @search="toolbarSearches++" @reset="toolbarKeyword = ''; toolbarCode = ''; toolbarDate = ''">
                        <template #filter>
                            <label class="flex flex-col gap-1.5 text-sm text-content-secondary">关键词<MfInput v-model="toolbarKeyword" placeholder="按名称搜索"/></label>
                        </template>
                        <template #advanced-filter>
                            <label class="flex flex-col gap-1.5 text-sm text-content-secondary">编码<MfInput v-model="toolbarCode"/></label>
                            <label class="flex flex-col gap-1.5 text-sm text-content-secondary">创建日期<MfDatePicker v-model="toolbarDate" locale="zh-cn" placeholder="请选择日期" today-text="今天" clear-text="清空"/></label>
                        </template>
                        <template #actions><MfButton variant="primary">创建数据源</MfButton></template>
                        <template #tools><MfButton>刷新</MfButton><MfButton>列设置</MfButton></template>
                    </MfTableToolbar>
                    <p class="text-xs text-content-tertiary">查询次数：{{ toolbarSearches }}</p>
                </div>
                <MfCard title="Navigation">
                    <div class="space-y-4">
                        <MfRadioGroup v-model="menuMode" variant="button">
                            <MfRadio value="horizontal">顶部导航</MfRadio>
                            <MfRadio value="inline">侧边导航</MfRadio>
                        </MfRadioGroup>
                        <MfMenu :mode="menuMode" :selected-keys="menuSelectedKeys" @click="menuSelectedKeys = [$event.key]; menuClickCount++">
                            <MfMenuItem v-for="item in navigation" :key="item.key" :key_="item.key">
                                <a :href="`#${item.key}`" class="flex items-center">{{ item.label }}</a>
                            </MfMenuItem>
                            <MfMenuItem key_="/disabled" disabled>
                                <a href="#disabled" class="flex items-center">禁用链接</a>
                            </MfMenuItem>
                            <MfSubMenu key_="/workspace" title="数据开发">
                                <MfMenuItem key_="/source">数据源</MfMenuItem>
                                <MfMenuItem key_="/workflow">工作流</MfMenuItem>
                            </MfSubMenu>
                            <MfSubMenu key_="/admin" title="系统管理">
                                <MfMenuItem key_="/admin/user">用户</MfMenuItem>
                                <MfMenuItem key_="/admin/configure">运行时配置</MfMenuItem>
                            </MfSubMenu>
                        </MfMenu>
                        <p class="text-xs text-content-tertiary">触发次数：{{ menuClickCount }}</p>
                    </div>
                </MfCard>
                <MfCard title="Buttons">
                    <MfSpace wrap>
                        <MfButton variant="primary">Primary</MfButton>
                        <MfButton>Secondary</MfButton>
                        <MfButton variant="outline">Outline</MfButton>
                        <MfButton variant="ghost">Ghost</MfButton>
                        <MfButton variant="danger">Danger</MfButton>
                        <MfButton variant="link">Link</MfButton>
                        <MfButton variant="primary" size="sm">Small</MfButton>
                        <MfButton variant="primary" size="lg">Large</MfButton>
                        <MfButton variant="primary" disabled>Disabled</MfButton>
                        <MfButton variant="primary" :loading="loading" @click="simulateLoading">
                            {{ loading ? 'Saving' : 'Save' }}
                        </MfButton>
                        <MfButton variant="primary" block>Block</MfButton>
                    </MfSpace>
                </MfCard>

                <MfCard title="Data entry">
                    <div class="grid gap-4 sm:grid-cols-2">
                        <MfInput v-model="input" placeholder="Basic input" clearable>
                            <template #prefix>
                                <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                    <circle cx="11" cy="11" r="7" />
                                    <path d="M21 21l-4.35-4.35" />
                                </svg>
                            </template>
                        </MfInput>
                        <MfInput model-value="" placeholder="Invalid state" invalid />
                        <MfInput model-value="Read only value" readonly />
                        <MfSelect v-model="select" :options="selectOptions" placeholder="Engine" />
                        <MfTextarea v-model="textarea" class="sm:col-span-2" placeholder="Multiline description" :rows="3" />
                        <div class="flex flex-wrap items-center gap-4">
                            <MfCheckbox v-model="agree" label="I agree the terms" />
                            <MfCheckbox :model-value="true" indeterminate label="Indeterminate" disabled />
                        </div>
                        <MfRadioGroup v-model="engine" name="engine">
                            <MfRadio value="local" label="Local" />
                            <MfRadio value="seatunnel" label="SeaTunnel" />
                        </MfRadioGroup>
                        <div class="flex items-center gap-3">
                            <MfSwitch v-model="darkMode" size="sm" />
                            <MfSwitch v-model="darkMode" />
                            <MfSwitch :model-value="false" disabled />
                            <span class="text-sm text-content-secondary">{{ darkMode ? 'on' : 'off' }}</span>
                        </div>
                    </div>
                </MfCard>

                <MfCard title="Feedback & display">
                    <div class="grid gap-4">
                        <MfSpace direction="vertical" class="w-full">
                            <MfAlert variant="info" title="Information">
                                Connected sources are refreshed every 30 seconds.
                            </MfAlert>
                            <MfAlert variant="success">Query executed in 128 ms.</MfAlert>
                            <MfAlert variant="warning" title="Warning" closable>
                                The ClickHouse driver is experimental.
                            </MfAlert>
                            <MfAlert variant="danger" title="Error">
                                Connection refused: source mysql-prod is unreachable.
                            </MfAlert>
                        </MfSpace>
                        <MfDivider label="MORE" dashed />
                        <MfSpace wrap>
                            <MfTag>default</MfTag>
                            <MfTag variant="primary">primary</MfTag>
                            <MfTag variant="success">running</MfTag>
                            <MfTag variant="warning">deprecated</MfTag>
                            <MfTag variant="danger">stopped</MfTag>
                            <MfTag variant="info">v2026.0.0</MfTag>
                            <MfTag variant="primary" closable @close="() => {}">closable</MfTag>
                        </MfSpace>
                        <MfSpace wrap>
                            <MfBadge :count="8">
                                <MfButton>Notifications</MfButton>
                            </MfBadge>
                            <MfBadge :count="100" :max="99">
                                <MfButton>Requests</MfButton>
                            </MfBadge>
                            <MfBadge dot variant="success">
                                <MfAvatar size="md">MF</MfAvatar>
                            </MfBadge>
                            <MfAvatar>MF</MfAvatar>
                            <MfAvatar size="sm" shape="square">A</MfAvatar>
                            <MfSpinner size="sm" />
                            <MfSpinner label="Loading..." />
                            <MfTooltip content="Token driven tooltip">
                                <MfButton variant="ghost">Hover me</MfButton>
                            </MfTooltip>
                        </MfSpace>
                    </div>
                </MfCard>

                <MfCard title="品牌加载动画">
                    <div class="flex flex-col items-center gap-5 py-6 text-primary">
                        <MfLoadingIcon :size="96" :loading="brandLoading"/>
                        <span class="text-sm text-content-secondary" role="status">{{ brandLoading ? '正在加载' : '加载完成' }}</span>
                        <MfSpace wrap>
                            <MfSpinner size="xs" :loading="brandLoading"/>
                            <MfSpinner size="sm" :loading="brandLoading"/>
                            <MfSpinner :label="brandLoading ? '加载中' : '已完成'" :loading="brandLoading"/>
                            <MfSpinner size="lg" :loading="brandLoading"/>
                        </MfSpace>
                        <MfSpace wrap>
                            <MfButton variant="primary" :loading="brandLoading">{{ brandLoading ? '保存中' : '保存' }}</MfButton>
                            <MfButton @click="brandLoading = !brandLoading">{{ brandLoading ? '模拟完成' : '重新加载' }}</MfButton>
                        </MfSpace>
                    </div>
                </MfCard>

                <MfCard title="空状态提示">
                    <div class="grid gap-4 md:grid-cols-3">
                        <MfEmpty title="暂无数据" description="创建第一条数据后，就能在这里查看。">
                            <template #extra><MfButton variant="primary" @click="modalOpen = true">创建数据源</MfButton></template>
                        </MfEmpty>
                        <MfEmpty variant="search" title="未找到匹配结果" description="试试调整筛选条件或清空关键字。"/>
                        <MfEmpty variant="initial" title="等待执行查询" description="选择数据源并执行 SQL，查询结果将显示在这里。"/>
                    </div>
                </MfCard>

                <MfCard title="Overlay">
                    <div class="mb-4 overflow-x-auto rounded-mf-md border border-line p-3">
                        <div class="flex items-center justify-between gap-4">
                            <span class="text-sm text-content-secondary">表格滚动容器边缘提示</span>
                            <MfTooltip content="分配菜单 [ 系统管理员路由 ]：长提示在视口内换行，不应撑大表格滚动范围。">
                                <MfButton variant="ghost">分配菜单</MfButton>
                            </MfTooltip>
                        </div>
                    </div>
                    <MfSpace>
                        <MfButton variant="primary" @click="modalOpen = true">Open modal</MfButton>
                        <MfCheckbox v-model="engines" value="local" label="local" />
                        <MfCheckbox v-model="engines" value="seatunnel" label="seatunnel" />
                        <span class="text-sm text-content-tertiary">{{ engines.join(', ') || 'none' }}</span>
                    </MfSpace>
                </MfCard>
            </main>

            <MfModal v-model:open="modalOpen" title="Create source">
                <p class="text-content-secondary">
                    Every color you see here resolves from `--mf-*` tokens, so a
                    custom theme is a single variable swap away.
                </p>
                <template #footer>
                    <MfButton @click="modalOpen = false">Cancel</MfButton>
                    <MfButton variant="primary" @click="modalOpen = false">Confirm</MfButton>
                </template>
            </MfModal>
        </div>
    </MfConfigProvider>
</template>
