<template>
  <div class="dc-profile-sidebar">
    <div class="dc-profile-sidebar__head">
      <div class="dc-profile-sidebar__badge">
        <Settings :size="18" />
      </div>
      <div class="min-w-0">
        <div class="dc-profile-sidebar__title">{{ $t('navigation.accountSettings') }}</div>
        <div class="dc-profile-sidebar__subtitle">{{ $t('user.tip.settings') }}</div>
      </div>
    </div>

    <MfMenu mode="inline"
            class="dc-profile-sidebar__menu"
            :selected-keys="[route.path]">
      <MfMenuItem v-for="item in items"
                  :key="item.href"
                  :key_="item.href">
        <router-link :to="item.href" class="dc-profile-sidebar__link">
          <component :is="item.icon" class="dc-profile-sidebar__link-icon" :size="15"/>
          <span>{{ item.title }}</span>
        </router-link>
      </MfMenuItem>
    </MfMenu>

    <div class="dc-profile-sidebar__promo">
      <div class="dc-profile-sidebar__promo-art">
        <Database :size="26" :style="{ color: '#fff' }"/>
      </div>
      <div class="dc-profile-sidebar__promo-title">{{ $t('user.tip.promoTitle') }}</div>
      <div class="dc-profile-sidebar__promo-desc">{{ $t('user.tip.promoDesc') }}</div>
      <div class="dc-profile-sidebar__promo-arrow">
        <ArrowRight :size="12" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { ArrowRight, Bell, Bot, CodeXml, Database, IdCard, Lock, Settings, SquareUser, Trophy, User } from '@lucide/vue'

defineOptions({ name: 'LayoutSidebar' })

const { t } = useI18n()
const route = useRoute()

const items = computed(() => [
  { title: t('user.common.profile'), href: '/account/profile', icon: User },
  { title: t('user.common.notify'), href: '/account/notify', icon: Bell },
  { title: t('user.common.info'), href: '/account/info', icon: Trophy },
  { title: t('user.common.log'), href: '/account/log', icon: SquareUser },
  { title: t('user.common.editor'), href: '/account/editor', icon: CodeXml },
  { title: t('user.common.assistant'), href: '/account/assistant', icon: Bot },
  { title: t('user.common.modifyPassword'), href: '/account/password', icon: Lock },
  { title: t('user.common.modifyUsername'), href: '/account/username', icon: IdCard }
])
</script>

<style scoped>
.dc-profile-sidebar {
    display: flex;
    flex-direction: column;
    width: 264px;
    background: var(--dc-sidebar);
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-lg);
    box-shadow: var(--dc-shadow-card);
    padding: 16px 12px;
    min-height: calc(100vh - 220px);
}

.dc-profile-sidebar__head {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 4px 8px 14px;
}

.dc-profile-sidebar__badge {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 38px;
    height: 38px;
    border-radius: var(--dc-radius-md);
    background: var(--dc-primary-light);
    color: var(--dc-primary);
    flex-shrink: 0;
}

.dc-profile-sidebar__title {
    font-size: 15px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-profile-sidebar__subtitle {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 2px;
}

.dc-profile-sidebar__menu {
    flex: 1;
    border-inline-end: none !important;
    background: transparent;
}

/* 菜单状态色对齐 design/ui-color-system.md 第 7 节（MfMenuItem 渲染为 li[role=menuitem]） */
.dc-profile-sidebar :deep(li[role='menuitem']) {
    color: var(--dc-menu-text);
}

.dc-profile-sidebar :deep(li[role='menuitem']:hover) {
    background: var(--dc-menu-hover) !important;
    color: var(--dc-primary-700);
}

.dc-profile-sidebar :deep(li[role='menuitem'][aria-selected='true']) {
    background: var(--dc-primary-light) !important;
    color: var(--dc-primary-700) !important;
    box-shadow: inset 3px 0 0 var(--dc-primary);
}

.dc-profile-sidebar__link {
    display: flex;
    align-items: center;
    gap: 8px;
}

.dc-profile-sidebar__link-icon {
    font-size: 15px;
}

.dc-profile-sidebar__promo {
    position: relative;
    margin: 12px 4px 4px;
    padding: 16px 14px 14px;
    border-radius: var(--dc-radius-lg);
    background: var(--dc-primary-surface);
    border: 1px solid var(--dc-border-light);
    overflow: hidden;
}

.dc-profile-sidebar__promo-art {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border-radius: var(--dc-radius-md);
    background: var(--dc-gradient-promo);
    margin-bottom: 10px;
}

.dc-profile-sidebar__promo-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-profile-sidebar__promo-desc {
    font-size: 11px;
    color: var(--dc-text-secondary);
    margin-top: 4px;
    padding-right: 24px;
    line-height: 1.5;
}

.dc-profile-sidebar__promo-arrow {
    position: absolute;
    right: 12px;
    bottom: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: var(--dc-primary-light);
    color: var(--dc-primary);
}
</style>
