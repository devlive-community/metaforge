<template>
  <MfSelect v-model="language"
            :options="languageOptions"
            :placeholder="$t('region.common.selectLanguage')"
            style="width: 120px;"
            @change="changeLanguage"/>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useI18nHandler } from '@/i18n/I18n'

// @ts-ignore
const { loadLocale } = useI18nHandler()
const { t } = useI18n()
const language = ref('language_zh-cn')

// a-select-opt-group → option 携带 group 字段
const languageOptions = computed(() => [
  { value: 'language_zh-cn', label: t('region.common.asia.chineseSimple'), group: t('region.common.asia.default') },
  { value: 'language_en', label: t('region.common.northAmerica.english'), group: t('region.common.northAmerica.default') }
])

const changeLanguage = async (value: string | number | (string | number)[]) => {
  const prefix = 'language_'
  if (typeof value === 'string' && value.startsWith(prefix)) {
    const locale = value.substring(prefix.length)
    await loadLocale(locale)
    language.value = value
  }
}

onMounted(() => {
  // 历史存储值可能是 zh_cn 下划线格式，归一化为下拉选项的 language_zh-cn
  const currentLocale = (localStorage.getItem('locale') || '').replace('_', '-')
  if (currentLocale) {
    language.value = `language_${ currentLocale }`
  }
})
</script>
