<template>
  <MfAlert variant="info" closable class="dc-header-banner rounded-none!">
    <router-link to="/chat" target="_blank" class="hover:text-primary">
      Support ChatGPT
    </router-link>
  </MfAlert>

  <!-- 白底铺满整行：底色在宽屏下从两侧透出来 -->
  <div class="dc-header__bar">
    <div class="container dc-header__layout flex min-h-16 w-full items-center justify-between gap-6 py-3">
      <div class="dc-header__navigation flex min-w-0 flex-1 items-center gap-6">
        <router-link to="/" class="flex items-center gap-2 shrink-0">
          <img src="/static/images/logo.png" alt="" width="40" height="40" class="dc-brand-logo"/>
          <span class="dc-header__wordmark">MetaForge</span>
        </router-link>

        <div class="min-w-0">
          <MfMenu mode="horizontal" class="dc-header__menu" :selected-keys="selectedKeys">
            <template v-for="item in activeMenus" :key="item.id">
              <MfSubMenu v-if="item.children" :key_="item.url || item.id">
                <template #title>
                  <div class="flex items-center space-x-2">
                    <component :size="16" v-if="item.icon" :is="menuIcons[item.icon]" />
                    <span>{{ item.i18nKey ? $t(item.i18nKey) : 'Unknown' }}</span>
                  </div>
                </template>

                <MfMenuItem v-for="children in item.children" :key_="children.url">
                  <router-link :to="children.url" class="flex items-center space-x-2">
                    <component :size="16" v-if="children.icon" :is="menuIcons[children.icon]" />
                    <span>{{ children.i18nKey ? $t(children.i18nKey) : 'Unknown' }}</span>
                  </router-link>
                </MfMenuItem>
              </MfSubMenu>

              <MfMenuItem v-else :key_="item.url">
                <router-link :to="item.url" class="flex items-center space-x-2">
                  <component :size="16" v-if="item.icon" :is="menuIcons[item.icon]" />
                  <span>{{ item.i18nKey ? $t(item.i18nKey) : 'Unknown' }}</span>
                </router-link>
              </MfMenuItem>
            </template>
          </MfMenu>
        </div>
      </div>

        <MfSpace :size="16" :wrap="false" class="dc-header__actions shrink-0">
          <!-- 帮助 -->
          <MfTooltip :content="$t('common.feedback')">
            <a href="https://github.com/devlive-community/metaforge" target="_blank" rel="noopener noreferrer"
               class="dc-header__icon-action" :aria-label="$t('common.feedback')">
              <CircleHelp :size="18" />
            </a>
          </MfTooltip>

          <div v-if="userInfo" class="flex items-center">
            <NotificationBell/>
          </div>

          <!-- User Info -->
          <MfSpace v-if="!isLoggedIn">
            <router-link to="/auth/signin">
              <MfButton variant="primary">{{ $t('user.common.signin') }}</MfButton>
            </router-link>
            <router-link to="/auth/signup">
              <MfButton>{{ $t('user.common.signup') }}</MfButton>
            </router-link>
          </MfSpace>
          <div v-else class="flex items-center">
            <MfDropdown placement="bottomRight" trigger="click">
              <button type="button" class="inline-flex shrink-0 rounded-mf-full outline-none focus-visible:ring-2 focus-visible:ring-primary/40 focus-visible:ring-offset-2" :aria-label="userInfo?.username || $t('user.common.setting')" aria-haspopup="menu">
                <MfAvatar class="dc-header__avatar"
                          :size="34"
                          :src="userInfo?.avatarConfigure?.path"
                          :alt="userInfo?.username">
                  {{ (userInfo?.username || '?').charAt(0).toUpperCase() }}
                </MfAvatar>
              </button>

              <template #overlay>
                <MfMenu class="min-w-40">
                  <MfMenuItem disabled>
                    <div class="flex flex-col space-y-1">
                      <p class="text-sm font-medium leading-none text-center">{{ userInfo?.username }}</p>
                    </div>
                  </MfMenuItem>

                  <li class="my-1 border-t border-line" role="separator" />

                  <MfMenuItem>
                    <router-link to="/account/profile" class="flex items-center space-x-2">
                      <SettingOutlined :style="{ fontSize: '16px' }"/>
                      <span>{{ $t('user.common.setting') }}</span>
                    </router-link>
                  </MfMenuItem>

                  <MfMenuItem @click="logout">
                    <div class="flex items-center space-x-2">
                      <LogOut :size="16" />
                      <span>{{ $t('user.common.signout') }}</span>
                    </div>
                  </MfMenuItem>
                </MfMenu>
              </template>
            </MfDropdown>
          </div>
        </MfSpace>
      </div>
    </div>
</template>

<script setup lang="ts">
import NotificationBell from '@/views/components/notify/NotificationBell.vue'
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { storeToRefs } from 'pinia'
import { TokenUtils } from '@/utils/token'
import router from '@/router'
import { CircleHelp, LogOut } from '@lucide/vue'
import { MENU_ICONS } from '@/config/menu-icons'

defineOptions({ name: 'LayoutHeader' })

/**
 * 顶级菜单高亮：按路径前缀匹配（子页面如 /account/profile 也命中所属菜单）
 */
const selectedKeys = computed(() => {
    const urls: string[] = []
    ;(activeMenus.value || []).forEach((item: any) => {
        if (item.children?.length) {
            if (item.url) {
                urls.push(item.url)
            }
            item.children.forEach((child: any) => urls.push(child.url))
        }
        else if (item.url) {
            urls.push(item.url)
        }
    })
    const matched = urls.filter(url => route.path.startsWith(url))
                        .sort((a, b) => b.length - a.length)[0]
    return matched ? [matched] : []
})

// 后端菜单返回的是图标名字符串，映射表统一维护在 src/config/menu-icons.ts
const menuIcons = MENU_ICONS

const route = useRoute()
const userStore = useUserStore()

const { userInfo, isLoggedIn, menu: activeMenus } = storeToRefs(userStore)

onMounted(async () => {
  if (TokenUtils.getAuthUser()) {
    await Promise.all([userStore.fetchUserInfo(), userStore.fetchMenus()])
  }
})

const logout = () => {
  userStore.logout()
  router.push('/auth/signin')
}

</script>

<style scoped>
.dc-header__bar {
    min-height: 64px;
    display: flex;
    align-items: center;
    background: var(--dc-header);
    backdrop-filter: blur(8px);
}

.dc-header__wordmark {
    font-size: 18px;
    font-weight: 700;
    color: var(--dc-text-title);
    letter-spacing: 0.2px;
}

.dc-header__icon-action {
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

.dc-header__icon-action:hover {
    color: var(--mf-color-primary);
    background-color: var(--mf-color-surface-hover);
}

.dc-header__icon-action:focus-visible {
    outline: 2px solid var(--mf-color-primary);
    outline-offset: 2px;
}

:deep(.dc-header__menu) {
    flex-wrap: wrap;
}

@media (max-width: 1200px) {
    .dc-header__layout {
        flex-wrap: wrap;
        row-gap: 12px;
    }

    .dc-header__navigation {
        flex-basis: 100%;
    }

    .dc-header__actions {
        margin-left: auto;
    }
}

.dc-header__avatar {
    background: var(--dc-gradient-avatar);
    box-shadow: var(--dc-shadow-avatar);
    font-weight: 600;
}
</style>
