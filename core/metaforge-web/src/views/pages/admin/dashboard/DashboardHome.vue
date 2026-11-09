<script setup lang="ts">
import { computed, ref, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Clock, Grid2X2, House, List, Plus, RefreshCw, Search } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { useScreenLibrary } from '@/composables/useScreenLibrary'
import { SCREEN_NEW_PATH } from '@/model/screen-route'
import { screenEntry } from '@/model/screen-entry'
import type { DashboardModel } from '@/model/dashboard'
import DashboardDelete from './DashboardDelete.vue'
import DashboardShare from './DashboardShare.vue'
import ScreenThumbnail from './components/ScreenThumbnail.vue'
import ScreenActions from './components/ScreenActions.vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'

const router = useRouter(), route = useRoute(), { t } = useI18n(), userStore = useUserStore()
const view = computed(() => route.query.tab === 'shared' ? 'shared' : 'mine')
const { data, loading, failed, page, size, total, keyword, sort, layout, load, changePage } = useScreenLibrary(view)
const dashboards = computed(() => data.value.map(screenEntry))
const shareItem = shallowRef<DashboardModel>(), deleteItem = shallowRef<DashboardModel>(), homeBusy = ref('')
const sortOptions = computed(() => ['updated', 'created', 'name'].map(value => ({ value, label: t('screenLibrary.sort.' + value) })))
function selectView(value: string | number) { void router.replace({ path: route.path, query: { ...route.query, tab: value === 'shared' ? 'shared' : undefined } }) }
void userStore.resolveHomePage()
async function toggleHome(code?: string) {
  if (!code || homeBusy.value) return
  homeBusy.value = code
  const unset = code === userStore.homePage
  try { await userStore.setHomePage(unset ? null : code); MfMessage.success(t(unset ? 'dashboard.home.cleared' : 'dashboard.home.saved')) }
  catch (error) { MfMessage.error(error instanceof Error ? error.message : t('dashboard.home.failed')) }
  finally { homeBusy.value = '' }
}
const columns = computed(() => [
  { key: 'preview', title: '', width: 144 },
  { key: 'name', title: t('common.name'), ellipsis: true },
  { key: 'state', title: t('common.state'), width: 110 },
  { key: 'updated', title: t('common.updateTime'), width: 180 },
  { key: 'actions', title: t('common.action'), width: 220 },
])
</script>

<template>
  <section class="flex h-full min-h-0 min-w-0 flex-col gap-3 px-2 py-3 text-content sm:px-4">
    <header class="flex shrink-0 flex-wrap items-center justify-between gap-3">
      <div class="flex min-w-0 items-center gap-3"><h1 class="text-2xl font-semibold">{{ $t('dashboard.common.list') }}</h1><span v-if="!loading && !failed" class="text-xs text-content-tertiary">{{ $t('screenLibrary.total', { count: total }) }}</span></div>
      <MfButton variant="primary" @click="router.push(SCREEN_NEW_PATH)"><Plus :size="16"/>{{ $t('screenLibrary.create') }}</MfButton>
    </header>
    <MfRadioGroup :model-value="view" variant="button" class="self-start" :aria-label="$t('dashboard.common.list')" @update:model-value="selectView"><MfRadio value="mine" :label="$t('dashboard.share.mine')"/><MfRadio value="shared" :label="$t('dashboard.share.sharedWithMe')"/></MfRadioGroup>
    <div class="flex shrink-0 flex-wrap items-center justify-between gap-3 border-t border-line pt-3">
      <label class="block w-full sm:w-72"><span class="sr-only">{{ $t('screenLibrary.search') }}</span><MfInput v-model="keyword" class="w-full" :maxlength="200" :placeholder="$t('screenLibrary.search')" clearable><template #prefix><Search :size="15"/></template></MfInput></label>
      <div class="ml-auto flex max-w-full items-center gap-2">
        <MfSelect v-model="sort" class="w-36" :options="sortOptions" :aria-label="$t('common.sort')"/>
        <MfButton :disabled="loading" :aria-label="$t('common.refresh')" @click="load"><RefreshCw :size="15"/></MfButton>
        <div class="flex gap-1 rounded-mf-md border border-line bg-surface p-1"><MfButton v-for="mode in (['grid','list'] as const)" :key="mode" size="sm" :variant="layout === mode ? 'primary' : 'ghost'" :aria-label="$t('screenLibrary.' + mode)" :aria-pressed="layout === mode" @click="layout = mode"><Grid2X2 v-if="mode === 'grid'" :size="15"/><List v-else :size="15"/></MfButton></div>
      </div>
    </div>
    <div class="min-h-0 flex-1 overflow-y-auto p-1">
      <div v-if="loading" class="flex h-full min-h-60 items-center justify-center"><MfSpinner size="lg"/></div>
      <MfEmpty v-else-if="failed" class="h-full min-h-60" :title="$t('dashboard.studio.loadFailed')"><template #extra><MfButton @click="load">{{ $t('common.refresh') }}</MfButton></template></MfEmpty>
      <MfEmpty v-else-if="!dashboards.length" class="h-full min-h-60" :title="$t(keyword ? 'screenLibrary.noMatch' : view === 'shared' ? 'dashboard.share.noShared' : 'dashboard.studio.noDashboards')"><template v-if="view === 'mine' && !keyword" #extra><MfButton variant="primary" @click="router.push(SCREEN_NEW_PATH)"><Plus :size="15"/>{{ $t('screenLibrary.create') }}</MfButton></template></MfEmpty>
      <div v-else-if="layout === 'grid'" class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <article v-for="item in dashboards" :key="item.code" class="group flex min-w-0 flex-col overflow-hidden rounded-mf-lg border border-line bg-surface shadow-mf-sm transition-shadow hover:shadow-mf-md">
          <RouterLink :to="item.link" class="relative block aspect-[2/1] overflow-hidden border-b border-line" :aria-label="item.name">
            <ScreenThumbnail :screen="item"/>
            <span v-if="item.code === userStore.homePage" class="absolute left-3 top-3 inline-flex items-center gap-1 rounded-mf-md bg-primary px-2 py-1 text-xs text-on-primary shadow-mf-sm"><House :size="12"/>{{ $t('dashboard.home.tag') }}</span>
          </RouterLink>
          <div class="flex flex-1 flex-col gap-2 px-3 pb-2 pt-3">
            <div class="flex min-w-0 items-center gap-2"><RouterLink :to="item.link" class="min-w-0 flex-1 truncate font-semibold text-content hover:text-primary" :title="item.name">{{ item.name }}</RouterLink><MfTag size="sm" :variant="item.published ? 'success' : 'warning'">{{ $t(item.published ? 'dashboard.studio.status.published' : 'dashboard.studio.status.draft') }}</MfTag></div>
            <div v-if="!item.own" class="flex min-w-0 items-center gap-2 text-xs text-content-tertiary"><span class="truncate">{{ $t('dashboard.share.from', { owner: item.ownerName || '--' }) }}</span><MfTag size="sm">{{ $t('dashboard.share.permission.' + item.permission) }}</MfTag></div>
            <div class="mt-auto flex min-w-0 flex-wrap items-center justify-between gap-1 border-t border-line/60 pt-1.5">
              <span class="flex min-w-0 items-center gap-1 text-[11px] text-content-tertiary"><Clock :size="12" class="shrink-0"/><span class="truncate">{{ item.updateTime || item.createTime || '--' }}</span></span>
              <ScreenActions :screen="item" :home-page="userStore.homePage" :home-busy="homeBusy" @home="toggleHome(item.code)" @share="shareItem = item" @remove="deleteItem = item"/>
            </div>
          </div>
        </article>
      </div>
      <MfTable v-else :columns="columns" :data-source="dashboards" row-key="code">
        <template #cell-preview="{ record }"><RouterLink :to="record.link" class="block aspect-[2/1] w-28 overflow-hidden rounded-mf-md"><ScreenThumbnail :screen="record"/></RouterLink></template>
        <template #cell-name="{ record }"><RouterLink :to="record.link" class="block text-sm font-semibold text-content hover:text-primary"><EllipsisCell :text="record.name"/></RouterLink><span v-if="record.code === userStore.homePage" class="text-xs text-primary">{{ $t('dashboard.home.tag') }}</span></template>
        <template #cell-state="{ record }"><MfTag size="sm" :variant="record.published ? 'success' : 'warning'">{{ $t(record.published ? 'dashboard.studio.status.published' : 'dashboard.studio.status.draft') }}</MfTag></template>
        <template #cell-updated="{ record }">{{ record.updateTime || record.createTime || '--' }}</template>
        <template #cell-actions="{ record }"><ScreenActions :screen="record" :home-page="userStore.homePage" :home-busy="homeBusy" @home="toggleHome(record.code)" @share="shareItem = record" @remove="deleteItem = record"/></template>
      </MfTable>
    </div>
    <MfPagination v-if="view === 'mine' && total > 0 && !failed" class="shrink-0 justify-end" :current="page" :page-size="size" :total="total" show-size :page-sizes="[6,12,24]" @change="changePage"/>
    <DashboardDelete v-if="deleteItem" :is-visible="true" :data="deleteItem" @close="deleteItem = undefined; load()"/>
    <DashboardShare v-if="shareItem" :dashboard="shareItem" @close="shareItem = undefined"/>
  </section>
</template>
