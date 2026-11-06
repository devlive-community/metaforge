<script setup lang="ts">
import { computed } from 'vue'
import { ArrowUpRight, Database, LayoutDashboard, Monitor, Plus } from '@lucide/vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import type { HomeResource, HomeResourceKind } from '@/composables/useHomeWorkspace'
import { readDashboardDocument } from '@/model/dashboard-layout'
import { useUtil } from '@/utils/common'

const props = defineProps<{ kind: HomeResourceKind; items: HomeResource[]; loading: boolean; failed: boolean }>()
const emit = defineEmits<{ retry: [] }>()
const { t } = useI18n(), router = useRouter(), { getDatasetStateText } = useUtil()
const entries = computed(() => props.items.map(item => {
  const code = encodeURIComponent(item.code)
  if (props.kind === 'datasets') {
    const state = Array.isArray(item.state) ? item.state.at(-1) || '' : ''
    return { ...item, icon: Database, link: '/dataset/detail/' + code, status: getDatasetStateText(state),
      tone: state.endsWith('FAILED') ? 'danger' as const : state === 'COMPLETE_SUCCESS' ? 'success' as const : 'default' as const }
  }
  const document = readDashboardDocument(item.configure, item.name, item.description)
  return { ...item, icon: (document.published ?? document.draft).mode === 'wall' ? Monitor : LayoutDashboard,
    link: document.published ? '/dashboard/preview/' + code : '/dashboard/info/' + code,
    status: t(document.published ? 'dashboard.studio.status.published' : 'dashboard.studio.status.draft'),
    tone: document.published ? 'success' as const : 'primary' as const }
}))
</script>

<template>
  <div class="flex min-h-64 flex-col">
    <div v-if="loading" class="flex min-h-64 flex-1 items-center justify-center"><MfSpinner/></div>
    <MfEmpty v-else-if="failed" class="min-h-64 flex-1" :title="$t('homeWorkspace.loadFailed')">
      <template #extra><MfButton size="sm" @click="emit('retry')">{{ $t('common.refresh') }}</MfButton></template>
    </MfEmpty>
    <MfEmpty v-else-if="!entries.length" class="min-h-64 flex-1" variant="initial" :title="$t('homeWorkspace.empty.' + kind)">
      <template #extra><MfButton size="sm" variant="primary" @click="router.push(kind === 'datasets' ? '/dataset/info' : '/dashboard/info')"><Plus :size="14"/>{{ $t(kind === 'datasets' ? 'dataset.common.create' : 'homeWorkspace.createVisualization') }}</MfButton></template>
    </MfEmpty>
    <MfMenu v-else class="gap-1!">
      <MfMenuItem v-for="item in entries" :key="item.code" :key_="item.code" class="px-3! py-2.5!">
        <RouterLink :to="item.link" class="flex min-w-0 items-center gap-3">
          <span class="grid size-9 shrink-0 place-items-center rounded-mf-md bg-primary/8 text-primary"><component :is="item.icon" :size="19"/></span>
          <span class="min-w-0 flex-1">
            <span class="block truncate text-sm font-semibold text-content" :title="item.name || item.code">{{ item.name || item.code }}</span>
            <span v-if="item.updateTime || item.createTime" class="mt-1 block truncate text-xs font-normal text-content-tertiary">{{ item.updateTime || item.createTime }}</span>
          </span>
          <MfTag v-if="item.status" size="sm" :variant="item.tone" class="max-w-28 truncate">{{ item.status }}</MfTag>
          <ArrowUpRight :size="15" class="shrink-0 text-content-tertiary"/>
        </RouterLink>
      </MfMenuItem>
    </MfMenu>
  </div>
</template>
