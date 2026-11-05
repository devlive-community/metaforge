<template>
  <div class="dc-notice-row"
       :class="{ 'dc-notice-row--unread': !item.read, 'dc-notice-row--compact': compact }"
       role="button"
       tabindex="0"
       @click="emit('open', item)"
       @keydown.enter="emit('open', item)">
    <NotificationIcon :item="item" :size="compact ? 32 : 36"/>
    <div class="dc-notice-row__body">
      <div class="dc-notice-row__title">
        <span class="truncate">{{ item.title || sceneLabel }}</span>
        <span v-if="!item.read" class="dc-notice-row__dot" :aria-label="$t('notify.center.unread')"/>
      </div>
      <div class="dc-notice-row__content">{{ item.content }}</div>
      <div class="dc-notice-row__time">{{ item.createTime }}</div>
    </div>
    <div class="dc-notice-row__actions" @click.stop>
      <MfTooltip :content="item.read ? $t('notify.center.markUnread') : $t('notify.center.markRead')">
        <button type="button" class="dc-notice-row__action" @click="emit('toggle-read', item)">
          <Mail v-if="item.read" :size="15"/>
          <MailOpen v-else :size="15"/>
        </button>
      </MfTooltip>
      <MfTooltip :content="$t('notify.center.remove')">
        <button type="button" class="dc-notice-row__action dc-notice-row__action--danger" @click="emit('remove', item)">
          <Trash2 :size="15"/>
        </button>
      </MfTooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Mail, MailOpen, Trash2 } from '@lucide/vue'
import NotificationIcon from '@/views/components/notify/NotificationIcon.vue'
import { NotificationItem, sceneKey } from '@/model/notification'

defineOptions({ name: 'NotificationRow' })

const props = withDefaults(defineProps<{ item: NotificationItem, compact?: boolean }>(), { compact: false })
const emit = defineEmits<{ open: [item: NotificationItem], 'toggle-read': [item: NotificationItem], remove: [item: NotificationItem] }>()

const { t, te } = useI18n()
const sceneLabel = computed(() => {
  const key = `notify.scene.${ sceneKey(props.item.scene) }`
  return te(key) ? t(key) : props.item.scene
})
</script>

<style scoped>
.dc-notice-row {
    position: relative;
    display: flex;
    gap: 12px;
    padding: 12px;
    border-radius: var(--dc-radius-md);
    cursor: pointer;
    outline: none;
    transition: background-color 0.15s ease;
}

.dc-notice-row:hover,
.dc-notice-row:focus-visible {
    background: var(--dc-surface-hover);
}

.dc-notice-row--unread {
    background: var(--dc-primary-surface, var(--dc-primary-light));
}

.dc-notice-row--compact {
    padding: 10px;
}

.dc-notice-row__body {
    flex: 1;
    min-width: 0;
}

.dc-notice-row__title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-notice-row__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--dc-danger);
    flex-shrink: 0;
}

.dc-notice-row__content {
    margin-top: 2px;
    font-size: 12px;
    line-height: 18px;
    color: var(--dc-text-secondary);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    word-break: break-word;
}

.dc-notice-row__time {
    margin-top: 4px;
    font-size: 11px;
    color: var(--dc-text-tertiary, var(--dc-text-secondary));
}

/* Row actions appear on hover or keyboard focus, so the list stays calm. */
.dc-notice-row__actions {
    position: absolute;
    top: 8px;
    right: 8px;
    display: none;
    gap: 2px;
    padding: 2px;
    border-radius: var(--dc-radius-md);
    background: var(--dc-surface);
    box-shadow: var(--dc-shadow-card);
}

.dc-notice-row:hover .dc-notice-row__actions,
.dc-notice-row:focus-within .dc-notice-row__actions {
    display: flex;
}

.dc-notice-row__action {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 26px;
    height: 26px;
    border-radius: var(--dc-radius-sm, 4px);
    color: var(--dc-text-secondary);
}

.dc-notice-row__action:hover {
    background: var(--dc-surface-hover);
    color: var(--dc-primary);
}

.dc-notice-row__action--danger:hover {
    color: var(--dc-danger);
}
</style>
