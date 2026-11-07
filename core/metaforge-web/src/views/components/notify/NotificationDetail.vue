<template>
  <MfDrawer v-model:open="store.detailOpen" width="min(440px, 100vw)" :title="$t('notify.center.detail')">
    <div v-if="item" class="dc-notice-detail">
      <NotificationIcon :item="item" :size="48"/>
      <div class="dc-notice-detail__title">{{ item.title || sceneLabel }}</div>
      <div class="dc-notice-detail__meta">
        <MfTag size="sm" variant="primary">{{ categoryLabel }}</MfTag>
        <span class="dc-notice-detail__time"><Clock :size="12"/>{{ item.createTime }}</span>
        <span>{{ item.read ? $t('notify.center.read') : $t('notify.center.unread') }}</span>
      </div>
      <div class="dc-notice-detail__content">{{ item.content }}</div>

      <div v-if="item.entityName" class="dc-notice-detail__entity">
        <div class="min-w-0">
          <div class="dc-notice-detail__entity-label">{{ sceneLabel }}</div>
          <div class="dc-notice-detail__entity-name">{{ item.entityName }}</div>
        </div>
        <MfButton v-if="link" size="sm" @click="go">
          <ExternalLink :size="14"/>
          {{ $t('notify.center.view') }}
        </MfButton>
        <span v-else class="dc-notice-detail__gone">{{ $t('notify.center.entityGone') }}</span>
      </div>
    </div>

    <template #footer>
      <div v-if="item" class="dc-notice-detail__foot">
        <MfButton variant="outline" class="dc-notice-detail__remove" @click="store.confirmRemove(item)">{{ $t('notify.center.remove') }}</MfButton>
        <div class="flex gap-2">
          <MfButton @click="store.detailOpen = false">{{ $t('notify.center.back') }}</MfButton>
          <MfButton variant="primary" @click="store.mark(item, !item.read)">
            {{ item.read ? $t('notify.center.markUnread') : $t('notify.center.markRead') }}
          </MfButton>
        </div>
      </div>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { Clock, ExternalLink } from '@lucide/vue'
import NotificationIcon from '@/views/components/notify/NotificationIcon.vue'
import { useNotificationStore } from '@/stores/notifications'
import { linkOf, sceneKey } from '@/model/notification'

defineOptions({ name: 'NotificationDetail' })

const store = useNotificationStore()
const router = useRouter()
const { t, te } = useI18n()

const item = computed(() => store.detail)
const link = computed(() => item.value ? linkOf(item.value) : '')
const sceneLabel = computed(() => {
  const key = `notify.scene.${ sceneKey(item.value?.scene || '') }`
  return te(key) ? t(key) : item.value?.scene || ''
})
const categoryLabel = computed(() => t(`notify.category.${ (item.value?.category || 'SYSTEM').toLowerCase() }`))

const go = () => {
  store.detailOpen = false
  router.push(link.value)
}
</script>

<style scoped>
.dc-notice-detail {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
}

.dc-notice-detail__title {
    font-size: 18px;
    font-weight: 600;
    color: var(--dc-text-title);
    word-break: break-word;
}

.dc-notice-detail__meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
    font-size: 12px;
    color: var(--dc-text-secondary);
}

.dc-notice-detail__time {
    display: inline-flex;
    align-items: center;
    gap: 4px;
}

.dc-notice-detail__content {
    font-size: 13px;
    line-height: 22px;
    color: var(--dc-text-title);
    white-space: pre-wrap;
    word-break: break-word;
}

.dc-notice-detail__entity {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    width: 100%;
    padding: 12px;
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-md);
    background: var(--dc-surface-hover);
}

.dc-notice-detail__entity :deep(button) {
    white-space: nowrap;
    flex-shrink: 0;
}

.dc-notice-detail__entity-label {
    font-size: 12px;
    color: var(--dc-text-secondary);
}

.dc-notice-detail__entity-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--dc-text-title);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.dc-notice-detail__gone {
    font-size: 12px;
    color: var(--dc-text-secondary);
    white-space: nowrap;
}

.dc-notice-detail__remove {
    color: var(--dc-danger);
    border-color: var(--dc-danger-border);
}

.dc-notice-detail__foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    width: 100%;
    white-space: nowrap;
}
</style>
