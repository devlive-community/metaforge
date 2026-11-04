<template>
  <MfModal v-model:open="visible" width="560px" :title="$t('report.share.title', { name: info.name })">
    <MfAlert v-if="!shareable" variant="warning" :title="$t('report.share.legacy')"/>

    <MfForm v-else id="report-share" ref="formRef" :model="form" layout="vertical" @finish="onSave">
      <MfFormItem name="enabled">
        <div class="flex items-center justify-between gap-4">
          <div class="min-w-0">
            <div class="font-medium text-content">{{ $t('report.share.public') }}</div>
            <div class="mt-0.5 text-xs text-content-tertiary">{{ $t('report.share.publicHint') }}</div>
          </div>
          <MfSwitch v-model="form.enabled" class="shrink-0"/>
        </div>
      </MfFormItem>

      <template v-if="form.enabled">
        <MfFormItem v-if="state.token" :label="$t('report.share.link')" name="link">
          <div class="flex items-center gap-2">
            <MfInput :model-value="shareUrl" readonly class="min-w-0 flex-1"/>
            <MfButton class="shrink-0" @click="copy(shareUrl)">
              <Copy :size="15"/>{{ $t('report.share.copy') }}
            </MfButton>
            <MfButton class="shrink-0" :loading="rotating" :disabled="saving" @click="onRotate">
              <RefreshCw :size="15"/>{{ $t('report.share.rotate') }}
            </MfButton>
          </div>
        </MfFormItem>
        <p v-else class="mb-4 text-xs text-content-tertiary">{{ $t('report.share.linkAfterSave') }}</p>

        <MfFormItem name="embed">
          <div class="flex items-center justify-between gap-4">
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
            <MfButton class="mt-2" @click="copy(iframeCode)">
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
import { Copy, RefreshCw } from '@lucide/vue'
import ReportService from '@/services/report'
import { ReportModel } from '@/model/report'
import { isShareableReport, reportEmbedCode, reportShareUrl, SHARE_ORIGIN } from './ReportShareUtils'

defineOptions({ name: 'ReportShare' })

const props = defineProps<{ isVisible: boolean; info: ReportModel }>()
const emit = defineEmits<{ (e: 'close', changed: boolean): void }>()
const { t } = useI18n()

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
})
const form = reactive({
  enabled: !!props.info.shareEnabled,
  embed: !!props.info.shareEmbed,
  origins: props.info.shareOrigins ?? '',
})
const formRef = ref()
const saving = ref(false)
const rotating = ref(false)

const shareUrl = computed(() => reportShareUrl(window.location.origin, state.token))
const iframeCode = computed(() => reportEmbedCode(window.location.origin, state.token, props.info.name ?? ''))

const originRules = computed(() => [{
  validator: (value: unknown) => String(value ?? '').split(/[\s,]+/).filter(Boolean).every(item => SHARE_ORIGIN.test(item.replace(/\/$/, '')))
      || t('report.share.originsInvalid')
}])

async function submit(rotate: boolean): Promise<boolean>
{
  const response = await ReportService.share(props.info.code as string, { ...form, rotate })
  if (!response.status) {
    MfMessage.error(response.message || t('report.share.failed'))
    return false
  }
  Object.assign(state, response.data)
  form.origins = response.data.origins ?? ''
  changed = true
  return true
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
