<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Ellipsis, Eye, House, Pencil, Share2, Trash2 } from '@lucide/vue'
import type { ScreenEntry } from '@/model/screen-entry'
defineProps<{ screen: ScreenEntry; homePage?: string | null; homeBusy?: string }>()
const emit = defineEmits<{ home: []; share: []; remove: [] }>()
const router = useRouter()
</script>

<template>
  <div class="flex shrink-0 items-center gap-1">
    <MfButton size="sm" variant="ghost" :aria-label="$t(screen.published || !screen.editable ? 'screenLibrary.view' : 'screenLibrary.continueEdit') + ' · ' + screen.name" @click="router.push(screen.link)">
      <Eye v-if="screen.published || !screen.editable" :size="14"/><Pencil v-else :size="14"/>
      {{ $t(screen.published || !screen.editable ? 'screenLibrary.view' : 'screenLibrary.continueEdit') }}
    </MfButton>
    <MfButton v-if="screen.editable && screen.published" size="sm" variant="ghost" :aria-label="$t('common.edit') + ' · ' + screen.name" @click="router.push(screen.editor)"><Pencil :size="14"/>{{ $t('common.edit') }}</MfButton>
    <MfDropdown v-if="screen.own || screen.published" trigger="click" placement="bottomRight">
      <MfButton size="sm" variant="ghost" :loading="homeBusy === screen.code" :aria-label="$t('screenLibrary.more') + ' · ' + screen.name"><Ellipsis :size="16"/></MfButton>
      <template #overlay>
        <MfMenu>
          <MfMenuItem v-if="screen.published" :disabled="!!homeBusy" @click="emit('home')"><template #icon><House :size="15"/></template>{{ $t(screen.code === homePage ? 'dashboard.home.unset' : 'dashboard.home.set') }}</MfMenuItem>
          <MfMenuItem v-if="screen.own" @click="emit('share')"><template #icon><Share2 :size="15"/></template>{{ $t('dashboard.share.action') }}</MfMenuItem>
          <MfMenuItem v-if="screen.own" danger @click="emit('remove')"><template #icon><Trash2 :size="15"/></template>{{ $t('common.delete') }}</MfMenuItem>
        </MfMenu>
      </template>
    </MfDropdown>
  </div>
</template>
