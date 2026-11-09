<template>
  <MfDrawer v-model:open="visible" :title="title || ''" width="min(1280px, 92vw)">
    <div v-if="loading" class="flex h-full items-center justify-center py-16"><MfSpinner size="lg"/></div>
    <ResultTable v-else-if="configuration" class="h-full" fill searchable :headers="configuration.headers || []" :rows="configuration.columns || []"/>
    <MfEmpty v-else class="h-full" :title="$t('common.noData')"/>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { HistoryModel } from '@/model/history'
import AuditService from '@/services/audit'
import { Configuration, ConfigurationRequest } from '@/views/components/visual/Configuration'
import ResultTable from '@/views/components/table/ResultTable.vue'

defineOptions({ name: 'HistoryData' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: HistoryModel | null }>(), {
  isVisible: false,
  info: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const title = ref<string | null>(null)
const loading = ref(false)
const configuration = ref<Configuration | null>(null)

// A query's saved result opens in a wide drawer, so its rows have room.
const handlerInitialize = () => {
  if (props.info) {
    title.value = `${ t('query.common.historyDataInfo').replace('$VALUE', props.info.id as unknown as string) }`
    loading.value = true
    AuditService.getData(props.info.code as string)
                .then(response => {
                  if (response.status && response.data?.isSuccessful) {
                    configuration.value = ConfigurationRequest.of(response)
                  }
                  else {
                    MfMessage.error(response.data?.message || response.message || t('common.requestError'))
                  }
                })
                .finally(() => (loading.value = false))
  }
}

handlerInitialize()
</script>
