<template>
  <div ref="workspaceRoot" class="source-workspace flex h-full min-h-0 w-full overflow-hidden">
    <aside :style="{ width: leftWidth + 'px' }" class="h-full shrink-0 overflow-hidden rounded-mf-lg bg-surface/90">
      <MetadataSidebar v-if="can('table:list') || capabilityLoading"/>
      <NativeManagementSidebar v-else-if="can('namespace:list')"/>
      <MfEmpty v-else class="h-full px-3" size="sm" variant="initial" :title="$t('source.manage.kind.' + (profile?.kind || 'unknown'))" :description="$t('source.manage.nativeHint')"/>
    </aside>
    <MfResizeHandle v-model="leftWidth" :min="limits.leftMin" :max="limits.leftMax" :label="$t('source.workspace.resizeSidebar')" @change="saveLayout"/>
    <div class="h-full min-w-0 flex-1 overflow-hidden rounded-mf-lg bg-surface/95">
      <MetadataContent/>
    </div>
    <MfResizeHandle v-show="fieldPanelOpen" v-model="rightWidth" reverse :min="limits.rightMin" :max="limits.rightMax" :label="$t('source.workspace.resizeEditor')" @change="saveLayout"/>
    <div v-show="fieldPanelOpen" ref="fieldPanelHost" :style="{ width: rightWidth + 'px' }" class="source-column-panel isolate h-full shrink-0 overflow-hidden rounded-mf-lg bg-surface/95"/>
  </div>
</template>

<script setup lang="ts">
import NativeManagementSidebar from './components/NativeManagementSidebar.vue'
import MetadataSidebar from '@/views/layouts/metadata/components/MetadataSidebar.vue'
import MetadataContent from '@/views/layouts/metadata/components/MetadataContent.vue'
import { useMetadataWorkspace } from '@/composables/useMetadataWorkspace'
import { MfResizeHandle } from 'metaforge-ui'

const { workspaceRoot, leftWidth, rightWidth, limits, saveLayout, fieldPanelOpen, fieldPanelHost, can, profile, capabilityLoading } = useMetadataWorkspace()
</script>

<style scoped>
.source-workspace :deep(*) {
  scrollbar-width: thin;
  scrollbar-color: color-mix(in srgb, var(--mf-color-content-tertiary) 40%, transparent) transparent;
}
.source-workspace :deep(*::-webkit-scrollbar) { width: 6px; height: 6px; }
.source-workspace :deep(*::-webkit-scrollbar-track) { background: transparent; }
.source-workspace :deep(*::-webkit-scrollbar-thumb) {
  background: color-mix(in srgb, var(--mf-color-content-tertiary) 40%, transparent);
  border-radius: var(--mf-radius-full);
}
/* The panel host is already a card, so the docked drawer drops its own divider and surface. */
.source-column-panel :deep(section[role='dialog']) { border: 0; background: transparent; }
</style>
