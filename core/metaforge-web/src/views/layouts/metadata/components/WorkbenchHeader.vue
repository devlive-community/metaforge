<template>
  <!-- 白底铺满整行：底色在宽屏下从两侧透出来 -->
  <div class="dc-header__bar">
    <div class="flex h-14 w-full items-center justify-between gap-4 px-5">
      <div class="dc-header__navigation flex min-w-0 flex-1 items-center gap-4">
        <!-- 数据源插件 Logo + 数据源名（省略 + tooltip） + 数据库选择器 -->
        <MfTooltip :content="sourceName" placement="bottom">
          <span class="flex min-w-0 cursor-default items-center gap-2.5">
            <MfAvatar :size="28" :src="pluginLogo" :alt="sourceName">
              <Database :size="18"/>
            </MfAvatar>
            <span class="dc-source-name max-w-[144px] truncate text-xl font-bold text-content">{{ sourceName }}</span>
          </span>
        </MfTooltip>

        <MfSelect v-if="can('database:list')" v-model="currentDatabase" class="shrink-0" style="width: 142px" size="sm"
                  :options="databaseOptions" :placeholder="$t('source.common.database')"/>

        <nav class="dc-workbench-nav flex min-w-0 items-center gap-1">
          <router-link v-for="item in navItems" :key="item.path" :to="item.to || item.path"
                       class="dc-header__nav-link inline-flex items-center gap-1.5 whitespace-nowrap h-14 border-b-2 px-2.5 text-sm font-medium transition-colors"
                       :class="isNavActive(item.path) ? 'border-primary text-primary' : 'border-transparent text-content-secondary hover:text-content'">
            <component :size="15" :is="item.icon"/>
            {{ item.label }}
          </router-link>

          <MfDropdown trigger="click" placement="bottomRight">
            <button type="button"
                    class="inline-flex items-center gap-1.5 whitespace-nowrap border-b-2 border-transparent px-2.5 py-0 text-sm font-medium text-content-secondary transition-colors hover:text-content">
              <Ellipsis :size="15"/>
              {{ $t('common.moreNav') }}
              <ChevronDown :size="12"/>
            </button>
            <template #overlay>
              <MfMenu>
                <MfMenuItem v-for="item in moreNavItems" :key_="item.path">
                  <router-link :to="item.path" class="flex items-center gap-2">
                    <component :size="15" v-if="item.icon" :is="item.icon"/>
                    <span>{{ item.label }}</span>
                  </router-link>
                </MfMenuItem>
              </MfMenu>
            </template>
          </MfDropdown>
        </nav>
      </div>

        <MfSpace :size="16" :wrap="false" class="dc-header__actions shrink-0">
          <!-- 全局搜索：在当前数据源内检索数据表 -->
          <div v-if="can('table:list')" ref="searchRoot" class="relative" @focusin="searchFocused = true" @focusout="onSearchBlur">
            <MfInput ref="searchInputRef"
                     v-model="searchText"
                     class="dc-header__search"
                     :placeholder="$t('common.searchPlaceholder')">
              <template #prefix>
                <Search :size="14" :style="{ color: 'var(--dc-text-secondary)' }"/>
              </template>

            </MfInput>

            <div v-if="searchFocused && searchResults.length > 0"
                 class="absolute right-0 top-full z-50 mt-1 flex max-h-80 w-80 flex-col gap-1 overflow-y-auto rounded-mf-lg border border-line bg-surface p-1 shadow-mf-lg">
              <button v-for="item in searchResults" :key="`${ item.database }_${ item.table }`" type="button"
                      class="flex w-full items-center gap-2 rounded-mf-md px-2.5 py-1.5 text-left text-sm transition-colors hover:bg-surface-hover"
                      @mousedown.prevent="onSearchPick(item)">
                <Database :size="13" class="shrink-0 text-content-tertiary"/>
                <span class="shrink-0 text-xs text-content-tertiary">{{ item.database }}</span>
                <span class="min-w-0 flex-1 truncate font-mono text-content">{{ item.table }}</span>
              </button>
            </div>
          </div>

          <div v-if="userInfo" class="flex items-center">
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
                          <router-link :to="`/dataset/info/${item.entityCode}`" target="_blank" class="hover:text-primary flex items-center">
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
                <button type="button" class="dc-header__icon-action" :aria-label="$t('notify.text.center')">
                  <Bell :size="20" />
                </button>
              </MfBadge>
            </MfPopover>
          </div>

          <!-- 帮助 -->
          <MfTooltip :content="$t('common.feedback')">
            <a href="https://github.com/devlive-community/metaforge" target="_blank" rel="noopener noreferrer"
               class="dc-header__icon-action" :aria-label="$t('common.feedback')">
              <CircleHelp :size="18" />
            </a>
          </MfTooltip>

          <MfTooltip :content="$t('user.common.setting')">
            <router-link to="/account/profile" class="dc-header__icon-action" :aria-label="$t('user.common.setting')"><Settings :size="18"/></router-link>
          </MfTooltip>

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
                      <Settings :size="16"/>
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
import { Bell, ChevronDown, CircleHelp, Database, Ellipsis, LogOut, Search, Settings } from '@lucide/vue'
import { useWorkbenchHeader } from '@/composables/useWorkbenchHeader'
defineOptions({ name: 'WorkbenchHeader' })
const { can, sourceName, pluginLogo, currentDatabase, databaseOptions, navItems, isNavActive, moreNavItems,
  searchFocused, searchRoot, searchText, searchInputRef, onSearchBlur, searchResults, onSearchPick, userInfo,
  messages, onNotifyScroll, handleNotificationClick, isLoggedIn, logout } = useWorkbenchHeader()
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
    min-height: 56px;
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
    width: 28px;
    height: 28px;
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

.dc-header__search {
    width: clamp(170px, 17.5vw, 268px);
}

:deep(.dc-header__menu) {
    flex-wrap: wrap;
}

.dc-workbench-nav { overflow-x: auto; scrollbar-width: none; }
.dc-workbench-nav::-webkit-scrollbar { display: none; }

@media (max-width: 1350px) {
    .dc-header__navigation { gap: 12px; }
    .dc-source-name { max-width: 110px; font-size: 18px; }
    .dc-header__nav-link { padding-inline: 7px; font-size: 12px; }
    .dc-header__search { width: 180px; }
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
@media (max-width: 1050px) {
    .dc-source-name { max-width: 80px; font-size: 16px; }
    .dc-header__search { width: 150px; }
    .dc-header__navigation { gap: 8px; }
}
</style>
