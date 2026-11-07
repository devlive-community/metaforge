<template>
  <MfDropdown placement="bottomRight" trigger="click">
    <button type="button" class="inline-flex items-center gap-1.5 rounded-mf-md px-2 py-1 text-sm text-content-secondary outline-none hover:text-content focus-visible:ring-2 focus-visible:ring-primary/40"
            aria-haspopup="menu">
      <Globe :size="16"/>{{ LANGUAGES.find(item => item.value === locale)?.label }}<ChevronDown :size="14"/>
    </button>
    <template #overlay>
      <MfMenu class="min-w-32">
        <MfMenuItem v-for="item in LANGUAGES" :key="item.value" @click="loadLocale(item.value)">
          <span class="flex items-center justify-between gap-3">{{ item.label }}<Check v-if="item.value === locale" :size="14" class="text-primary"/></span>
        </MfMenuItem>
      </MfMenu>
    </template>
  </MfDropdown>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Check, ChevronDown, Globe } from '@lucide/vue'
import { useI18nHandler } from '@/i18n/I18n'

/** The language menu; rendered inside BaseLayout, which provides the i18n handler. */
const LANGUAGES = [{ value: 'zh-cn', label: '简体中文' }, { value: 'en', label: 'English' }]
const { locale } = useI18n()
const { loadLocale } = useI18nHandler() as { loadLocale: (locale: string) => Promise<boolean> }
</script>
