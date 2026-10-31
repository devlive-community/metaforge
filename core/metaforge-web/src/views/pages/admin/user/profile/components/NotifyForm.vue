<template>
  <div class="dc-notify">
    <!-- 页头：标题 + 描述 + 标语 -->
    <div class="dc-notify__pagehead">
      <div class="min-w-0">
        <div class="dc-notify__title">{{ $t('user.common.notify') }}</div>
        <div class="dc-notify__desc">{{ $t('notify.tip.page') }}</div>
      </div>
      <div class="dc-notify__slogan">Data for a smarter tomorrow</div>
    </div>

    <!-- 通知渠道 -->
    <div class="dc-notify__card">
      <div class="dc-notify__section-head">
        <div class="dc-notify__section-icon">
          <Mail :size="16" />
        </div>
        <div>
          <div class="dc-notify__section-title">{{ $t('notify.common.channels') }}</div>
          <div class="dc-notify__section-sub">{{ $t('notify.tip.channels') }}</div>
        </div>
      </div>

      <template v-if="loading">
        <div class="flex items-center justify-center py-10"><MfSpinner/></div>
      </template>
      <template v-else>
        <div class="dc-notify__channel-grid">
          <div v-for="channel in channels"
               :key="channel.type"
               class="dc-notify__channel">
            <div class="flex items-start gap-3">
              <div class="dc-notify__channel-icon">
                <component :size="18" :is="channelIcon(channel.type)" />
              </div>
              <div class="min-w-0">
                <div class="dc-notify__channel-name">{{ channelName(channel.type) }}</div>
                <div class="dc-notify__channel-desc">{{ channelDesc(channel.type) }}</div>
              </div>
            </div>
            <div class="flex items-center justify-between mt-3">
              <span class="dc-notify__channel-state"
                    :class="channel.enabled ? 'dc-notify__channel-state--on' : 'dc-notify__channel-state--off'">
                <CircleCheck :size="12" v-if="channel.enabled"/>
                <CircleMinus :size="12" v-else/>
                {{ channel.enabled ? $t('notify.state.enabled') : $t('notify.state.disabled') }}
              </span>
              <MfButton v-if="!channel.enabled"
                        size="sm"
                        @click="toggleChannel(channel, true)">
                {{ $t('notify.common.enable') }}
              </MfButton>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 触发场景 -->
    <div class="dc-notify__card">
      <div class="dc-notify__section-head">
        <div class="dc-notify__section-icon">
          <BellRing :size="16" />
        </div>
        <div class="min-w-0">
          <div class="dc-notify__section-title">{{ $t('notify.common.scenarios') }}</div>
          <div class="dc-notify__section-sub">{{ $t('notify.tip.scenarios') }}</div>
        </div>
        <MfButton variant="primary"
                  style="margin-left: auto"
                  @click="openBatch">
          <Settings :size="14"/>
          {{ $t('notify.common.batch') }}
        </MfButton>
      </div>

      <div class="dc-notify__rows">
        <div v-for="scenario in scenarios"
             :key="scenario.key"
             class="dc-notify__row">
          <div class="dc-notify__row-icon">
            <component :size="16" :is="scenario.icon" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="dc-notify__row-name">{{ $t(scenario.nameKey) }}</div>
            <div class="dc-notify__row-desc">{{ $t(scenario.tipKey) }}</div>
          </div>
          <MfSwitch :model-value="isScenarioOn(scenario.key)"
                    :disabled="saving"
                    @update:model-value="checked => toggleScenario(scenario.key, checked)"/>
          <MfButton variant="ghost" size="sm" @click="openScenarioChannels(scenario.key)">
            <Settings :size="15" :style="{ color: 'var(--dc-text-secondary)' }"/>
          </MfButton>
        </div>
      </div>
    </div>

    <!-- 批量设置：发送渠道 × 触发场景 -->
    <MfModal v-model:open="batchVisible"
             :title="$t('notify.common.batch')"
             width="480px">
      <div class="dc-notify__batch-section">
        <div class="dc-notify__batch-label">{{ $t('notify.common.sendChannels') }}</div>
        <MfCheckboxGroup v-model="batchChannels"
                         class="dc-notify__batch-group"
                         :options="channels.map((channel: any) => ({ label: channelName(channel.type), value: channel.type }))"/>
      </div>

      <div class="dc-notify__batch-section">
        <div class="dc-notify__batch-label">{{ $t('notify.common.scenarios') }}</div>
        <MfCheckboxGroup v-model="batchScenarios"
                         class="dc-notify__batch-group"
                         :options="scenarios.map((scenario: any) => ({ label: $t(scenario.nameKey), value: scenario.key }))"/>
      </div>

      <div class="dc-notify__batch-tip">{{ $t('notify.tip.batch') }}</div>

      <template #footer>
        <MfButton @click="batchVisible = false">{{ $t('common.cancel') }}</MfButton>
        <MfButton variant="primary" :loading="saving" @click="applyBatch">{{ $t('common.apply') }}</MfButton>
      </template>
    </MfModal>

    <!-- 单场景配置：选择接收该场景通知的渠道 -->
    <MfModal v-model:open="scenarioVisible"
             :title="scenarioName(scenarioKey)"
             width="440px">
      <div class="dc-notify__batch-section">
        <div class="dc-notify__batch-label">{{ $t('notify.common.sendChannels') }}</div>
        <MfCheckboxGroup v-model="scenarioChannels"
                         class="dc-notify__batch-group"
                         :options="channels.map((channel: any) => ({ label: channelName(channel.type), value: channel.type }))"/>
      </div>

      <template #footer>
        <MfButton @click="scenarioVisible = false">{{ $t('common.cancel') }}</MfButton>
        <MfButton variant="primary" :loading="saving" @click="applyScenarioChannels">{{ $t('common.confirm') }}</MfButton>
      </template>
    </MfModal>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { BellRing, CircleCheck, CircleMinus, CirclePlus, Mail, MessageCircle, Pencil, Plug, RefreshCw, Settings, Trash2 } from '@lucide/vue'
import UserService from '@/services/user.ts'
import PluginService from '@/services/plugin.ts'
import { HttpUtils } from '@/utils/http.ts'

defineOptions({ name: 'NotifyForm' })

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const formState = reactive<{ notifyConfigure: any[] }>({ notifyConfigure: [] })

/** 触发场景（服务端枚举：CREATED/UPDATED/DELETED/DYNAMIC/SYNCDATA） */
const scenarios = [
  { key: 'CREATED', icon: CirclePlus, nameKey: 'notify.text.created', tipKey: 'notify.tip.created' },
  { key: 'UPDATED', icon: Pencil, nameKey: 'notify.text.updated', tipKey: 'notify.tip.updated' },
  { key: 'DELETED', icon: Trash2, nameKey: 'notify.text.deleted', tipKey: 'notify.tip.deleted' },
  { key: 'DYNAMIC', icon: BellRing, nameKey: 'notify.text.dynamic', tipKey: 'notify.tip.dynamic' },
  { key: 'SYNCDATA', icon: RefreshCw, nameKey: 'notify.text.syncData', tipKey: 'notify.tip.syncData' }
]

const channels = computed(() => formState.notifyConfigure)
const batchVisible = ref(false)
const batchChannels = ref<string[]>([])
const batchScenarios = ref<string[]>([])

const scenarioVisible = ref(false)
const scenarioKey = ref('')
const scenarioChannels = ref<string[]>([])

const channelIcon = (type: string): any => {
  if (type === 'Internal') {
    return Mail
  }
  if (type === 'DingTalk') {
    return MessageCircle
  }
  return Plug
}

const channelName = (type: string): string => {
  const key = `notify.text.${ type.toLowerCase() }`
  const value = t(key)
  return value === key ? type : value
}

const channelDesc = (type: string): string => {
  const key = `notify.tip.${ type.toLowerCase() }`
  const value = t(key)
  return value === key ? t('notify.tip.channelFallback') : value
}

const scenarioName = (key: string): string => {
  const scenario = scenarios.find(item => item.key === key)
  return scenario ? t(scenario.nameKey) : key
}

const isScenarioOn = (key: string): boolean => {
  return channels.value.some(channel => (channel.services || []).includes(key))
}

const handlerInitialize = () => {
  loading.value = true
  const axios = new HttpUtils().getAxios()

  axios.all([UserService.getInfo(), PluginService.getPlugins()])
       .then(axios.spread((infoResponse: any, plugin: any) => {
         formState.notifyConfigure = [{ type: 'Internal', enabled: false, services: [] }]

         const pluginNames: string[] = []
         if (plugin.status && plugin.data && Array.isArray(plugin.data)) {
           plugin.data
                 .filter((v: { type: string }) => v.type === 'NOTIFY')
                 .forEach((item: { name: string }) => {
                   if (item?.name && !pluginNames.includes(item.name)) {
                     pluginNames.push(item.name)
                     formState.notifyConfigure.push({ type: item.name, enabled: false, services: [] })
                   }
                 })
         }

         if (infoResponse.status && infoResponse.data) {
           const serverConfigure = infoResponse.data.notifyConfigure
           if (serverConfigure && Array.isArray(serverConfigure)) {
             serverConfigure.forEach((serverConfig: any) => {
               if (!serverConfig?.type) {
                 return
               }
               const exists = formState.notifyConfigure.find(c => c.type === serverConfig.type)
               if (exists) {
                 exists.enabled = !!serverConfig.enabled
                 exists.services = serverConfig.services || []
               }
               else {
                 formState.notifyConfigure.push({ ...serverConfig, services: serverConfig.services || [] })
               }
             })
           }

           // 清理历史脏数据：只保留内置渠道与已安装的通知插件
           const validTypes = new Set(formState.notifyConfigure
             .filter(channel => channel.type === 'Internal' || pluginNames.includes(channel.type))
             .map(channel => channel.type))
           const removed = formState.notifyConfigure.filter(channel => !validTypes.has(channel.type))
           formState.notifyConfigure = formState.notifyConfigure.filter(channel => validTypes.has(channel.type))
           if (removed.length > 0) {
             save(true)
           }
         }
         else if (infoResponse.message) {
           MfMessage.error(infoResponse.message)
         }
       }))
       .finally(() => (loading.value = false))
}

const save = (silent = false) => {
  saving.value = true
  UserService.changeNotify(formState)
             .then(response => {
               if (response.status) {
                 if (!silent) {
                   MfMessage.success(t('common.successfully') as string)
                 }
               }
               else {
                 MfMessage.error(response.message)
               }
             })
             .finally(() => (saving.value = false))
}

const toggleChannel = (channel: any, enabled: boolean) => {
  channel.enabled = enabled
  save()
}

const toggleScenario = (key: string, checked: boolean) => {
  formState.notifyConfigure.forEach(channel => {
    const services = new Set(channel.services || [])
    if (checked) {
      services.add(key)
    }
    else {
      services.delete(key)
    }
    channel.services = Array.from(services)
  })
  save()
}

const openBatch = () => {
  batchChannels.value = channels.value.map(channel => channel.type)
  batchScenarios.value = scenarios.filter(scenario => isScenarioOn(scenario.key)).map(scenario => scenario.key)
  batchVisible.value = true
}

const applyBatch = () => {
  formState.notifyConfigure.forEach(channel => {
    if (batchChannels.value.includes(channel.type)) {
      channel.enabled = true
      channel.services = [...batchScenarios.value]
    }
  })
  batchVisible.value = false
  save()
}

const openScenarioChannels = (key: string) => {
  scenarioKey.value = key
  scenarioChannels.value = channels.value
      .filter(channel => (channel.services || []).includes(key))
      .map(channel => channel.type)
  scenarioVisible.value = true
}

const applyScenarioChannels = () => {
  formState.notifyConfigure.forEach(channel => {
    const services = new Set(channel.services || [])
    if (scenarioChannels.value.includes(channel.type)) {
      services.add(scenarioKey.value)
    }
    else {
      services.delete(scenarioKey.value)
    }
    channel.services = Array.from(services)
  })
  scenarioVisible.value = false
  save()
}

handlerInitialize()
</script>

<style scoped>
.dc-notify__pagehead {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 24px;
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

.dc-notify__slogan {
    font-size: 14px;
    font-style: italic;
    color: var(--dc-primary);
    opacity: 0.7;
    flex-shrink: 0;
    transform: rotate(-2deg);
}

.dc-notify__card {
    background: var(--dc-surface);
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-lg);
    box-shadow: var(--dc-shadow-card);
    padding: 20px;
    margin-bottom: 16px;
}

.dc-notify__section-head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
}

.dc-notify__section-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    border-radius: var(--dc-radius-md);
    background: var(--dc-primary-light);
    color: var(--dc-primary);
    flex-shrink: 0;
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

.dc-notify__channel-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 12px;
}

.dc-notify__channel {
    padding: 14px;
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-md);
    background: var(--dc-bg);
}

.dc-notify__channel-icon {
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

.dc-notify__channel-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-notify__channel-desc {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 4px;
}

.dc-notify__channel-state {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 12px;
}

.dc-notify__channel-state--on {
    color: var(--dc-success);
}

.dc-notify__channel-state--off {
    color: var(--dc-text-disabled);
}

.dc-notify__rows {
    display: flex;
    flex-direction: column;
}

.dc-notify__row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 4px;
    border-bottom: 1px solid var(--dc-border-light);
}

.dc-notify__row:last-child {
    border-bottom: none;
}

.dc-notify__row-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    border-radius: var(--dc-radius-md);
    background: var(--dc-primary-light);
    color: var(--dc-primary);
    flex-shrink: 0;
}

.dc-notify__row-name {
    font-size: 14px;
    color: var(--dc-text-title);
}

.dc-notify__row-desc {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 2px;
}

.dc-notify__batch-section {
    margin-bottom: 14px;
}

.dc-notify__batch-label {
    font-size: 13px;
    font-weight: 600;
    color: var(--dc-text-title);
    margin-bottom: 8px;
}

.dc-notify__batch-group {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 16px;
}

.dc-notify__batch-tip {
    font-size: 12px;
    color: var(--dc-text-secondary);
}
</style>
