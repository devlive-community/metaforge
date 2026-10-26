<template>
  <MfModal v-model:open="visible" :title="title">
    <div class="relative max-h-[80%]" :class="configuration ? '' : 'h-24'">
      <div v-if="loading" class="flex items-center justify-center py-8">
        <MfSpinner size="lg"/>
      </div>
      <VisualTable v-else-if="configuration" :configuration="configuration as any" :submitted="false"/>
    </div>

    <template #footer>
      <MfButton variant="danger" @click="handlerCancel">
        {{ $t('common.cancel') }}
      </MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { HistoryModel } from '@/model/history'
import AuditService from '@/services/audit'
import { Configuration, ConfigurationRequest } from '@/views/components/visual/Configuration'
import VisualTable from '@/views/components/visual/components/VisualTable.vue'

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
                    MfMessage.error(response.data.message)
                  }
                })
                .finally(() => (loading.value = false))
  }
}

const handlerCancel = () => {
  visible.value = false
}

handlerInitialize()
</script>
