<template>
  <div class="dc-notify">
    <div class="dc-notify__pagehead">
      <div class="dc-notify__title">{{ $t('user.common.notify') }}</div>
      <div class="dc-notify__desc">{{ $t('notify.tip.page') }}</div>
    </div>

    <div v-if="loading" class="dc-notify__card dc-notify__loading">
      <MfSpinner size="lg" :label="$t('common.loading')"/>
    </div>

    <MfResult v-else-if="failed"
              status="error"
              class="dc-notify__card"
              :title="$t('notify.tip.loadFailed')"
              :sub-title="failed">
      <template #extra>
        <MfButton @click="load">{{ $t('common.refresh') }}</MfButton>
      </template>
    </MfResult>

    <template v-else>
      <!-- Channels: compact cards that wrap, settings live in a drawer -->
      <div class="dc-notify__card">
        <div class="dc-notify__section-head">
          <div class="min-w-0">
            <div class="dc-notify__section-title">{{ $t('notify.common.channels') }}</div>
            <div class="dc-notify__section-sub">{{ $t('notify.tip.channels') }}</div>
          </div>
        </div>

        <div class="dc-notify__channels">
          <div v-for="channel in channels"
               :key="channel.type"
               class="dc-notify__channel"
               :class="{ 'dc-notify__channel--on': savedOf(channel.type).enabled }">
            <div class="dc-notify__channel-top">
              <div class="dc-notify__channel-icon">
                <component :is="channelIcon(channel.type)" :size="18"/>
              </div>
              <div class="min-w-0 flex-1">
                <div class="dc-notify__channel-name">{{ channelName(channel.type) }}</div>
                <div class="dc-notify__channel-state">
                  <span class="dc-notify__dot"/>
                  {{ stateText(channel) }}
                </div>
              </div>
              <MfSwitch size="sm"
                        :model-value="savedOf(channel.type).enabled"
                        :disabled="busy === channel.type"
                        @update:model-value="(value: any) => toggle(channel, !!value)"/>
            </div>

            <div class="dc-notify__channel-desc">{{ channelDesc(channel.type) }}</div>

            <div class="dc-notify__channel-foot">
              <span class="dc-notify__channel-meta">
                {{ channel.fields.length > 0 ? $t('notify.tip.fieldCount', { count: channel.fields.length }) : $t('notify.tip.noSettings') }}
              </span>
              <div class="dc-notify__channel-actions">
                <MfButton v-if="savedOf(channel.type).enabled"
                          size="sm"
                          variant="ghost"
                          :loading="testing === channel.type"
                          :disabled="testing !== '' && testing !== channel.type"
                          @click="test(channel, savedOf(channel.type).configure)">
                  {{ $t('notify.common.test') }}
                </MfButton>
                <MfButton v-if="savedOf(channel.type).enabled && channel.fields.length > 0"
                          size="sm"
                          @click="openEditor(channel, false)">
                  {{ $t('notify.common.configure') }}
                </MfButton>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Scenes by category; one column per enabled channel -->
      <div class="dc-notify__card">
        <div class="dc-notify__section-head">
          <div class="min-w-0 flex-1">
            <div class="dc-notify__section-title">{{ $t('notify.common.scenarios') }}</div>
            <div class="dc-notify__section-sub">{{ $t('notify.tip.scenarios') }}</div>
          </div>
          <div v-if="enabledChannels.length > 0" class="dc-notify__section-actions">
            <MfButton :disabled="!scenesDirty || savingScenes" @click="resetScenes">{{ $t('common.reset') }}</MfButton>
            <MfButton variant="primary" :loading="savingScenes" :disabled="!scenesDirty" @click="saveScenes">{{ $t('common.save') }}</MfButton>
          </div>
        </div>

        <MfEmpty v-if="enabledChannels.length === 0" size="sm" :description="$t('notify.tip.noEnabledChannel')"/>

        <div v-else class="dc-notify__matrix">
          <div class="dc-notify__matrix-row dc-notify__matrix-head" :style="gridStyle">
            <div/>
            <div v-for="channel in enabledChannels" :key="channel.type" class="dc-notify__matrix-cell" :title="channelName(channel.type)">
              {{ channelName(channel.type) }}
            </div>
          </div>

          <div v-for="category in categories" :key="category.key" class="dc-notify__category">
            <div class="dc-notify__matrix-row dc-notify__category-row" :style="gridStyle">
              <div class="min-w-0">
                <div class="dc-notify__category-name">{{ categoryName(category.key) }}</div>
                <div class="dc-notify__category-desc">{{ categoryDesc(category.key) }}</div>
              </div>
              <div v-for="channel in enabledChannels" :key="channel.type" class="dc-notify__matrix-cell">
                <MfCheckbox :model-value="coverage(draftOf(channel.type), category.scenes) === 'all'"
                            :indeterminate="coverage(draftOf(channel.type), category.scenes) === 'some'"
                            :disabled="savingScenes"
                            @update:model-value="(checked: any) => setScenes(draftOf(channel.type), category.scenes, !!checked)"/>
              </div>
            </div>
            <div v-for="scene in category.scenes" :key="scene" class="dc-notify__matrix-row" :style="gridStyle">
              <div class="dc-notify__scene-name">{{ sceneName(scene) }}</div>
              <div v-for="channel in enabledChannels" :key="channel.type" class="dc-notify__matrix-cell">
                <MfCheckbox :model-value="draftOf(channel.type).services.includes(scene)"
                            :disabled="savingScenes"
                            @update:model-value="(checked: any) => setScenes(draftOf(channel.type), [scene], !!checked)"/>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Channel settings -->
    <MfDrawer v-model:open="editorOpen"
              width="min(480px, 100vw)"
              :close-on-overlay="!savingChannel"
              :closable="!savingChannel">
      <template #title>
        <div v-if="editor" class="flex items-center gap-2">
          <component :is="channelIcon(editor.channel.type)" :size="16"/>
          {{ channelName(editor.channel.type) }}
        </div>
      </template>

      <div v-if="editor" class="dc-notify__editor">
        <div class="dc-notify__editor-tip">
          {{ editor.enabling ? $t('notify.tip.configureToEnable') : channelDesc(editor.channel.type) }}
        </div>
        <MfForm ref="editorForm" layout="vertical" :model="editor.configure">
          <MfFormItem v-for="field in editor.channel.fields"
                      :key="field.name"
                      :name="field.name"
                      :label="fieldLabel(editor.channel.type, field)"
                      :tip="fieldTip(editor.channel.type, field)"
                      :required-mark="field.required"
                      :rules="fieldRules(editor.channel.type, field)">
            <MfInput v-model="editor.configure[field.name]"
                     :type="field.type === 'SECRET' ? 'password' : 'text'"
                     :show-password="field.type === 'SECRET'"
                     :maxlength="MAX_VALUE_LENGTH"
                     :placeholder="fieldPlaceholder(editor.channel.type, field)"/>
          </MfFormItem>
        </MfForm>

        <div v-if="savingChannel" class="dc-notify__editor-mask">
          <MfSpinner :label="$t('notify.tip.saving')"/>
        </div>
      </div>

      <template #footer>
        <div v-if="editor" class="dc-notify__editor-foot">
          <MfButton :loading="testing === editor.channel.type"
                    :disabled="savingChannel || (testing !== '' && testing !== editor.channel.type)"
                    @click="testEditor">
            {{ $t('notify.common.test') }}
          </MfButton>
          <div class="flex gap-2">
            <MfButton :disabled="savingChannel" @click="editorOpen = false">{{ $t('common.cancel') }}</MfButton>
            <MfButton variant="primary" :loading="savingChannel" @click="saveEditor">
              {{ editor.enabling ? $t('notify.common.saveAndEnable') : $t('common.save') }}
            </MfButton>
          </div>
        </div>
      </template>
    </MfDrawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { Mail, MessageCircle, Plug } from '@lucide/vue'
import UserService from '@/services/user.ts'
import { useNotificationStore } from '@/stores/notifications'
import { coverage, NotifyCategory, NotifyChannel, NotifyChannelSetting, NotifyField, sameScenes, setScenes } from '@/model/notify.ts'

defineOptions({ name: 'NotifyForm' })

const MAX_VALUE_LENGTH = 2000

const { t, te } = useI18n()
const notifications = useNotificationStore()

const loading = ref(true)
const failed = ref('')
const busy = ref('')
const testing = ref('')
const savingScenes = ref(false)
const savingChannel = ref(false)
const channels = ref<NotifyChannel[]>([])
const categories = ref<NotifyCategory[]>([])
/** What the server holds. Channel changes save at once; scene ticks stay in drafts until saved. */
const saved = ref<NotifyChannelSetting[]>([])
const drafts = ref<Record<string, { services: string[] }>>({})

const editorOpen = ref(false)
const editorForm = ref<any>()
const editor = ref<{ channel: NotifyChannel, enabling: boolean, configure: Record<string, string> } | null>(null)

const empty = (type: string): NotifyChannelSetting => ({ type, enabled: false, services: [], configure: {}, configured: [] })
const savedOf = (type: string): NotifyChannelSetting => saved.value.find(item => item.type === type) || empty(type)
const draftOf = (type: string): { services: string[] } => drafts.value[type] || { services: [] }

const enabledChannels = computed(() => channels.value.filter(channel => savedOf(channel.type).enabled))
const scenesDirty = computed(() => enabledChannels.value.some(channel => !sameScenes(draftOf(channel.type).services, savedOf(channel.type).services)))
const gridStyle = computed(() => ({ gridTemplateColumns: `minmax(160px, 1fr) repeat(${ enabledChannels.value.length }, 88px)` }))

const translated = (key: string, fallback: string): string => te(key) ? t(key) : fallback

const channelIcon = (type: string): any => type === 'Internal' ? Mail : type === 'DingTalk' ? MessageCircle : Plug
const channelName = (type: string): string => translated(`notify.text.${ type.toLowerCase() }`, type)
const channelDesc = (type: string): string => translated(`notify.tip.${ type.toLowerCase() }`, t('notify.tip.channelFallback'))
const categoryName = (key: string): string => translated(`notify.category.${ key.toLowerCase() }`, key)
const categoryDesc = (key: string): string => translated(`notify.categoryTip.${ key.toLowerCase() }`, '')
const sceneName = (key: string): string => translated(`notify.scene.${ key }`, key)
const fieldLabel = (type: string, field: NotifyField): string => translated(`notify.field.${ type.toLowerCase() }.${ field.name }`, field.label)
const fieldTip = (type: string, field: NotifyField): string => translated(`notify.fieldTip.${ type.toLowerCase() }.${ field.name }`, field.description || '')

const isConfigured = (type: string, field: NotifyField): boolean => (savedOf(type).configured || []).includes(field.name)

/** A channel with required settings it does not hold yet cannot be turned on without them. */
const needsSettings = (channel: NotifyChannel): boolean => {
  const setting = savedOf(channel.type)
  return channel.fields.some(field => field.required && !isConfigured(channel.type, field) && !setting.configure[field.name])
}

const stateText = (channel: NotifyChannel): string => {
  if (!savedOf(channel.type).enabled) {
    return t('notify.state.off')
  }
  const count = draftOf(channel.type).services.length
  return count > 0 ? t('notify.state.receiving', { count }) : t('notify.state.noScene')
}

const fieldPlaceholder = (type: string, field: NotifyField): string => {
  if (field.type === 'SECRET' && isConfigured(type, field)) {
    return t('notify.tip.secretKept')
  }
  return field.placeholder || ''
}

const fieldRules = (type: string, field: NotifyField) => {
  const label = fieldLabel(type, field)
  return [
    // A secret that is already stored may stay empty: the server keeps it.
    { required: field.required && !(field.type === 'SECRET' && isConfigured(type, field)), message: t('notify.validate.required', { field: label }) },
    { max: MAX_VALUE_LENGTH, message: t('notify.validate.tooLong', { field: label }) },
    ...(field.type === 'URL'
      ? [{ validator: (value: unknown) => !value || /^https?:\/\/\S+$/i.test(String(value)), message: t('notify.validate.url', { field: label }) }]
      : [])
  ]
}

const adopt = (data: NotifyChannelSetting[], keepDrafts: string[] = []) => {
  saved.value = channels.value.map(channel => {
    const item = (data || []).find(entry => entry.type === channel.type) || empty(channel.type)
    const configure: Record<string, string> = {}
    channel.fields.forEach(field => (configure[field.name] = item.configure?.[field.name] || ''))
    return { type: channel.type, enabled: !!item.enabled, services: [...(item.services || [])], configure, configured: [...(item.configured || [])] }
  })
  const next: Record<string, { services: string[] }> = {}
  saved.value.forEach(setting => {
    next[setting.type] = keepDrafts.includes(setting.type) && drafts.value[setting.type]
      ? drafts.value[setting.type]
      : { services: [...setting.services] }
  })
  drafts.value = next
}

const load = async () => {
  loading.value = true
  failed.value = ''
  try {
    const [catalog, current] = await Promise.all([UserService.getNotifyCatalog(), UserService.getNotifySettings()])
    if (!catalog.status || !current.status) {
      failed.value = String((catalog.status ? current : catalog).message || '')
      return
    }
    channels.value = catalog.data?.channels || []
    categories.value = catalog.data?.categories || []
    adopt(current.data)
  }
  finally {
    loading.value = false
  }
}

/** Saves one channel with its stored subscriptions; unsaved scene ticks of other channels stay as drafts. */
const saveChannel = async (setting: NotifyChannelSetting): Promise<boolean> => {
  const response = await UserService.saveNotifySettings([setting])
  if (!response.status) {
    MfMessage.error(response.message)
    return false
  }
  adopt(response.data, saved.value.map(item => item.type).filter(type => type !== setting.type))
  return true
}

const toggle = async (channel: NotifyChannel, enabled: boolean) => {
  if (enabled && needsSettings(channel)) {
    openEditor(channel, true)
    return
  }
  busy.value = channel.type
  try {
    const current = savedOf(channel.type)
    // Secrets go back empty, which keeps the stored values.
    if (await saveChannel({ ...current, enabled })) {
      MfMessage.success(t(enabled ? 'notify.tip.enabled' : 'notify.tip.disabled', { channel: channelName(channel.type) }))
    }
  }
  finally {
    busy.value = ''
  }
}

const openEditor = (channel: NotifyChannel, enabling: boolean) => {
  editor.value = { channel, enabling, configure: { ...savedOf(channel.type).configure } }
  editorOpen.value = true
}

const validEditor = async (): Promise<boolean> => {
  try {
    await editorForm.value?.validate()
    return true
  }
  catch {
    return false
  }
}

const saveEditor = async () => {
  if (!editor.value || !(await validEditor())) {
    return
  }
  const { channel, enabling, configure } = editor.value
  savingChannel.value = true
  try {
    const current = savedOf(channel.type)
    if (await saveChannel({ ...current, enabled: enabling || current.enabled, configure: { ...configure } })) {
      MfMessage.success(t(enabling ? 'notify.tip.enabled' : 'common.successfully', { channel: channelName(channel.type) }))
      editorOpen.value = false
    }
  }
  finally {
    savingChannel.value = false
  }
}

const test = async (channel: NotifyChannel, configure: Record<string, string>) => {
  testing.value = channel.type
  try {
    const response = await UserService.testNotify({ type: channel.type, enabled: true, services: [], configure })
    if (response.status) {
      MfMessage.success(t('notify.tip.testSent', { channel: channelName(channel.type) }))
      if (channel.builtin) {
        // The site channel stored a notification: show it in the header now.
        notifications.changed()
      }
    }
    else {
      MfMessage.error(response.message)
    }
  }
  finally {
    testing.value = ''
  }
}

const testEditor = async () => {
  if (editor.value && await validEditor()) {
    await test(editor.value.channel, editor.value.configure)
  }
}

const resetScenes = () => adopt(saved.value)

const saveScenes = async () => {
  savingScenes.value = true
  try {
    const payload = enabledChannels.value.map(channel => ({ ...savedOf(channel.type), services: [...draftOf(channel.type).services] }))
    const response = await UserService.saveNotifySettings(payload)
    if (response.status) {
      adopt(response.data)
      MfMessage.success(t('common.successfully'))
    }
    else {
      MfMessage.error(response.message)
    }
  }
  finally {
    savingScenes.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.dc-notify__pagehead {
    padding: 18px 20px;
    border-radius: var(--dc-radius-lg);
    background: var(--dc-gradient-hero);
    border: 1px solid var(--dc-border-light);
    margin-bottom: 16px;
}

.dc-notify__title {
    font-size: 22px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-notify__desc {
    font-size: 13px;
    color: var(--dc-text-secondary);
    margin-top: 4px;
}

.dc-notify__card {
    background: var(--dc-surface);
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-lg);
    box-shadow: var(--dc-shadow-card);
    padding: 20px;
    margin-bottom: 16px;
}

.dc-notify__loading {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 240px;
}

.dc-notify__section-head {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
}

.dc-notify__section-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-notify__section-sub {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 2px;
}

.dc-notify__section-actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
    white-space: nowrap;
}

/* Cards wrap into as many columns as fit, so more channels only add rows. */
.dc-notify__channels {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 12px;
}

.dc-notify__channel {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 14px;
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-md);
    background: var(--dc-surface);
    transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.dc-notify__channel:hover {
    box-shadow: var(--dc-shadow-card);
}

.dc-notify__channel-top {
    display: flex;
    align-items: center;
    gap: 10px;
}

.dc-notify__channel-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: var(--dc-radius-md);
    background: var(--dc-surface-hover);
    color: var(--dc-text-secondary);
    flex-shrink: 0;
}

.dc-notify__channel--on .dc-notify__channel-icon {
    background: var(--dc-primary-light);
    color: var(--dc-primary);
}

.dc-notify__channel-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--dc-text-title);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.dc-notify__channel-state {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 2px;
}

.dc-notify__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--dc-border);
    flex-shrink: 0;
}

.dc-notify__channel--on .dc-notify__dot {
    background: var(--dc-primary);
}

.dc-notify__channel-desc {
    font-size: 12px;
    line-height: 18px;
    min-height: 36px;
    color: var(--dc-text-secondary);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.dc-notify__channel-foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    min-height: 28px;
    padding-top: 10px;
    border-top: 1px solid var(--dc-border-light);
}

.dc-notify__channel-meta {
    font-size: 12px;
    color: var(--dc-text-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.dc-notify__channel-actions {
    display: flex;
    gap: 6px;
    flex-shrink: 0;
    white-space: nowrap;
}

.dc-notify__matrix {
    overflow-x: auto;
}

.dc-notify__matrix-row {
    display: grid;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    min-width: max-content;
}

.dc-notify__matrix-head {
    font-size: 12px;
    font-weight: 600;
    color: var(--dc-text-secondary);
    border-bottom: 1px solid var(--dc-border-light);
}

.dc-notify__matrix-cell {
    display: flex;
    justify-content: center;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.dc-notify__matrix-head .dc-notify__matrix-cell {
    display: block;
    text-align: center;
}

.dc-notify__category-row {
    background: var(--dc-surface-hover);
    border-radius: var(--dc-radius-md);
    margin-top: 8px;
}

.dc-notify__category-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-notify__category-desc {
    font-size: 12px;
    color: var(--dc-text-secondary);
}

.dc-notify__scene-name {
    font-size: 13px;
    color: var(--dc-text-title);
    padding-left: 12px;
}

.dc-notify__editor {
    position: relative;
    min-height: 100%;
}

.dc-notify__editor-tip {
    font-size: 12px;
    color: var(--dc-text-secondary);
    padding: 10px 12px;
    margin-bottom: 16px;
    border-radius: var(--dc-radius-md);
    background: var(--dc-surface-hover);
}

.dc-notify__editor-mask {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: color-mix(in srgb, var(--dc-surface) 70%, transparent);
}

.dc-notify__editor-foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    width: 100%;
    white-space: nowrap;
}
</style>
