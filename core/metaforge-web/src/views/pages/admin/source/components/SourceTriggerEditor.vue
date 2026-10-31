<template>
  <MfDrawer :open="open" inline :container="container || undefined" :width="width + 'px'"
            :title="$t(mode === 'edit' ? 'source.trg.editTitle' : 'source.trg.createTitle')" :closable="!saving" @update:open="value => !value && emit('close')">
    <form class="flex flex-col gap-4" @submit.prevent="emit('save')">
      <div class="flex flex-col gap-1.5">
        <label :for="`${uid}-name`" class="text-sm text-content">{{ $t('source.trg.name') }} <span class="text-danger">*</span></label>
        <MfInput :id="`${uid}-name`" v-model="form.name" size="sm" :disabled="saving" :invalid="!!issues.name"/>
        <p v-if="issues.name" role="alert" class="m-0 text-xs text-danger">{{ $t(`source.trg.issue.${issues.name}`) }}</p>
        <p v-else class="m-0 text-xs text-content-tertiary">{{ $t('source.trg.nameHint') }}</p>
      </div>
      <div class="flex flex-col gap-1.5">
        <span class="text-sm text-content">{{ $t('source.trg.table') }} <span class="text-danger">*</span></span>
        <MfSelect :model-value="table" size="sm" :options="[{ label: table, value: table }]" disabled :aria-label="$t('source.trg.table')"/>
      </div>
      <div class="grid grid-cols-2 gap-3">
        <div class="flex min-w-0 flex-col gap-1.5">
          <span class="text-sm text-content">{{ $t('source.trg.timing') }} <span class="text-danger">*</span></span>
          <MfSelect v-model="form.timing" size="sm" :options="TRIGGER_TIMINGS.map(value => ({ label: value, value }))" :disabled="saving" :aria-label="$t('source.trg.timing')"/>
        </div>
        <div class="flex min-w-0 flex-col gap-1.5">
          <span class="text-sm text-content">{{ $t('source.trg.event') }} <span class="text-danger">*</span></span>
          <MfSelect v-model="form.event" size="sm" :options="TRIGGER_EVENTS.map(value => ({ label: value, value }))" :disabled="saving" :aria-label="$t('source.trg.event')"/>
        </div>
      </div>
      <div class="flex flex-col gap-1.5">
        <label :for="`${uid}-definer`" class="text-sm text-content">{{ $t('source.trg.definer') }}</label>
        <MfInput :id="`${uid}-definer`" v-model="form.definer" size="sm" placeholder="user@host" :disabled="saving"/>
        <p class="m-0 text-xs text-content-tertiary">{{ $t('source.trg.definerHint') }}</p>
      </div>
      <div class="flex flex-col gap-1.5">
        <label :for="`${uid}-body`" class="text-sm text-content">{{ $t('source.trg.body') }} <span class="text-danger">*</span></label>
        <SqlCodeEditor :id="`${uid}-body`" v-model="form.body" :label="$t('source.trg.body')" :disabled="saving" :invalid="!!issues.body"/>
        <p v-if="issues.body" role="alert" class="m-0 text-xs text-danger">{{ $t(`source.trg.issue.${issues.body}`) }}</p>
        <p v-else class="m-0 text-xs text-content-tertiary">{{ $t('source.trg.bodyHint') }}</p>
      </div>
      <div class="flex flex-col gap-1.5">
        <span class="text-sm font-medium text-content">{{ $t('source.idx.generatedSql') }}</span>
        <SqlCodeBlock :code="previewSql" :placeholder="previewError || $t('source.trg.previewHint')" compact max-height="max-h-64"/>
      </div>
    </form>
    <template #footer>
      <MfButton size="sm" :disabled="saving" @click="emit('close')">{{ $t('common.cancel') }}</MfButton>
      <MfButton size="sm" variant="primary" :loading="saving" @click="emit('save')">{{ $t(mode === 'edit' ? 'common.save' : 'source.prototype.confirm') }}</MfButton>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { getCurrentInstance } from 'vue'
import SqlCodeBlock from '@/views/components/sql/SqlCodeBlock.vue'
import SqlCodeEditor from '@/views/components/sql/SqlCodeEditor.vue'
import { TRIGGER_EVENTS, TRIGGER_TIMINGS, type TriggerForm, type TriggerIssue } from '@/model/source-trigger'

const form = defineModel<TriggerForm>({ required: true })
defineProps<{
  open: boolean; mode: 'add' | 'edit'; table: string; issues: Partial<Record<'name' | 'body', TriggerIssue>>
  previewSql: string; previewError: string; saving: boolean; width: number; container: HTMLElement | null
}>()
const emit = defineEmits<{ close: []; save: [] }>()
const uid = `source-trigger-${getCurrentInstance()!.uid}`
</script>
