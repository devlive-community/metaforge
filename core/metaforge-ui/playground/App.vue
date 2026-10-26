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
    MfInput,
    MfModal,
    MfRadio,
    MfRadioGroup,
    MfSelect,
    MfSpace,
    MfSpinner,
    MfSwitch,
    MfTag,
    MfTextarea,
    MfTooltip,
} from '../src/index'

const mode = ref<MfThemeMode>('light')
const primary = ref('#2563eb')

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

                <MfCard title="Overlay">
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
