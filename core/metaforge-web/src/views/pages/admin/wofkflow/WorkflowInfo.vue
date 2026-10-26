<template>
  <div class="w-full h-screen min-h-screen" style="height: 100%;">
    <MfCard>
      <template #header>
        <div class="ml-2">{{ $t('workflow.text.create') }}</div>
      </template>

      <template #extra>
        <div class="flex items-center space-x-4">
          <div class="flex items-center space-x-2">
            <span class="text-sm">{{ $t('common.executor') }}</span>

            <MfSelect v-model="formState.executor"
                      :style="{ minWidth: '160px' }"
                      :options="installedExecutors.map((executor: any) => ({ label: executor.name, value: executor.name }))"/>
          </div>

          <MfButton variant="primary" :disabled="!(workflowState?.validation?.length === 0)" @click="visible = true">
            {{ $t('common.publish') }}
          </MfButton>
        </div>
      </template>

      <div class="relative h-screen">
        <WorkflowEditor v-if="configuration && !loading"
                            v-model="workflowState"
                            :categories="configuration.categories"
                            :nodes="configuration.nodes"
                            :connections="[]"
                            :configureWidth="380">
        </WorkflowEditor>

        <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center">
          <MfSpinner size="lg" label="Loading..."/>
        </div>
      </div>
    </MfCard>
  </div>

  <MfModal v-model:open="visible" :title="$t('workflow.text.configure')">
    <MfForm :model="formState" layout="vertical" @finish="onSubmit">
      <MfFormItem name="name"
                  :label="$t('workflow.text.name')"
                  :rules="[
                      { required: true, message: $t('workflow.validator.name.required') },
                  ]">
        <MfInput v-model="formState.name" :placeholder="$t('workflow.placeholder.name')"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfButton variant="primary" type="submit">{{ $t('common.submit') }}</MfButton>
      </div>
    </MfForm>
  </MfModal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import ConfigurationService from '@/services/configure'
import WorkflowService from '@/services/workflow'
import PluginService from '@/services/plugin'
import HttpUtils from '@/utils/http.ts'
import { RouterUtils } from '@/utils/route.ts'
import WorkflowEditor from '@/views/components/editor/workflow/WorkflowEditor.vue'

export interface Configuration
{
  categories: any[]
  nodes: any[]
}

defineOptions({ name: 'WorkflowInfo' })

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const loading = ref(false)
const visible = ref(false)
const configuration = ref<Configuration | null>(null)
const workflowState = ref<any | null>(null)
const installedExecutors = ref<any[]>([])
const formState = ref({
  code: null as string | null,
  executor: 'Local',
  name: '',
  configure: null as any
})

const handleInitialize = () => {
  const code = RouterUtils.getParam(route, 'code')
  loading.value = true

  // Prepare the base API calls that are always needed
  const baseApiCalls = [
    ConfigurationService.getExecutor(),
    PluginService.getPlugins()
  ]

  // If code exists, add the workflow API call
  const apiCalls = code
      ? [...baseApiCalls, WorkflowService.getByCode(code)]
      : baseApiCalls

  HttpUtils.all(apiCalls)
           .then(HttpUtils.spread((...responses: any[]) => {
             // Handle executor configuration
             const [executor, plugin, ...rest] = responses

             if (executor.status && executor.data) {
               configuration.value = executor.data
             }

             // Handle plugins
             if (plugin.status && plugin.data) {
               installedExecutors.value = plugin.data.filter(
                   (v: { type: string }) => v.type === 'EXECUTOR'
               )
             }

             // Handle workflow data if code exists
             if (code && rest.length > 0) {
               const workflow = rest[0]
               if (workflow.status && workflow.data) {
                 workflowState.value = workflow.data.configure
                 // Update form state with existing workflow data
                 formState.value.executor = workflow.data.executor || 'Local'
                 formState.value.name = workflow.data.name || ''
                 formState.value.configure = workflow.data.configure || null
                 formState.value.code = workflow.data.code || null
               }
               else {
                 MfMessage.error(workflow.message)
               }
             }
           }))
           .catch((error: any) => {
             MfMessage.error(error.message || 'Failed to initialize workflow')
           })
           .finally(() => {
             loading.value = false
           })
}

const onSubmit = () => {
  formState.value.configure = workflowState.value.data
  WorkflowService.saveOrUpdate(formState.value)
                 .then((response) => {
                   if (response.status) {
                     MfMessage.success(`${ t('workflow.tip.publishSuccess').replace('$VALUE', response.data.name) }`)
                     router.push('/admin/workflow')
                     visible.value = false
                   }
                   else {
                     MfMessage.error(response.message)
                   }
                 })
}

handleInitialize()
</script>
