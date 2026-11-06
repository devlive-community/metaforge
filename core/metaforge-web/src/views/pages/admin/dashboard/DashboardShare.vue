<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import useClipboard from 'vue-clipboard3'
import { Copy, ExternalLink, RefreshCw, Shield, Trash2, User } from '@lucide/vue'
import DashboardService from '@/services/dashboard'
import { screenEmbedCode, screenShareUrl } from '@/router/shared'
import { formatShareTime, isShareExpired, localDate, SHARE_ORIGIN, shareExpiresAt, type ShareExpiry } from '../report/ReportShareUtils'
import type { DashboardGrant, DashboardModel, DashboardShare, DashboardShareMember } from '@/model/dashboard'
import { readDashboardDocument } from '@/model/dashboard-layout'

/**
 * How the owner shares a data screen: with whom and how much (members and every signed-in user),
 * what viewers see (the whole screen or chosen widgets), and a public link for visitors who are not
 * signed in. Editors always see the whole screen; the public link shows what viewers see.
 */
const props = defineProps<{ dashboard: DashboardModel }>()
const emit = defineEmits<{ close: [] }>()
const { t, locale } = useI18n()
const open = ref(true)
const tab = ref<'members' | 'content' | 'public'>('members')
const loading = ref(true), saving = ref(false), rotating = ref(false), failure = ref('')
const share = ref<DashboardShare>({ everyone: null, members: [], widgets: null, publicLink: { enabled: false, embed: false, origins: '' } })
const form = ref<any>()

// The public link as last saved: its address exists only after the first save.
const saved = ref<{ token: string; enabled: boolean; embed: boolean; expiresAt: number | null }>({ token: '', enabled: false, embed: false, expiresAt: null })
const expiry = ref<ShareExpiry>('never'), expiryDate = ref('')
const today = localDate(Date.now())
const shareUrl = computed(() => screenShareUrl(window.location.origin, saved.value.token))
const embedCode = computed(() => screenEmbedCode(window.location.origin, saved.value.token, props.dashboard.name || ''))
const expired = computed(() => saved.value.enabled && isShareExpired(saved.value.expiresAt))
const expiryNote = computed(() => {
  if (!saved.value.enabled || saved.value.expiresAt == null) return ''
  const time = formatShareTime(saved.value.expiresAt)
  return t(expired.value ? 'report.share.expiredAt' : 'report.share.expiresAt', { time })
})
const expiryOptions = computed(() => [
  { label: t('report.share.expiryNever'), value: 'never' },
  ...[1, 7, 30].map(days => ({ label: t('report.share.expiryDays', { days }), value: `${ days }d` })),
  { label: t('report.share.expiryDate'), value: 'date' },
])
const expiryRules = computed(() => [{
  validator: () => {
    if (!share.value.publicLink.enabled || expiry.value !== 'date') return true
    if (!expiryDate.value) return t('report.share.expiryRequired')
    const at = shareExpiresAt('date', expiryDate.value)
    return (at !== null && at > Date.now()) || t('report.share.expiryPast')
  }
}])
const originRules = computed(() => [{
  validator: () => !share.value.publicLink.embed || share.value.publicLink.origins.split(/[\s,]+/).filter(Boolean)
    .every(item => SHARE_ORIGIN.test(item.replace(/\/$/, ''))) || t('report.share.originsInvalid')
}])
function expiresAt(): number | null {
  // An untouched date keeps the exact saved moment instead of moving to the end of that day.
  const kept = saved.value.expiresAt
  return expiry.value === 'date' && kept != null && expiryDate.value === localDate(kept) ? kept : shareExpiresAt(expiry.value, expiryDate.value)
}
function apply(data: any) {
  const members = (data?.members || []).map((member: DashboardShareMember) => ({ ...member, key: memberKey(member) }))
  const link = { ...share.value.publicLink, ...(data?.publicLink || {}) }
  link.origins = link.origins || ''
  share.value = { ...share.value, ...data, members, publicLink: link }
  saved.value = { token: link.token || '', enabled: !!link.enabled, embed: !!link.embed, expiresAt: link.expiresAt ?? null }
  // A preset becomes its concrete date, so saving again does not push the expiry further out.
  expiry.value = saved.value.expiresAt == null ? 'never' : 'date'
  expiryDate.value = saved.value.expiresAt == null ? '' : localDate(saved.value.expiresAt)
}
async function copy(text: string) {
  try { await useClipboard().toClipboard(text); MfMessage.success(t('report.share.copied')) }
  catch { MfMessage.error(t('report.share.copyFailed')) }
}
function preview() { window.open(shareUrl.value, '_blank', 'noopener') }

const grants: DashboardGrant[] = ['VIEW', 'EDIT', 'PUBLISH']
const grantOptions = computed(() => grants.map(value => ({ label: t('dashboard.share.permission.' + value), value })))
const everyoneOptions = computed(() => [{ label: t('dashboard.share.everyoneNone'), value: '' }, ...grantOptions.value])
const everyone = computed({
  get: () => share.value.everyone || '',
  set: value => { share.value.everyone = (value || null) as DashboardGrant | null },
})

// The published widgets the owner can choose for viewers, outermost first; a container brings its children.
const widgets = computed(() => {
  const design = readDashboardDocument(props.dashboard.configure, props.dashboard.name, props.dashboard.description, t).published
  if (!design) return []
  const seen = new Map<string, { id: string; name: string }>()
  for (const board of Object.values(design.boards)) {
    for (const widget of board.widgets) {
      if (!widget.parentId && !seen.has(widget.id)) seen.set(widget.id, { id: widget.id, name: widget.name || t('dashboard.share.unnamedWidget') })
    }
  }
  return [...seen.values()]
})
const published = computed(() => widgets.value.length > 0)
const scope = computed({
  get: () => share.value.widgets === null ? 'all' : 'chosen',
  set: value => { share.value.widgets = value === 'all' ? null : (share.value.widgets ?? []) },
})
function toggleWidget(id: string, checked: boolean) {
  const current = new Set(share.value.widgets ?? [])
  if (checked) current.add(id)
  else current.delete(id)
  share.value.widgets = [...current]
}

// Candidates come from the server as the owner types.
const candidates = ref<DashboardShareMember[]>([])
const searching = ref(false)
let searchTicket = 0, searchTimer: ReturnType<typeof setTimeout> | undefined
async function search(keyword = '') {
  const ticket = ++searchTicket
  searching.value = true
  try {
    const response = await DashboardService.shareCandidates(keyword)
    if (ticket === searchTicket && response.status) candidates.value = response.data || []
  } finally { if (ticket === searchTicket) searching.value = false }
}
function onSearch(keyword: string) { clearTimeout(searchTimer); searchTimer = setTimeout(() => search(keyword), 250) }
const memberKey = (member: Pick<DashboardShareMember, 'type' | 'id'>) => `${ member.type }:${ member.id }`
const candidateOptions = computed(() => candidates.value
  .filter(candidate => !share.value.members.some(member => memberKey(member) === memberKey(candidate)))
  .map(candidate => ({ label: `${ t('dashboard.share.type.' + candidate.type) } · ${ candidate.name }`, value: memberKey(candidate) })))
function addMember(key: unknown) {
  const candidate = candidates.value.find(item => memberKey(item) === key)
  if (candidate) share.value.members = [...share.value.members, { ...candidate, key: memberKey(candidate), permission: 'VIEW' }]
}
function removeMember(member: DashboardShareMember) { share.value.members = share.value.members.filter(item => item !== member) }
const memberColumns = computed(() => [
  { title: t('dashboard.share.member'), dataIndex: 'name', key: 'name' },
  { title: t('dashboard.share.permissionLabel'), dataIndex: 'permission', key: 'permission', width: 160 },
  { title: '', dataIndex: 'id', key: 'action', width: 48 },
])

async function load() {
  loading.value = true; failure.value = ''
  try {
    const [response] = await Promise.all([DashboardService.getShare(props.dashboard.code || ''), search()])
    if (!response.status) throw new Error(String(response.message || t('dashboard.share.loadFailed')))
    apply(response.data)
  } catch (error) { failure.value = error instanceof Error ? error.message : t('dashboard.share.loadFailed') }
  finally { loading.value = false }
}
async function validate() {
  try { await form.value?.validate(); return true }
  catch {
    // Show the pane that holds the first problem.
    if (scope.value === 'chosen' && !share.value.widgets?.length) tab.value = 'content'
    else tab.value = 'public'
    return false
  }
}
async function submit(rotate: boolean) {
  const response = await DashboardService.saveShare(props.dashboard.code || '', {
    ...share.value, publicLink: { ...share.value.publicLink, expiresAt: expiresAt(), rotate },
  })
  if (!response.status) throw new Error(String(response.message || t('dashboard.share.saveFailed')))
  apply(response.data)
}
async function save() {
  if (saving.value || rotating.value || !await validate()) return
  // A link turned on for the first time stays on screen so the owner can copy it.
  const fresh = share.value.publicLink.enabled && !(saved.value.enabled && saved.value.token)
  saving.value = true
  try {
    await submit(false)
    MfMessage.success(t('dashboard.share.saved'))
    if (fresh) tab.value = 'public'
    else open.value = false
  } catch (error) { MfMessage.error(error instanceof Error ? error.message : t('dashboard.share.saveFailed')) }
  finally { saving.value = false }
}
async function rotate() {
  if (saving.value || rotating.value || !await validate()) return
  rotating.value = true
  try { await submit(true); MfMessage.success(t('report.share.rotated')) }
  catch (error) { MfMessage.error(error instanceof Error ? error.message : t('dashboard.share.saveFailed')) }
  finally { rotating.value = false }
}
watch(open, value => { if (!value) emit('close') })
void load()
</script>

<template>
  <MfModal v-model:open="open" width="640px" :title="$t('dashboard.share.title', { name: dashboard.name })">
    <div class="flex h-[420px] min-h-0 flex-col">
      <div v-if="loading" class="flex flex-1 items-center justify-center"><MfSpinner/></div>
      <MfAlert v-else-if="failure" variant="danger" :title="failure"><MfButton size="sm" @click="load">{{ $t('common.refresh') }}</MfButton></MfAlert>
      <template v-else>
        <MfTabs v-model:active-key="tab" class="mb-4 shrink-0">
          <MfTabPane item-key="members" :tab="$t('dashboard.share.tab.members')"/>
          <MfTabPane item-key="content" :tab="$t('dashboard.share.tab.content')"/>
          <MfTabPane item-key="public" :tab="$t('dashboard.share.tab.public')"/>
        </MfTabs>
        <MfForm id="dashboard-share" ref="form" :model="share" layout="vertical" class="min-h-0 flex-1 overflow-y-auto" @finish="save">
          <!-- Both panes stay rendered so the widget choice is validated from either tab. -->
          <div v-show="tab === 'members'">
            <MfFormItem :label="$t('dashboard.share.everyone')">
              <div class="w-full">
                <MfSelect v-model="everyone" class="w-full" :options="everyoneOptions"/>
                <p class="mt-1.5 text-[11px] leading-5 text-content-tertiary">{{ $t('dashboard.share.everyoneHint') }}</p>
              </div>
            </MfFormItem>
            <MfFormItem :label="$t('dashboard.share.members')">
              <div class="w-full space-y-3">
                <MfSelect class="w-full" searchable :loading="searching" :options="candidateOptions" :filter-option="() => true" model-value=""
                          :placeholder="$t('dashboard.share.addMember')" @search="onSearch" @update:model-value="addMember"/>
                <MfTable :columns="memberColumns" :data-source="share.members" row-key="key" density="compact" :empty-text="$t('dashboard.share.noMembers')">
                  <template #cell-name="{ record }">
                    <div class="flex min-w-0 items-center gap-2">
                      <component :is="record.type === 'ROLE' ? Shield : User" :size="14" class="shrink-0 text-content-tertiary"/>
                      <span class="truncate">{{ record.name }}</span>
                      <MfTag size="sm">{{ $t('dashboard.share.type.' + record.type) }}</MfTag>
                    </div>
                  </template>
                  <template #cell-permission="{ record }"><MfSelect v-model="record.permission" size="sm" class="w-full" :options="grantOptions"/></template>
                  <template #cell-action="{ record }"><MfButton variant="ghost" size="sm" :aria-label="$t('common.delete')" @click="removeMember(record)"><Trash2 :size="14"/></MfButton></template>
                </MfTable>
              </div>
            </MfFormItem>
          </div>
          <div v-show="tab === 'content'">
            <MfFormItem :label="$t('dashboard.share.visible')">
              <div class="w-full">
                <MfRadioGroup v-model="scope">
                  <MfRadio value="all" :label="$t('dashboard.share.visibleAll')"/>
                  <MfRadio value="chosen" :label="$t('dashboard.share.visibleChosen')" :disabled="!published"/>
                </MfRadioGroup>
                <p class="mt-1.5 text-[11px] leading-5 text-content-tertiary">{{ $t(published ? 'dashboard.share.visibleHint' : 'dashboard.share.unpublishedHint') }}</p>
              </div>
            </MfFormItem>
            <MfFormItem v-if="scope === 'chosen'" name="widgets" :label="$t('dashboard.share.widgets')"
                        :rules="[{ validator: () => (share.widgets?.length ?? 0) > 0 || $t('dashboard.share.widgetsRequired') }]">
              <div class="grid w-full grid-cols-1 gap-2 sm:grid-cols-2">
                <MfCheckbox v-for="widget in widgets" :key="widget.id" :model-value="share.widgets?.includes(widget.id)" :label="widget.name"
                            @update:model-value="toggleWidget(widget.id, !!$event)"/>
              </div>
            </MfFormItem>
          </div>
          <div v-show="tab === 'public'">
            <MfFormItem name="publicEnabled">
              <div class="flex w-full items-center justify-between gap-4">
                <div class="min-w-0">
                  <div class="font-medium text-content">{{ $t('dashboard.share.public.enabled') }}</div>
                  <div class="mt-0.5 text-xs text-content-tertiary">{{ $t(published ? 'dashboard.share.public.enabledHint' : 'dashboard.share.public.unpublished') }}</div>
                </div>
                <MfSwitch v-model="share.publicLink.enabled" class="shrink-0" :disabled="!published && !share.publicLink.enabled"/>
              </div>
            </MfFormItem>
            <template v-if="share.publicLink.enabled">
              <MfFormItem v-if="saved.token && saved.enabled" :label="$t('report.share.link')" name="publicLink">
                <div class="w-full min-w-0">
                  <MfInput :model-value="shareUrl" readonly class="w-full"/>
                  <div class="mt-2 flex flex-wrap items-center gap-2">
                    <MfButton size="sm" class="shrink-0" @click="copy(shareUrl)"><Copy :size="14"/>{{ $t('report.share.copy') }}</MfButton>
                    <MfButton size="sm" class="shrink-0" :disabled="expired" @click="preview"><ExternalLink :size="14"/>{{ $t('report.share.preview') }}</MfButton>
                    <MfButton size="sm" class="shrink-0" :loading="rotating" :disabled="saving" @click="rotate"><RefreshCw :size="14"/>{{ $t('report.share.rotate') }}</MfButton>
                    <span v-if="expiryNote" class="min-w-0 text-xs" :class="expired ? 'text-warning' : 'text-content-tertiary'">{{ expiryNote }}</span>
                  </div>
                  <p class="mt-1.5 text-[11px] leading-5 text-content-tertiary">{{ $t('dashboard.share.public.contentHint') }}</p>
                </div>
              </MfFormItem>
              <p v-else class="mb-4 text-xs text-content-tertiary">{{ $t('report.share.linkAfterSave') }}</p>
              <MfFormItem :label="$t('report.share.expiry')" name="expiryDate" :rules="expiryRules">
                <div class="flex flex-wrap items-center gap-2">
                  <MfSelect v-model="expiry" :options="expiryOptions" class="w-40"/>
                  <MfDatePicker v-if="expiry === 'date'" v-model="expiryDate" class="w-48" :min="today" :locale="locale"
                                :label="$t('report.share.expiryDate')" :placeholder="$t('dataTable.selectDate')" :today-text="$t('dataTable.today')"
                                :clear-text="$t('dataTable.clear')" :previous-text="$t('dataTable.previousMonth')" :next-text="$t('dataTable.nextMonth')"/>
                </div>
              </MfFormItem>
              <MfFormItem name="publicEmbed">
                <div class="flex w-full items-center justify-between gap-4">
                  <div class="min-w-0">
                    <div class="font-medium text-content">{{ $t('report.share.embed') }}</div>
                    <div class="mt-0.5 text-xs text-content-tertiary">{{ $t('report.share.embedHint') }}</div>
                  </div>
                  <MfSwitch v-model="share.publicLink.embed" class="shrink-0"/>
                </div>
              </MfFormItem>
              <template v-if="share.publicLink.embed">
                <MfFormItem :label="$t('report.share.origins')" name="origins" :rules="originRules">
                  <MfTextarea v-model="share.publicLink.origins" :rows="3" :placeholder="$t('report.share.originsPlaceholder')"/>
                </MfFormItem>
                <MfFormItem v-if="saved.token && saved.enabled && saved.embed" :label="$t('report.share.code')" name="code">
                  <MfTextarea :model-value="embedCode" readonly :rows="3"/>
                  <MfButton class="self-start" @click="copy(embedCode)"><Copy :size="15"/>{{ $t('report.share.copyCode') }}</MfButton>
                </MfFormItem>
              </template>
            </template>
          </div>
        </MfForm>
      </template>
    </div>
    <template #footer>
      <MfButton @click="open = false">{{ $t('common.cancel') }}</MfButton>
      <MfButton type="submit" form="dashboard-share" variant="primary" :loading="saving" :disabled="loading || !!failure || rotating">{{ $t('common.save') }}</MfButton>
    </template>
  </MfModal>
</template>
