<script setup lang="ts">
import { reactive } from 'vue'

export interface MfUploadFile {
    uid: string
    name: string
    status?: 'uploading' | 'done' | 'error'
    response?: unknown
}

const props = withDefaults(defineProps<{
    action?: string
    name?: string
    multiple?: boolean
    accept?: string
    data?: Record<string, unknown>
    headers?: Record<string, string>
    fileList?: MfUploadFile[]
}>(), {
    action: '',
    name: 'file',
    multiple: false,
    accept: '',
    data: () => ({}),
    headers: () => ({}),
    fileList: () => [],
})

const emit = defineEmits<{
    'update:fileList': [files: MfUploadFile[]]
    change: [info: { file: MfUploadFile, fileList: MfUploadFile[] }]
}>()

const local = reactive<MfUploadFile[]>([])
const files = () => props.fileList.length || local.length ? props.fileList.length ? props.fileList : local : local

function setFiles(next: MfUploadFile[]): void {
    local.splice(0, local.length, ...next)
    emit('update:fileList', next)
    emit('change', { file: next[next.length - 1], fileList: next })
}

function upload(event: Event): void {
    const input = event.target as HTMLInputElement
    const picked = Array.from(input.files ?? [])
    input.value = ''

    const current = [...files()]
    for (const file of picked) {
        const uid = `${Date.now()}-${Math.random().toString(36).slice(2)}`
        const entry: MfUploadFile = { uid, name: file.name, status: 'uploading' }
        current.push(entry)
        setFiles([...current])

        const body = new FormData()
        body.append(props.name, file)
        for (const [key, value] of Object.entries(props.data)) {
            body.append(key, String(value))
        }

        fetch(props.action, { method: 'POST', body, headers: props.headers })
            .then(async response => ({ ok: response.ok, json: await response.json().catch(() => null) }))
            .then(({ ok, json }) => {
                entry.status = ok ? 'done' : 'error'
                entry.response = json
                setFiles([...current])
            })
            .catch(() => {
                entry.status = 'error'
                setFiles([...current])
            })
    }
}
</script>

<template>
    <div class="inline-flex flex-col gap-2">
        <label class="inline-flex cursor-pointer items-center justify-center gap-2 rounded-mf-md border border-dashed border-line-strong px-4 py-2 text-sm text-content-secondary transition-[color,border-color] duration-(--mf-duration-base) hover:border-primary hover:text-primary">
            <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M12 16V4m0 0L8 8m4-4l4 4" stroke-linecap="round" />
                <path d="M4 16v2a2 2 0 002 2h12a2 2 0 002-2v-2" stroke-linecap="round" />
            </svg>
            <slot>Click to upload</slot>
            <input type="file" class="sr-only" :multiple="multiple" :accept="accept" @change="upload">
        </label>
        <ul v-if="files().length" class="flex flex-col gap-1 text-xs">
            <li
                v-for="file in files()"
                :key="file.uid"
                class="flex items-center gap-2 text-content-secondary"
                :class="file.status === 'error' && 'text-danger'"
            >
                <span class="size-1.5 rounded-mf-full bg-primary" :class="file.status === 'error' && 'bg-danger!'" />
                <span class="truncate">{{ file.name }}</span>
                <span v-if="file.status === 'uploading'" class="text-content-tertiary">uploading…</span>
            </li>
        </ul>
    </div>
</template>
