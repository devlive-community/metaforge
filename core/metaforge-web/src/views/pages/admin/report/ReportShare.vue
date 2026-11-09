<template>
  <MfModal v-model:open="visible" width="600px" :title="$t('report.share.title', { name: info.name })">
    <MfAlert v-if="!shareable" variant="warning" :title="$t('report.share.legacy')"/>

    <MfForm v-else id="report-share" ref="formRef" :model="form" layout="vertical" @finish="onSave">
      <MfFormItem name="enabled">
        <div class="flex w-full items-center justify-between gap-4">
          <div class="min-w-0">
            <div class="font-medium text-content">{{ $t('report.share.public') }}</div>
            <div class="mt-0.5 text-xs text-content-tertiary">{{ $t('report.share.publicHint') }}</div>
          </div>
          <MfSwitch v-model="form.enabled" class="shrink-0"/>
        </div>
      </MfFormItem>

      <template v-if="form.enabled">
        <MfFormItem v-if="state.token" :label="$t('report.share.link')" name="link">
          <div class="w-full min-w-0">
            <MfInput :model-value="shareUrl" readonly class="w-full"/>
            <div class="mt-2 flex flex-wrap items-center gap-2">
              <MfButton size="sm" class="shrink-0" @click="copy(shareUrl)">
                <Copy :size="14"/>{{ $t('report.share.copy') }}
              </MfButton>
              <MfButton size="sm" class="shrink-0" :disabled="expired" @click="preview">
                <ExternalLink :size="14"/>{{ $t('report.share.preview') }}
              </MfButton>
              <MfButton size="sm" class="shrink-0" :loading="rotating" :disabled="saving" @click="onRotate">
                <RefreshCw :size="14"/>{{ $t('report.share.rotate') }}
              </MfButton>
              <span v-if="expiryNote" class="min-w-0 text-xs" :class="expired ? 'text-warning' : 'text-content-tertiary'">{{ expiryNote }}</span>
            </div>
            <p v-if="dirty" class="mt-1.5 text-xs text-content-tertiary">{{ $t('report.share.previewSaved') }}</p>
          </div>
        </MfFormItem>
        <p v-else class="mb-4 text-xs text-content-tertiary">{{ $t('report.share.linkAfterSave') }}</p>

        <MfFormItem :label="$t('report.share.expiry')" name="expiryDate" :rules="expiryRules">
          <div class="flex flex-wrap items-center gap-2">
            <MfSelect v-model="form.expiry" :options="expiryOptions" class="w-40"/>
            <MfDatePicker v-if="form.expiry === 'date'"
                          v-model="form.expiryDate"
                          class="w-48"
                          :min="today"
                          :locale="locale"
                          :label="$t('report.share.expiryDate')"
                          :placeholder="$t('dataTable.selectDate')"
                          :today-text="$t('dataTable.today')"
                          :clear-text="$t('dataTable.clear')"
                          :previous-text="$t('dataTable.previousMonth')"
                          :next-text="$t('dataTable.nextMonth')"/>
          </div>
        </MfFormItem>

        <MfFormItem :label="$t('report.share.display')" name="display">
          <div class="flex flex-wrap items-center gap-x-6 gap-y-2">
            <label class="flex items-center gap-2 text-sm text-content">
              <MfSwitch v-model="form.showTitle" size="sm"/>{{ $t('report.share.showTitle') }}
            </label>
            <label class="flex items-center gap-2 text-sm text-content">
              <MfSwitch v-model="form.showDescription" size="sm"/>{{ $t('report.share.showDescription') }}
            </label>
          </div>
        </MfFormItem>

        <MfFormItem :label="$t('report.share.dataAccess')" name="dataAccess">
          <div class="w-full min-w-0">
            <div class="flex flex-wrap items-center gap-x-6 gap-y-2">
              <label class="flex items-center gap-2 text-sm text-content">
                <MfSwitch v-model="form.allowData" size="sm"/>{{ $t('report.share.allowData') }}
              </label>
              <label class="flex items-center gap-2 text-sm text-content">
                <MfSwitch v-model="form.allowDownload" size="sm"/>{{ $t('report.share.allowDownload') }}
              </label>
            </div>
            <p class="mt-1.5 text-xs text-content-tertiary">{{ $t('report.share.dataAccessHint') }}</p>
          </div>
        </MfFormItem>

        <MfFormItem name="embed">
          <div class="flex w-full items-center justify-between gap-4">
            <div class="min-w-0">
              <div class="font-medium text-content">{{ $t('report.share.embed') }}</div>
              <div class="mt-0.5 text-xs text-content-tertiary">{{ $t('report.share.embedHint') }}</div>
            </div>
            <MfSwitch v-model="form.embed" class="shrink-0"/>
          </div>
        </MfFormItem>

        <template v-if="form.embed">
          <MfFormItem :label="$t('report.share.origins')" name="origins" :rules="originRules">
            <MfTextarea v-model="form.origins" :rows="3" :placeholder="$t('report.share.originsPlaceholder')"/>
          </MfFormItem>
          <MfFormItem v-if="state.token && state.embed" :label="$t('report.share.code')" name="code">
            <MfTextarea :model-value="iframeCode" readonly :rows="3"/>
            <MfButton class="self-start" @click="copy(iframeCode)">
              <Copy :size="15"/>{{ $t('report.share.copyCode') }}
            </MfButton>
          </MfFormItem>
        </template>
      </template>
    </MfForm>

    <template #footer>
      <MfButton class="shrink-0" @click="visible = false">{{ $t('common.cancel') }}</MfButton>
      <MfButton v-if="shareable" class="shrink-0" variant="primary" type="submit" form="report-share" :loading="saving" :disabled="rotating">
        {{ $t('common.save') }}
      </MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import useClipboard from 'vue-clipboard3'
import { Copy, ExternalLink, RefreshCw } from '@lucide/vue'
import ReportService from '@/services/report'
import { ReportModel } from '@/model/report'
import {
  formatShareTime, isShareableReport, isShareExpired, localDate, reportEmbedCode, reportShareUrl, SHARE_ORIGIN, shareExpiresAt, type ShareExpiry
} from './ReportShareUtils'

defineOptions({ name: 'ReportShare' })

const props = defineProps<{ isVisible: boolean; info: ReportModel }>()
const emit = defineEmits<{ (e: 'close', changed: boolean): void }>()
const { t, locale } = useI18n()

let changed = false
const visible = computed({
  get: () => props.isVisible,
  set: () => emit('close', changed)
})

const shareable = computed(() => isShareableReport(props.info))
const state = reactive({
  token: props.info.shareToken ?? '',
  enabled: !!props.info.shareEnabled,
  embed: !!props.info.shareEmbed,
  expiresAt: props.info.shareExpiresAt ?? null as number | null,
})
// A saved expiry reopens as its date, so the exact moment stays visible below the link.
const form = reactive({
  enabled: !!props.info.shareEnabled,
  embed: !!props.info.shareEmbed,
  origins: props.info.shareOrigins ?? '',
  expiry: (props.info.shareExpiresAt ? 'date' : 'never') as ShareExpiry,
  expiryDate: props.info.shareExpiresAt ? localDate(props.info.shareExpiresAt) : '',
  showTitle: props.info.shareShowTitle !== false,
  showDescription: props.info.shareShowDescription !== false,
  allowData: !!props.info.shareAllowData,
  allowDownload: !!props.info.shareAllowDownload,
})
const saved = ref(JSON.stringify(form))
// The preview shows the saved settings; say so while the form has unsaved changes.
const dirty = computed(() => JSON.stringify(form) !== saved.value)
const formRef = ref()
const saving = ref(false)
const rotating = ref(false)
const today = localDate(Date.now())

const shareUrl = computed(() => reportShareUrl(window.location.origin, state.token))
const iframeCode = computed(() => reportEmbedCode(window.location.origin, state.token, props.info.name ?? ''))
const expired = computed(() => state.enabled && isShareExpired(state.expiresAt))
const expiryNote = computed(() => {
  if (!state.enabled || state.expiresAt == null) return ''
  const time = formatShareTime(state.expiresAt)
  return expired.value ? t('report.share.expiredAt', { time }) : t('report.share.expiresAt', { time })
})

const expiryOptions = computed(() => [
  { label: t('report.share.expiryNever'), value: 'never' },
  { label: t('report.share.expiryDays', { days: 1 }), value: '1d' },
  { label: t('report.share.expiryDays', { days: 7 }), value: '7d' },
  { label: t('report.share.expiryDays', { days: 30 }), value: '30d' },
  { label: t('report.share.expiryDate'), value: 'date' },
])

const expiryRules = computed(() => [{
  validator: () => {
    if (form.expiry !== 'date') return true
    if (!form.expiryDate) return t('report.share.expiryRequired')
    const at = shareExpiresAt('date', form.expiryDate)
    return (at !== null && at > Date.now()) || t('report.share.expiryPast')
  }
}])

const originRules = computed(() => [{
  validator: (value: unknown) => String(value ?? '').split(/[\s,]+/).filter(Boolean).every(item => SHARE_ORIGIN.test(item.replace(/\/$/, '')))
      || t('report.share.originsInvalid')
}])

async function submit(rotate: boolean): Promise<boolean>
{
  const response = await ReportService.share(props.info.code as string, {
    enabled: form.enabled,
    embed: form.embed,
    origins: form.origins,
    rotate,
    // An untouched date keeps the exact saved moment instead of moving to the end of that day.
    expiresAt: form.expiry === 'date' && state.expiresAt != null && form.expiryDate === localDate(state.expiresAt)
        ? state.expiresAt : shareExpiresAt(form.expiry, form.expiryDate),
    showTitle: form.showTitle,
    showDescription: form.showDescription,
    allowData: form.allowData,
    allowDownload: form.allowDownload,
  })
  if (!response.status) {
    MfMessage.error(response.message || t('report.share.failed'))
    return false
  }
  Object.assign(state, { ...response.data, expiresAt: response.data.expiresAt ?? null })
  form.origins = response.data.origins ?? ''
  // A preset becomes its concrete date, so saving again does not push the expiry further out.
  if (state.expiresAt != null) {
    form.expiry = 'date'
    form.expiryDate = localDate(state.expiresAt)
  }
  changed = true
  saved.value = JSON.stringify(form)
  return true
}

function preview()
{
  window.open(shareUrl.value, '_blank', 'noopener')
}

async function onSave()
{
  saving.value = true
  try {
    if (await submit(false)) MfMessage.success(t('report.share.saved'))
  }
  finally {
    saving.value = false
  }
}

async function onRotate()
{
  if (await formRef.value?.validate?.() === false) return
  rotating.value = true
  try {
    if (await submit(true)) MfMessage.success(t('report.share.rotated'))
  }
  finally {
    rotating.value = false
  }
}

async function copy(text: string)
{
  try {
    await useClipboard().toClipboard(text)
    MfMessage.success(t('report.share.copied'))
  }
  catch {
    MfMessage.error(t('report.share.copyFailed'))
  }
}
</script>
