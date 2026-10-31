<template>
  <div class="flex h-full min-h-0 flex-col p-3">
    <div class="mb-3 flex shrink-0 items-center gap-2 text-sm font-semibold text-content">
      <Cloud v-if="storage" :size="17" class="text-primary"/><Database v-else :size="17" class="text-primary"/>
      {{ $t(storage ? 'source.native.buckets' : 'source.native.databases') }}
    </div>
    <MfInput v-if="storage" v-model="filter" size="sm" class="mb-3 shrink-0" :placeholder="$t('source.native.searchBuckets')" @keydown.enter="load()">
      <template #suffix><button type="button" :aria-label="$t('common.search')" @click="load()"><Search :size="14"/></button></template>
    </MfInput>
    <MfAlert v-if="error" variant="danger" :title="error" class="mb-2 shrink-0"/>
    <div class="min-h-0 flex-1 overflow-auto">
      <div v-if="loading && !entries.length" class="flex justify-center py-8"><MfSpinner/></div>
      <div v-else-if="entries.length" class="flex flex-col gap-1">
        <button v-for="entry in entries" :key="entry.id" type="button" class="flex w-full items-center gap-2 rounded-mf-md px-3 py-2 text-left text-sm transition-colors hover:bg-surface-hover" :class="entry.id === selected && 'bg-primary/10 font-medium text-primary'" :aria-current="entry.id === selected ? 'true' : undefined" @click="select(entry.id)">
          <Cloud v-if="storage" :size="15" class="shrink-0"/><Database v-else :size="15" class="shrink-0"/>
          <EllipsisCell :text="entry.name"/>
        </button>
      </div>
      <MfEmpty v-else class="h-full" size="sm" :title="$t('common.noData')"/>
    </div>
    <MfButton v-if="hasMore" size="sm" class="mt-2 shrink-0" :loading="loading" @click="load(true)">{{ $t('source.native.loadMore') }}</MfButton>
  </div>
</template>
<script setup lang="ts">
import { computed, inject } from 'vue'
import { Cloud, Database, Search } from '@lucide/vue'
import type { useNativeNamespaces } from '@/composables/useNativeNamespaces'
import type { useSourceCapabilities } from '@/composables/useSourceCapabilities'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
const { profile } = inject<ReturnType<typeof useSourceCapabilities>>('metadataCapabilityState')!
const storage = computed(() => profile.value?.kind === 'storage')
const { entries, filter, loading, error, hasMore, selected, select, load } = inject<ReturnType<typeof useNativeNamespaces>>('nativeNamespaceState')!
</script>
