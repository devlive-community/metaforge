<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { Shield, Trash2, User } from '@lucide/vue'
import DashboardService from '@/services/dashboard'
import type { DashboardGrant, DashboardModel, DashboardShare, DashboardShareMember } from '@/model/dashboard'
import { readDashboardDocument } from '@/model/dashboard-layout'

/**
 * How the owner shares a data screen: with whom and how much (members and every signed-in user),
 * and what viewers see (the whole screen or chosen widgets). Editors always see the whole screen.
 */
const props = defineProps<{ dashboard: DashboardModel }>()
const emit = defineEmits<{ close: [] }>()
const { t } = useI18n()
const open = ref(true)
const tab = ref<'members' | 'content'>('members')
const loading = ref(true), saving = ref(false), failure = ref('')
const share = ref<DashboardShare>({ everyone: null, members: [], widgets: null, publicLink: { enabled: false, embed: false, origins: '' } })
const form = ref<any>()

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
    const members = (response.data?.members || []).map((member: DashboardShareMember) => ({ ...member, key: memberKey(member) }))
    share.value = { ...share.value, ...response.data, members, publicLink: { ...share.value.publicLink, ...(response.data?.publicLink || {}) } }
  } catch (error) { failure.value = error instanceof Error ? error.message : t('dashboard.share.loadFailed') }
  finally { loading.value = false }
}
async function save() {
  if (saving.value) return
  try { await form.value?.validate() } catch { tab.value = 'content'; return }
  saving.value = true
  try {
    const response = await DashboardService.saveShare(props.dashboard.code || '', { ...share.value, publicLink: { ...share.value.publicLink, rotate: false } })
    if (!response.status) throw new Error(String(response.message || t('dashboard.share.saveFailed')))
    MfMessage.success(t('dashboard.share.saved'))
    open.value = false
  } catch (error) { MfMessage.error(error instanceof Error ? error.message : t('dashboard.share.saveFailed')) }
  finally { saving.value = false }
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
        </MfForm>
      </template>
    </div>
    <template #footer>
      <MfButton @click="open = false">{{ $t('common.cancel') }}</MfButton>
      <MfButton type="submit" form="dashboard-share" variant="primary" :loading="saving" :disabled="loading || !!failure">{{ $t('common.save') }}</MfButton>
    </template>
  </MfModal>
</template>
