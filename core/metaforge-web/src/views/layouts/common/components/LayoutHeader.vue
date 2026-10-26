<template>
  <MfAlert variant="info" closable class="dc-header-banner rounded-none!">
    <router-link to="/admin/chat" target="_blank" class="hover:text-primary">
      Support ChatGPT
    </router-link>
  </MfAlert>

  <!-- 白底铺满整行：底色在宽屏下从两侧透出来 -->
  <div class="dc-header__bar">
    <div class="container flex h-full items-center justify-between w-full">
      <div class="flex items-center">
        <router-link to="/" class="flex items-center gap-2 shrink-0">
          <MfAvatar src="/static/images/logo.png" alt="MetaForge Logo" :size="34"/>
          <span class="dc-header__wordmark">MetaForge</span>
        </router-link>

        <div class="ml-6">
          <MfMenu mode="horizontal" class="dc-header__menu" :selected-keys="selectedKeys">
            <template v-for="item in activeMenus" :key="item.id">
              <MfSubMenu v-if="item.children" :key_="item.url || item.id">
                <template #title>
                  <div class="flex items-center space-x-2">
                    <component v-if="item.icon" :is="menuIcons[item.icon]" :style="{ fontSize: '16px' }"/>
                    <span>{{ item.i18nKey ? $t(item.i18nKey) : 'Unknown' }}</span>
                  </div>
                </template>

                <MfMenuItem v-for="children in item.children" :key_="children.url">
                  <router-link :to="children.url" class="flex items-center space-x-2">
                    <component v-if="children.icon" :is="menuIcons[children.icon]" :style="{ fontSize: '16px' }"/>
                    <span>{{ children.i18nKey ? $t(children.i18nKey) : 'Unknown' }}</span>
                  </router-link>
                </MfMenuItem>
              </MfSubMenu>

              <MfMenuItem v-else :key_="item.url">
                <router-link :to="item.url" class="flex items-center space-x-2">
                  <component v-if="item.icon" :is="menuIcons[item.icon]" :style="{ fontSize: '16px' }"/>
                  <span>{{ item.i18nKey ? $t(item.i18nKey) : 'Unknown' }}</span>
                </router-link>
              </MfMenuItem>
            </template>
          </MfMenu>
        </div>
      </div>

        <MfSpace :size="16">
          <!-- 全局搜索 -->
          <MfInput ref="searchInputRef"
                   v-model="searchText"
                   class="dc-header__search"
                   :placeholder="$t('common.searchPlaceholder')">
            <template #prefix>
              <SearchOutlined :style="{ fontSize: '14px', color: 'var(--dc-text-secondary)' }"/>
            </template>
            <template #suffix>
              <span class="dc-header__kbd">⌘K</span>
            </template>
          </MfInput>

          <!-- 帮助 -->
          <MfTooltip :content="$t('common.feedback')">
            <a href="https://github.com/devlive-community/metaforge" target="_blank" rel="noopener noreferrer"
               class="dc-header__help">
              <QuestionCircleOutlined :style="{ fontSize: '18px' }"/>
            </a>
          </MfTooltip>

          <div v-if="userInfo">
            <MfPopover trigger="click" placement="bottomRight">
              <template #content>
                <div class="dc-notify" style="width: 340px; max-height: 300px; overflow-y: auto;" @scroll="onNotifyScroll">
                  <div v-if="messages.length === 0" class="p-4 text-center text-sm text-content-tertiary">
                    {{ $t('common.noData') }}
                  </div>

                  <div v-for="(item, index) in messages"
                       :key="index"
                       class="dc-notify__item"
                       @click="handleNotificationClick(item)">
                    <div class="mt-1 text-sm text-content-secondary">
                      <div v-if="item.entityExists" class="flex space-x-1">
                        <span>{{ $t(`common.${ item.entityType?.toLowerCase() || '' }`) }}</span>

                        <template v-if="item.entityType === 'DATASET'">
                          <router-link :to="`/admin/dataset/info/${item.entityCode}`" target="_blank" class="hover:text-primary flex items-center">
                            [ {{ item.entityName }} ]
                          </router-link>
                        </template>

                        <template v-else>
                          <router-link class="hover:text-primary" :to="'/' + item.entityType + '/' + item.entityCode">[ {{ item.entityName }} ]</router-link>
                        </template>

                        <span>{{ $t(`common.${ item.type?.toLowerCase() || '' }`) }}</span>
                      </div>
                      <div v-else>
                        {{ $t(`common.${ item.entityType?.toLowerCase() || '' }`) }} [ {{ item.entityName }} ] {{ $t(`common.${ item.type?.toLowerCase() || '' }`) }}
                      </div>
                    </div>

                    <div class="text-xs text-content-tertiary mt-1">{{ item.createTime }}</div>
                  </div>
                </div>
              </template>

              <MfBadge :count="userInfo?.unreadCount || 0">
                <BellOutlined class="dc-header__bell cursor-pointer" :style="{ fontSize: '20px' }"/>
              </MfBadge>
            </MfPopover>
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
          <div v-else>
            <MfDropdown placement="bottomRight" trigger="click">
              <MfAvatar class="dc-header__avatar cursor-pointer"
                        :size="34"
                        :src="userInfo?.avatarConfigure?.path"
                        :alt="userInfo?.username">
                {{ (userInfo?.username || '?').charAt(0).toUpperCase() }}
              </MfAvatar>

              <template #overlay>
                <MfMenu class="min-w-40">
                  <MfMenuItem disabled>
                    <div class="flex flex-col space-y-1">
                      <p class="text-sm font-medium leading-none text-center">{{ userInfo?.username }}</p>
                    </div>
                  </MfMenuItem>

                  <li class="my-1 border-t border-line" role="separator" />

                  <MfMenuItem>
                    <router-link to="/admin/user" class="flex items-center space-x-2">
                      <SettingOutlined :style="{ fontSize: '16px' }"/>
                      <span>{{ $t('user.common.setting') }}</span>
                    </router-link>
                  </MfMenuItem>

                  <MfMenuItem @click="logout">
                    <div class="flex items-center space-x-2">
                      <LogoutOutlined :style="{ fontSize: '16px' }"/>
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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import { useUserStore } from '@/stores/user'
import { storeToRefs } from 'pinia'
import { TokenUtils } from '@/utils/token'
import router from '@/router'
import { createDefaultRouter } from '@/router/default'
import NotificationService from '@/services/notification'
import { FilterModel } from '@/model/filter.ts'
import { BellOutlined, BgColorsOutlined, SearchOutlined, CodeOutlined, DashboardOutlined, HistoryOutlined, HomeOutlined, LogoutOutlined, ProjectOutlined, QuestionCircleOutlined, RightOutlined, SettingOutlined, ToolOutlined, UnorderedListOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'LayoutHeader' })

/**
 * 顶级菜单高亮：按路径前缀匹配（子页面如 /admin/user/profile 也命中所属菜单）
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

// 后端菜单返回的是图标名字符串，这里映射到图标组件
const menuIcons: Record<string, any> = {
    Contrast: BgColorsOutlined,
    Gauge: DashboardOutlined,
    Hammer: ToolOutlined,
    History: HistoryOutlined,
    Home: HomeOutlined,
    List: UnorderedListOutlined,
    SquareChevronRight: RightOutlined,
    SquareDashedBottomCode: CodeOutlined,
    SquareKanban: ProjectOutlined
}

const route = useRoute()
const userStore = useUserStore()
const filter: FilterModel = new FilterModel()
const messages = ref<any[]>([])
const pageIndex = ref<number>(1)
const hasMoreData = ref(true)
const loading = ref(false)

const { userInfo, isLoggedIn, menu: activeMenus } = storeToRefs(userStore)

const fetchMessages = async (value: number = 1, append = false) => {
  filter.page = value
  filter.orders = [{ column: 'createTime', order: 'desc' }]
  loading.value = true
  try {
    const response = await NotificationService.getAll(filter)
    if (response.status && response.data) {
      const items = response.data.content.map((item: any) => {
        item.read = item.isRead
        return item
      })
      messages.value = append ? [...messages.value, ...items] : items
      pageIndex.value = response.data.page
      hasMoreData.value = response.data.page < response.data.totalPage
    }
    else {
      MfMessage.error(response.message)
    }
  }
  finally {
    loading.value = false
  }
}

const onNotifyScroll = (event: any) => {
  const target = event.target
  if (loading.value || !hasMoreData.value) {
    return
  }
  if (target.scrollTop + target.offsetHeight >= target.scrollHeight - 10) {
    fetchMessages(pageIndex.value + 1, true)
  }
}

onMounted(async () => {
  if (TokenUtils.getAuthUser()) {
    await userStore.fetchUserInfo()
    await fetchMessages()
  }
  window.addEventListener('keydown', onGlobalKeydown)
})

onBeforeUnmount(() => window.removeEventListener('keydown', onGlobalKeydown))

const searchText = ref('')
const searchInputRef = ref<HTMLInputElement | null>(null)

const focusSearch = () => {
  searchInputRef.value?.focus()
}

const onGlobalKeydown = (event: KeyboardEvent) => {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    focusSearch()
  }
}

const logout = () => {
  userStore.logout()
  createDefaultRouter(router)
  router.push('/auth/signin')
}

const handleNotificationClick = (msg: any) => {
  const { id, code } = msg
  const payload = { id, code, isRead: true }
  NotificationService.saveOrUpdate(payload)
                     .then(response => {
                       if (response.status && response.data) {
                         fetchMessages()
                         userStore.fetchUserInfo()
                       }
                       else {
                         MfMessage.error(response.message)
                       }
                     })
}
</script>

<style scoped>
.dc-notify__item {
    padding: 8px 12px;
    border-radius: 6px;
    cursor: pointer;
}

.dc-notify__item:hover {
    background-color: var(--mf-color-surface-hover);
}

.dc-header__bar {
    height: 64px;
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

.dc-header__bell {
    color: var(--dc-text-primary);
    transition: color 0.2s;
}

.dc-header__bell:hover {
    color: var(--dc-primary);
}

.dc-header__help {
    display: flex;
    align-items: center;
    color: var(--dc-text-secondary);
    transition: color 0.2s;
}

.dc-header__help:hover {
    color: var(--dc-primary);
}

.dc-header__search {
    width: 240px;
}

.dc-header__kbd {
    display: inline-flex;
    align-items: center;
    padding: 0 5px;
    border: 1px solid var(--dc-border);
    border-radius: 4px;
    font-size: 11px;
    line-height: 16px;
    color: var(--dc-text-secondary);
    background: var(--dc-bg);
}

.dc-header__avatar {
    background: var(--dc-gradient-avatar);
    box-shadow: var(--dc-shadow-avatar);
    font-weight: 600;
}
</style>
