<template>
  <div class="dc-center">
    <div class="dc-center__head">
      <div class="min-w-0">
        <h1 class="m-0 text-xl leading-7 font-semibold text-content">{{ $t('notify.text.center') }}</h1>
        <p class="mt-1 mb-0 text-sm leading-5 text-content-secondary">{{ $t('notify.center.description') }}</p>
      </div>
      <div class="dc-center__actions">
        <MfButton @click="router.push('/account/notify')">
          <Settings :size="14"/>
          {{ $t('notify.center.settings') }}
        </MfButton>
        <MfButton :disabled="readCount === 0 || clearing" :loading="clearing" @click="clearOpen = true">
          <Trash2 :size="14"/>
          {{ $t('notify.center.removeRead') }}
        </MfButton>
        <MfButton variant="primary" :disabled="store.summary.unread === 0" @click="store.markAllRead()">
          <CheckCheck :size="14"/>
          {{ $t('notify.center.markAllRead') }}
        </MfButton>
      </div>
    </div>

    <div class="dc-center__body">
      <div class="dc-center__card dc-center__main">
        <div class="dc-center__filters">
          <div class="dc-center__segments">
            <button v-for="state in STATES"
                    :key="state"
                    type="button"
                    class="dc-center__segment"
                    :class="{ 'dc-center__segment--active': read === state }"
                    @click="read = state">
              {{ $t(`notify.center.tab.${ state }`) }}
              <template v-if="state === 'unread' && store.summary.unread > 0">({{ store.summary.unread }})</template>
            </button>
          </div>
          <div class="dc-center__search">
            <MfInput v-model="keyword" clearable :placeholder="$t('notify.center.search')">
              <template #prefix><Search :size="14"/></template>
            </MfInput>
          </div>
        </div>

        <div class="dc-center__chips">
          <button type="button" class="dc-center__chip" :class="{ 'dc-center__chip--active': !category }" @click="category = ''">
            {{ $t('notify.center.tab.all') }}
          </button>
          <button v-for="key in NOTIFICATION_CATEGORIES"
                  :key="key"
                  type="button"
                  class="dc-center__chip"
                  :class="{ 'dc-center__chip--active': category === key }"
                  @click="category = key">
            {{ $t(`notify.category.${ key.toLowerCase() }`) }}
            <span class="dc-center__chip-count">{{ store.summary.categories[key] || 0 }}</span>
          </button>
        </div>

        <div class="dc-center__list">
          <div v-if="loading && items.length === 0" class="dc-center__state"><MfSpinner size="lg"/></div>
          <MfEmpty v-else-if="items.length === 0" :description="emptyText"/>
          <template v-else>
            <div v-for="group in groups" :key="group.key">
              <div class="dc-center__group">{{ $t(`notify.center.${ group.key }`) }}</div>
              <NotificationRow v-for="item in group.items"
                               :key="item.code"
                               :item="item"
                               @open="store.open"
                               @toggle-read="(value: NotificationItem) => store.mark(value, !value.read)"
                               @remove="store.confirmRemove"/>
            </div>
            <div class="dc-center__more">
              <MfButton v-if="items.length < total" :loading="loading" @click="load(page + 1)">{{ $t('notify.center.loadMore') }}</MfButton>
              <span v-else>{{ $t('notify.center.allShown', { count: total }) }}</span>
            </div>
          </template>
        </div>
      </div>

      <div class="dc-center__card dc-center__side">
        <div class="dc-center__side-title">{{ $t('notify.center.overview') }}</div>
        <button type="button" class="dc-center__stat" @click="showAll">
          <span>{{ $t('notify.center.total') }}</span><b>{{ store.summary.total }}</b>
        </button>
        <button type="button" class="dc-center__stat" @click="showUnread">
          <span>{{ $t('notify.center.unreadTotal') }}</span><b>{{ store.summary.unread }}</b>
        </button>
        <button v-for="key in NOTIFICATION_CATEGORIES"
                :key="key"
                type="button"
                class="dc-center__stat"
                :class="{ 'dc-center__stat--active': category === key }"
                @click="category = key">
          <span>{{ $t(`notify.category.${ key.toLowerCase() }`) }}</span><b>{{ store.summary.categories[key] || 0 }}</b>
        </button>
        <div class="dc-center__tip">{{ $t('notify.center.tip') }}</div>
      </div>
    </div>

    <MfModal v-model:open="clearOpen" width="400px" :title="$t('notify.center.removeRead')">
      <div class="text-sm text-content-secondary">{{ $t('notify.center.removeReadConfirm', { count: readCount }) }}</div>
      <template #footer>
        <MfButton :disabled="clearing" @click="clearOpen = false">{{ $t('common.cancel') }}</MfButton>
        <MfButton variant="danger" :loading="clearing" @click="clearRead">{{ $t('notify.center.remove') }}</MfButton>
      </template>
    </MfModal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import { CheckCheck, Search, Settings, Trash2 } from '@lucide/vue'
import NotificationRow from '@/views/components/notify/NotificationRow.vue'
import NotificationService from '@/services/notification'
import { useNotificationStore } from '@/stores/notifications'
import { groupByDay, NOTIFICATION_CATEGORIES, NotificationItem } from '@/model/notification'

defineOptions({ name: 'NotificationCenter' })

const STATES = ['all', 'unread', 'read'] as const
const PAGE_SIZE = 20

const store = useNotificationStore()
const router = useRouter()
const { t } = useI18n()

const read = ref<typeof STATES[number]>('all')
const category = ref('')
const keyword = ref('')
const items = ref<NotificationItem[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const clearOpen = ref(false)
const clearing = ref(false)

const groups = computed(() => groupByDay(items.value))
const readCount = computed(() => store.summary.total - store.summary.unread)
const emptyText = computed(() => keyword.value.trim() ? t('notify.center.noMatch') : read.value === 'unread' ? t('notify.center.noUnread') : t('notify.center.empty'))

let request = 0

const load = async (next = 1) => {
  const current = ++request
  loading.value = true
  try {
    const response = await NotificationService.list({
      read: read.value === 'all' ? undefined : read.value === 'read',
      category: category.value || undefined,
      keyword: keyword.value.trim() || undefined,
      page: next,
      size: PAGE_SIZE
    })
    // A newer filter replaced this one while it loaded.
    if (current !== request) {
      return
    }
    if (response.status && response.data) {
      items.value = next === 1 ? response.data.items : [...items.value, ...response.data.items]
      page.value = next
      total.value = response.data.total
    }
    else {
      MfMessage.error(response.message)
    }
  }
  finally {
    if (current === request) {
      loading.value = false
    }
  }
}

const showAll = () => {
  read.value = 'all'
  category.value = ''
}

const showUnread = () => {
  read.value = 'unread'
  category.value = ''
}

const clearRead = async () => {
  clearing.value = true
  try {
    const response = await NotificationService.removeRead()
    if (response.status) {
      MfMessage.success(t('notify.center.removedRead', { count: response.data?.deleted || 0 }))
      clearOpen.value = false
      await store.changed()
    }
    else {
      MfMessage.error(response.message)
    }
  }
  finally {
    clearing.value = false
  }
}

let typing: ReturnType<typeof setTimeout> | undefined
watch(keyword, () => {
  clearTimeout(typing)
  typing = setTimeout(() => load(), 300)
})
watch([read, category], () => load())
// Reads, removals and new arrivals elsewhere reload the list.
watch(() => store.version, () => load())

onMounted(() => {
  load()
  store.refresh()
})
</script>

<style scoped>
.dc-center {
    width: 100%;
    min-width: 0;
}

.dc-center__head {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
}

.dc-center__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    white-space: nowrap;
}

.dc-center__body {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 300px;
    gap: 16px;
    align-items: start;
}

@media (max-width: 960px) {
    .dc-center__body {
        grid-template-columns: minmax(0, 1fr);
    }

    .dc-center__side {
        order: -1;
    }
}

.dc-center__card {
    background: var(--dc-surface);
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-lg);
    box-shadow: var(--dc-shadow-card);
}

.dc-center__filters {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 14px 16px;
    border-bottom: 1px solid var(--dc-border-light);
}

.dc-center__segments {
    display: inline-flex;
    gap: 4px;
    padding: 3px;
    border-radius: var(--dc-radius-md);
    background: var(--dc-surface-hover);
}

.dc-center__segment {
    padding: 5px 12px;
    font-size: 13px;
    border-radius: var(--dc-radius-sm, 4px);
    color: var(--dc-text-secondary);
    white-space: nowrap;
}

.dc-center__segment--active {
    background: var(--dc-surface);
    color: var(--dc-primary);
    font-weight: 600;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}

.dc-center__search {
    width: 260px;
    max-width: 100%;
}

.dc-center__chips {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding: 12px 16px;
    border-bottom: 1px solid var(--dc-border-light);
}

.dc-center__chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 4px 12px;
    font-size: 12px;
    border: 1px solid var(--dc-border-light);
    border-radius: 999px;
    color: var(--dc-text-secondary);
    white-space: nowrap;
}

.dc-center__chip:hover {
    color: var(--dc-primary);
}

.dc-center__chip--active {
    border-color: var(--dc-primary);
    background: var(--dc-primary);
    color: #fff;
}

.dc-center__chip--active:hover {
    color: #fff;
}

.dc-center__chip-count {
    font-size: 11px;
    opacity: 0.75;
}

.dc-center__list {
    padding: 8px 12px 12px;
    min-height: 320px;
}

.dc-center__state {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 280px;
}

.dc-center__group {
    padding: 12px 12px 6px;
    font-size: 12px;
    color: var(--dc-text-secondary);
}

.dc-center__more {
    display: flex;
    justify-content: center;
    padding: 12px;
    font-size: 12px;
    color: var(--dc-text-secondary);
}

.dc-center__side {
    padding: 16px;
}

.dc-center__side-title {
    margin-bottom: 8px;
    font-size: 15px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-center__stat {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding: 10px 8px;
    font-size: 13px;
    color: var(--dc-text-secondary);
    border-bottom: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-sm, 4px);
    text-align: left;
}

.dc-center__stat:hover,
.dc-center__stat--active {
    color: var(--dc-primary);
    background: var(--dc-surface-hover);
}

.dc-center__stat b {
    color: var(--dc-text-title);
    font-weight: 600;
}

.dc-center__tip {
    margin-top: 16px;
    padding: 12px;
    font-size: 12px;
    line-height: 20px;
    border-radius: var(--dc-radius-md);
    background: var(--dc-primary-light);
    color: var(--dc-primary);
}
</style>
