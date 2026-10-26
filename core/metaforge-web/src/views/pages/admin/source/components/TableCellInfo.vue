<template>
  <MfModal v-model:open="visible"
           width="40%"
           :title="$t('source.common.previewDML')">
    <div class="relative h-full">
      <div v-if="loading" class="flex items-center justify-center py-8">
        <MfSpinner size="lg" label="Loading..."/>
      </div>
      <AceEditor v-else-if="!loading && contentDML" :value="contentDML" :read-only="true"/>
    </div>

    <template #footer>
      <MfSpace>
        <MfButton @click="onCancel">
          {{ $t('common.cancel') }}
        </MfButton>

        <MfButton variant="primary" :disabled="submitting" :loading="submitting" @click="onSubmit">
          {{ $t('common.submit') }}
        </MfButton>
      </MfSpace>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import AceEditor from '@/views/components/editor/AceEditor.vue'
import MetadataService from '@/services/metadata.ts'

defineOptions({ name: 'TableCellInfo' })

const props = withDefaults(defineProps<{ isVisible?: boolean; columns?: any[]; isUpdate?: boolean }>(), {
  isVisible: false,
  isUpdate: false
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const route = useRoute()
const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const submitting = ref(false)
const contentDML = ref<string | null>(null)

const onChange = (preview: boolean) => {
  const code = route?.params.source as string
  const database = route?.params.database as string
  const table = route?.params.table as string

  if (code && database && table) {
    if (preview) {
      loading.value = true
    }
    else {
      submitting.value = true
    }

    const configure = {
      preview: preview,
      rows: props.columns
    }

    const apiCall = props.isUpdate
        ? MetadataService.updateData(code, database, table, configure)
        : MetadataService.insertData(code, database, table, configure)

    apiCall.then((response) => {
      if (response.status && response.data && response.data.isSuccessful) {
        if (preview) {
          contentDML.value = response.data.content
        }
        else {
          MfMessage.success(t('source.tip.updateSuccess'))
          onCancel()
        }
      }
      else {
        MfMessage.error(response.data.message)
      }
    })
           .finally(() => {
             if (preview) {
               loading.value = false
             }
             else {
               submitting.value = false
             }
           })
  }
}

const onSubmit = () => onChange(false)

const onCancel = () => {
  visible.value = false
}

onChange(true)
</script>
