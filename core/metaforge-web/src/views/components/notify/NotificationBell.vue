<template>
  <div class="inline-flex">
    <!-- The badge sits on the bell glyph's corner, inside the 36px button's 8px padding. -->
    <MfBadge :count="store.summary.unread" :max="99" :offset="[8, 8]">
      <button ref="trigger"
              type="button"
              class="dc-bell"
              :class="{ 'dc-bell--open': open }"
              :aria-label="$t('notify.text.center')"
              :aria-expanded="open"
              @click="toggle">
        <Bell :size="20"/>
      </button>
    </MfBadge>

    <Teleport to="body">
      <Transition name="dc-bell-panel">
        <div v-if="open" ref="panel" class="dc-bell-panel" :style="position" role="dialog" :aria-label="$t('notify.text.center')">
          <div class="dc-bell-panel__head">
            <div class="dc-bell-panel__title">{{ $t('notify.text.center') }}</div>
            <MfTag v-if="store.summary.unread > 0" size="sm" variant="primary">
              {{ $t('notify.center.unreadCount', { count: store.summary.unread }) }}
            </MfTag>
            <div class="flex-1"/>
            <MfButton size="sm" variant="ghost" :disabled="store.summary.unread === 0" @click="store.markAllRead()">
              <CheckCheck :size="14"/>
              {{ $t('notify.center.markAllRead') }}
            </MfButton>
            <MfTooltip :content="$t('notify.center.settings')">
              <button type="button" class="dc-bell-panel__icon" @click="go('/account/notify')">
                <Settings :size="15"/>
              </button>
            </MfTooltip>
          </div>

          <div class="dc-bell-panel__tabs">
            <button v-for="tab in TABS"
                    :key="tab"
                    type="button"
                    class="dc-bell-panel__tab"
                    :class="{ 'dc-bell-panel__tab--active': active === tab }"
                    @click="switchTab(tab)">
              {{ $t(`notify.center.tab.${ tab }`) }}
              <span v-if="tab === 'unread' && store.summary.unread > 0" class="dc-bell-panel__count">{{ store.summary.unread }}</span>
            </button>
          </div>

          <div class="dc-bell-panel__list" @scroll="onScroll">
            <div v-if="loading && items.length === 0" class="dc-bell-panel__state"><MfSpinner/></div>
            <div v-else-if="items.length === 0" class="dc-bell-panel__state">
              <Inbox :size="28"/>
              <span>{{ active === 'unread' ? $t('notify.center.noUnread') : $t('notify.center.empty') }}</span>
            </div>
            <template v-else>
              <div v-for="group in groups" :key="group.key">
                <div class="dc-bell-panel__group">{{ $t(`notify.center.${ group.key }`) }}</div>
                <NotificationRow v-for="item in group.items"
                                 :key="item.code"
                                 compact
                                 :item="item"
                                 @open="store.open"
                                 @toggle-read="(value: NotificationItem) => store.mark(value, !value.read)"
                                 @remove="store.confirmRemove"/>
              </div>
              <div v-if="loading" class="dc-bell-panel__more"><MfSpinner size="sm"/></div>
            </template>
          </div>

          <button type="button" class="dc-bell-panel__foot" @click="go('/notifications')">
            {{ $t('notify.center.viewAll') }}
            <ChevronRight :size="14"/>
          </button>
        </div>
      </Transition>
    </Teleport>

    <NotificationDetail/>

    <MfModal :open="!!store.pendingRemoval"
             width="400px"
             :title="$t('notify.center.removeTitle')"
             @update:open="(value: boolean) => !value && (store.pendingRemoval = null)">
      <div class="text-sm text-content-secondary">{{ $t('notify.center.removeConfirm', { title: store.pendingRemoval?.title || '' }) }}</div>
      <template #footer>
        <MfButton :disabled="removing" @click="store.pendingRemoval = null">{{ $t('common.cancel') }}</MfButton>
        <MfButton variant="danger" :loading="removing" @click="remove">{{ $t('notify.center.remove') }}</MfButton>
      </template>
    </MfModal>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, CheckCheck, ChevronRight, Inbox, Settings } from '@lucide/vue'
import NotificationRow from '@/views/components/notify/NotificationRow.vue'
import NotificationDetail from '@/views/components/notify/NotificationDetail.vue'
import NotificationService from '@/services/notification'
import { useNotificationStore } from '@/stores/notifications'
import { groupByDay, NotificationItem } from '@/model/notification'

defineOptions({ name: 'NotificationBell' })

const TABS = ['all', 'unread'] as const
const PAGE_SIZE = 20

const store = useNotificationStore()
const router = useRouter()

const open = ref(false)
const trigger = ref<HTMLElement>()
const panel = ref<HTMLElement>()
const position = ref<Record<string, string>>({})
const active = ref<typeof TABS[number]>('all')
const items = ref<NotificationItem[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const removing = ref(false)

const groups = computed(() => groupByDay(items.value))

const place = () => {
  const rect = trigger.value?.getBoundingClientRect()
  if (!rect) {
    return
  }
  const width = Math.min(420, window.innerWidth - 16)
  const left = Math.max(8, Math.min(rect.right - width, window.innerWidth - width - 8))
  position.value = { top: `${ rect.bottom + 8 }px`, left: `${ left }px`, width: `${ width }px` }
}

const load = async (next = 1) => {
  loading.value = true
  try {
    const response = await NotificationService.list({ read: active.value === 'unread' ? false : undefined, page: next, size: PAGE_SIZE })
    if (response.status && response.data) {
      items.value = next === 1 ? response.data.items : [...items.value, ...response.data.items]
      page.value = next
      total.value = response.data.total
    }
  }
  finally {
    loading.value = false
  }
}

const toggle = async () => {
  open.value = !open.value
  if (open.value) {
    place()
    await Promise.all([load(), store.refresh()])
  }
}

const close = () => (open.value = false)

const switchTab = (tab: typeof TABS[number]) => {
  active.value = tab
  load()
}

const go = (path: string) => {
  close()
  router.push(path)
}

const onScroll = (event: Event) => {
  const target = event.target as HTMLElement
  if (!loading.value && items.value.length < total.value && target.scrollTop + target.clientHeight >= target.scrollHeight - 24) {
    load(page.value + 1)
  }
}

const remove = async () => {
  if (!store.pendingRemoval) {
    return
  }
  removing.value = true
  try {
    if (await store.remove(store.pendingRemoval)) {
      store.pendingRemoval = null
    }
  }
  finally {
    removing.value = false
  }
}

// Any change (read, removed, new arrivals) reloads the open list.
watch(() => store.version, () => {
  if (open.value) {
    load()
  }
})

// Opening a notification moves to its drawer.
watch(() => store.detailOpen, value => {
  if (value) {
    close()
  }
})

const onPointerDown = (event: PointerEvent) => {
  const target = event.target as Node
  if (open.value && !panel.value?.contains(target) && !trigger.value?.contains(target)) {
    close()
  }
}

const onKey = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    close()
  }
}

let stopWatching: (() => void) | undefined

onMounted(() => {
  stopWatching = store.watch()
  document.addEventListener('pointerdown', onPointerDown, true)
  document.addEventListener('keydown', onKey)
  window.addEventListener('resize', close)
  nextTick(place)
})

onBeforeUnmount(() => {
  stopWatching?.()
  document.removeEventListener('pointerdown', onPointerDown, true)
  document.removeEventListener('keydown', onKey)
  window.removeEventListener('resize', close)
})
</script>

<style scoped>
.dc-bell {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    flex-shrink: 0;
    border-radius: var(--mf-radius-md);
    color: var(--mf-color-content-secondary);
    cursor: pointer;
    transition: color var(--mf-duration-fast), background-color var(--mf-duration-fast);
}

.dc-bell:hover,
.dc-bell--open {
    color: var(--mf-color-primary);
    background-color: var(--mf-color-surface-hover);
}

.dc-bell:focus-visible {
    outline: 2px solid var(--mf-color-primary);
    outline-offset: 2px;
}

.dc-bell-panel {
    position: fixed;
    z-index: 50;
    display: flex;
    flex-direction: column;
    max-height: min(560px, calc(100vh - 96px));
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-lg);
    background: var(--dc-surface);
    box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
    overflow: hidden;
}

.dc-bell-panel__head {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 14px 16px 8px;
    white-space: nowrap;
}

.dc-bell-panel__title {
    font-size: 15px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-bell-panel__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: var(--dc-radius-md);
    color: var(--dc-text-secondary);
}

.dc-bell-panel__icon:hover {
    background: var(--dc-surface-hover);
    color: var(--dc-primary);
}

.dc-bell-panel__tabs {
    display: flex;
    gap: 4px;
    padding: 0 16px;
    border-bottom: 1px solid var(--dc-border-light);
}

.dc-bell-panel__tab {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 10px;
    margin-bottom: -1px;
    font-size: 13px;
    color: var(--dc-text-secondary);
    border-bottom: 2px solid transparent;
    white-space: nowrap;
}

.dc-bell-panel__tab--active {
    color: var(--dc-primary);
    font-weight: 600;
    border-bottom-color: var(--dc-primary);
}

.dc-bell-panel__count {
    min-width: 18px;
    padding: 0 5px;
    border-radius: 9px;
    font-size: 11px;
    line-height: 18px;
    text-align: center;
    background: var(--dc-surface-hover);
    color: var(--dc-text-secondary);
}

.dc-bell-panel__list {
    flex: 1;
    min-height: 120px;
    overflow-y: auto;
    padding: 4px 8px 8px;
}

.dc-bell-panel__group {
    padding: 10px 8px 4px;
    font-size: 12px;
    color: var(--dc-text-secondary);
}

.dc-bell-panel__state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: 200px;
    font-size: 13px;
    color: var(--dc-text-secondary);
}

.dc-bell-panel__more {
    display: flex;
    justify-content: center;
    padding: 8px;
}

.dc-bell-panel__foot {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    padding: 12px;
    font-size: 13px;
    font-weight: 600;
    color: var(--dc-primary);
    border-top: 1px solid var(--dc-border-light);
}

.dc-bell-panel__foot:hover {
    background: var(--dc-surface-hover);
}

.dc-bell-panel-enter-active,
.dc-bell-panel-leave-active {
    transition: opacity 0.15s ease, transform 0.15s ease;
}

.dc-bell-panel-enter-from,
.dc-bell-panel-leave-to {
    opacity: 0;
    transform: translateY(-4px);
}
</style>
