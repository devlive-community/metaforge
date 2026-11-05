<template>
  <div class="text-sm">
    <div class="flex items-center gap-1 min-w-0">
      <span class="font-medium text-content shrink-0">{{ sceneLabel }}</span>
      <router-link v-if="link"
                   :to="link"
                   class="truncate text-content-secondary hover:text-primary"
                   @click.stop>
        [ {{ item.entityName }} ]
      </router-link>
      <span v-else-if="item.entityName" class="truncate text-content-secondary">[ {{ item.entityName }} ]</span>
    </div>
    <div v-if="item.content" class="mt-1 text-xs text-content-secondary break-words">{{ item.content }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'NotificationItem' })

const props = defineProps<{
  item: {
    type?: string
    name?: string
    content?: string
    entityType?: string
    entityCode?: string
    entityName?: string
    entityExists?: boolean
  }
}>()

const { t, te } = useI18n()

// Notifications saved before scenes existed carry an operation (CREATED, SYNCDATA, ...) as their type.
const LEGACY: Record<string, string> = {
  CREATED: 'DATASET_CREATED',
  UPDATED: 'DATASET_UPDATED',
  DELETED: 'DATASET_DELETED',
  SYNCDATA: 'DATASET_SYNC_SUCCEEDED'
}

const sceneLabel = computed(() => {
  const type = props.item.type || ''
  const scene = LEGACY[type] || type
  if (te(`notify.scene.${ scene }`)) {
    return t(`notify.scene.${ scene }`)
  }
  return props.item.name || type
})

const ROUTES: Record<string, (code: string) => string> = {
  DATASET: code => `/dataset/info/${ code }`,
  SOURCE: code => `/source/${ code }`,
  DASHBOARD: code => `/dashboard/preview/${ code }`,
  WORKFLOW: code => `/workflow/monitor/${ code }`,
  REPORT: () => '/report'
}

const link = computed(() => {
  const route = ROUTES[props.item.entityType || '']
  return props.item.entityExists && props.item.entityCode && route ? route(props.item.entityCode) : ''
})
</script>
