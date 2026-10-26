<template>
  <MfModal v-model:open="visible"
           width="60%"
           :title="$t('workflow.text.configure')"
           @close="onCancel">
    <div v-if="loading" class="flex items-center justify-center py-10">
      <MfSpinner size="lg" label="Loading..."/>
    </div>

    <WorkflowView v-else-if="!loading && configuration"
                          :nodes="configuration.nodes"
                          :canvas="{ height: 500 }"
                          :data="data"/>

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
import ConfigurationService from '@/services/configure.ts'
import WorkflowService from '@/services/workflow.ts'
import HttpUtils from '@/utils/http.ts'
import WorkflowView from '@/views/components/editor/workflow/WorkflowView.vue'

export interface Configuration
{
  categories: any[]
  nodes: any[]
}

defineOptions({ name: 'WorkflowFlow' })

const props = withDefaults(defineProps<{ isVisible?: boolean; code?: string }>(), { isVisible: false })
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const configuration = ref<Configuration | null>(null)
const data = ref<any>(null)

const onCancel = () => {
  visible.value = false
}

HttpUtils.all([ConfigurationService.getExecutor(), WorkflowService.getByCode(props.code)])
         .then(HttpUtils.spread((...responses: any[]) => {
           const [executor, workflow] = responses

           if (executor.status && executor.data) {
             configuration.value = executor.data
           }

           if (workflow.status && workflow.data) {
             data.value = workflow.data.configure
           }
           else {
             MfMessage.error(workflow.message)
           }
         }))
         .catch((error: any) => {
           MfMessage.error(error.message || 'Failed to initialize workflow')
         })
         .finally(() => {
           loading.value = false
         })
</script>
