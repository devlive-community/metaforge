<template>
  <section class="flex min-h-[70vh] flex-col gap-4">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-xl font-bold">{{ $t('dashboard.common.list') }}</h1>
      <MfButton variant="primary" @click="router.push(SCREEN_NEW_PATH)"><Plus :size="16"/>{{ $t('dashboard.common.create') }}</MfButton>
    </div>

    <div v-if="loading" class="flex min-h-60 flex-1 items-center justify-center"><MfSpinner size="lg"/></div>
    <div v-else-if="failed" class="flex min-h-60 flex-1 items-center justify-center">
      <MfAlert variant="danger" :title="$t('dashboard.studio.loadFailed')"><MfButton size="sm" @click="load">{{ $t('common.refresh') }}</MfButton></MfAlert>
    </div>
    <MfEmpty v-else-if="!data.length" class="min-h-60 flex-1" :title="$t('dashboard.studio.noDashboards')"/>
    <template v-else>
      <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4">
        <article v-for="item in dashboards" :key="item.code" class="flex min-w-0 flex-col rounded-mf-lg border border-line bg-surface">
          <RouterLink :to="item.link" class="relative flex aspect-[2/1] items-center justify-center overflow-hidden rounded-t-mf-lg border-b border-line"
                      :class="item.mode === 'wall' ? 'bg-[#071b2d] text-[#15e5d1]' : 'bg-primary/6 text-primary'">
            <img v-if="item.avatar?.path" :src="item.avatar.path" :alt="item.name" class="size-full object-cover"/>
            <Monitor v-else-if="item.mode === 'wall'" :size="40"/>
            <LayoutDashboard v-else :size="40"/>
          </RouterLink>
          <div class="flex flex-1 flex-col gap-2 p-3">
            <div class="flex min-w-0 items-center gap-2">
              <RouterLink :to="item.link" class="min-w-0 flex-1 truncate font-semibold hover:text-primary">{{ item.name }}</RouterLink>
              <MfTag v-if="item.code === userStore.homePage" size="sm" variant="primary"><House :size="12"/>{{ $t('dashboard.home.tag') }}</MfTag>
              <MfTag size="sm" :variant="item.published ? 'success' : 'primary'">{{ $t(item.published ? 'dashboard.studio.status.published' : 'dashboard.studio.status.draft') }}</MfTag>
            </div>
            <p v-if="item.description" class="line-clamp-2 text-xs text-content-secondary">{{ item.description }}</p>
            <div class="mt-auto flex items-center justify-between gap-2">
              <span class="min-w-0 truncate text-xs text-content-tertiary">{{ item.updateTime || item.createTime }}</span>
              <div class="flex shrink-0 items-center gap-1">
                <MfTooltip v-if="item.published" :content="$t(item.code === userStore.homePage ? 'dashboard.home.unset' : 'dashboard.home.set')">
                  <MfButton variant="ghost" size="sm" :class="item.code === userStore.homePage && 'text-primary'" :loading="homeBusy === item.code"
                            :aria-label="$t(item.code === userStore.homePage ? 'dashboard.home.unset' : 'dashboard.home.set')" @click="toggleHome(item.code)"><House :size="15"/></MfButton>
                </MfTooltip>
                <MfButton variant="ghost" size="sm" :aria-label="$t('dashboard.common.modify')" @click="router.push(item.editor)"><Pencil :size="15"/></MfButton>
                <MfButton variant="ghost" size="sm" :aria-label="$t('dashboard.common.delete')" class="text-danger" @click="deleteItem = item"><Trash2 :size="15"/></MfButton>
              </div>
            </div>
          </div>
        </article>
      </div>
      <MfPagination class="mt-auto justify-end" :current="page" :page-size="size" :total="total" show-size :page-sizes="[12, 24, 36]"
                    @change="changePage"/>
    </template>
  </section>

  <DashboardDelete v-if="deleteItem" :is-visible="true" :data="deleteItem" @close="deleteItem = undefined; load()"/>
</template>

<script setup lang="ts">
import { SCREEN_NEW_PATH, screenDesignPath, screenViewPath } from '@/model/screen-route'
import { computed, onScopeDispose, ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { House, LayoutDashboard, Monitor, Pencil, Plus, Trash2 } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import DashboardService from '@/services/dashboard'
import type { DashboardModel } from '@/model/dashboard'
import { readDashboardDocument } from '@/model/dashboard-layout'
import { FilterModel } from '@/model/filter'
import DashboardDelete from './DashboardDelete.vue'

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()
const homeBusy = ref('')
void userStore.resolveHomePage()
/** Makes a published data screen the home page, or restores the default home when it already is. */
async function toggleHome(code?: string) {
  if (!code || homeBusy.value) return
  homeBusy.value = code
  const unset = code === userStore.homePage
  try {
    await userStore.setHomePage(unset ? null : code)
    MfMessage.success(t(unset ? 'dashboard.home.cleared' : 'dashboard.home.saved'))
  } catch (error) { MfMessage.error(error instanceof Error ? error.message : t('dashboard.home.failed')) }
  finally { homeBusy.value = '' }
}
const loading = ref(false)
const failed = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const data = shallowRef<DashboardModel[]>([])
const deleteItem = shallowRef<DashboardModel>()
const dashboards = computed(() => data.value.map(item => {
  const document = readDashboardDocument(item.configure, item.name, item.description)
  const published = !!document.published
  const editor = screenDesignPath(item.code)
  return { ...item, published, mode: (document.published ?? document.draft).mode, editor,
    link: published ? screenViewPath(item.code || '') : editor }
}))
let request = 0
async function load() {
  const id = ++request
  loading.value = true
  failed.value = false
  try {
    const response = await DashboardService.getAll({ ...new FilterModel(), page: page.value, size: size.value })
    if (id !== request) return
    if (!response.status || !Array.isArray(response.data?.content)) throw new Error('Unable to load dashboards')
    data.value = response.data.content
    total.value = response.data.total
    if (!data.value.length && page.value > 1 && total.value > 0) {
      page.value = Math.max(1, Math.ceil(total.value / size.value))
      await load()
    }
  }
  catch { if (id === request) failed.value = true }
  finally { if (id === request) loading.value = false }
}
function changePage(current: number, pageSize: number) {
  page.value = pageSize === size.value ? current : 1
  size.value = pageSize
  void load()
}
void load()
onScopeDispose(() => { request++ })
</script>
