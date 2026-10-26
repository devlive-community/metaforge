<template>
  <MfModal v-model:open="visible"
           width="60%"
           :title="$t('workflow.text.logger')"
           @close="onCancel">
    <div v-if="loading" class="flex items-center justify-center py-10">
      <MfSpinner size="lg" label="Loading..."/>
    </div>

    <LogViewer v-else-if="!loading"
                    height="380"
                    toolbar
                    :items="logs"
                    :custom-patterns="customPatterns"/>

    <template #footer>
      <MfButton @click="onCancel">
        {{ $t('common.cancel') }}
      </MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { MfMessage } from 'metaforge-ui'
import { WorkflowModel } from '@/model/workflow'
import WorkflowService from '@/services/workflow'
import LogViewer from '@/views/components/logger/LogViewer.vue'

defineOptions({ name: 'WorkflowLogger' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: WorkflowModel | null }>(), {
  isVisible: false,
  info: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const logs = ref<string[]>([])
const customPatterns = {
  timestamp: [/^(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2},\d{3})/],
  level: [/\b(INFO|ERROR|WARN|DEBUG)\b/],
  thread: [/\[(pool-\d+-thread-\d+)\]/],
  file: [/\[([^[\]]+\.java:\d+)\]/]
}

const onCancel = () => {
  visible.value = false
}

const handleInitialize = () => {
  if (props.info) {
    loading.value = true
    WorkflowService.getLogger(props.info.code)
                   .then((response) => {
                     if (response.status) {
                       logs.value = response.data
                     }
                     else {
                       MfMessage.error(response.message)
                     }
                   })
                   .finally(() => (loading.value = false))
  }
}

handleInitialize()
</script>
