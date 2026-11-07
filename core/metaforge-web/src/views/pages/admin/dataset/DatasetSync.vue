<template>
  <MfModal v-model:open="visible" width="720px" :title="`[ ${ info?.name } ] ${ $t('dataset.common.syncData') }`">
    <MfAlert variant="danger" :title="$t('dataset.tip.syncData')"/>

    <div v-if="loadingFields" class="py-2 text-xs text-gray-500">{{ $t('common.loading') }}</div>

    <div v-else-if="fields.length > 0" class="mt-3 space-y-3">
      <div class="text-xs font-medium">{{ $t('dataset.sync.overrideTitle') }}</div>
      <RuntimeFields collapsible :fields="fields" :values="overrides" @change="(name, value) => (overrides[name] = value)"/>
    </div>

    <template #footer>
      <MfSpace>
        <MfButton @click="onCancel">{{ $t('common.cancel') }}</MfButton>
        <MfButton variant="primary" :disabled="loading" :loading="loading" @click="onSubmit">
          {{ $t('dataset.common.syncData') }}
        </MfButton>
      </MfSpace>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import DatasetService from '@/services/dataset'
import { DatasetModel } from '@/model/dataset'
import type { RuntimeField } from '@/model/runtime-fields'
import RuntimeFields from '@/views/components/runtime/RuntimeFields.vue'

defineOptions({ name: 'DatasetSync' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: DatasetModel | null }>(), {
  isVisible: false,
  info: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()
const router = useRouter()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const loadingFields = ref(false)
const fields = ref<RuntimeField[]>([])
const overrides = ref<Record<string, string>>({})

const handleInitialize = () => {
  if (!props.info?.code) {
    return
  }
  loadingFields.value = true
  DatasetService.getSyncFields(props.info.code)
                .then(response => {
                  if (response.status && Array.isArray(response.data)) {
                    fields.value = response.data
                    for (const f of fields.value) {
                      // The switch compares with 'true', so booleans keep their lower-case form.
                      overrides.value[f.name] = f.type === 'BOOLEAN' ? String(f.defaultValue).toLowerCase() : f.defaultValue ?? ''
                    }
                  }
                })
                .finally(() => (loadingFields.value = false))
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  if (props.info) {
    loading.value = true
    const code = props.info.code
    DatasetService.syncData(code, overrides.value)
                  .then(response => {
                    if (response.status) {
                      MfMessage.success(`${ t('dataset.common.syncData') } [ ${ props.info?.name } ] ${ t('common.successfully') }`)
                      onCancel()
                      // Straight to this run's progress.
                      router.push({ path: `/dataset/runs/${ code }`, query: { run: String(response.data?.id ?? '') } })
                    }
                    else {
                      MfMessage.error(String(response.message || `${ t('dataset.common.syncData') } [ ${ props.info?.name } ] ${ t('common.fail') }`))
                    }
                  })
                  .finally(() => (loading.value = false))
  }
}

handleInitialize()
</script>
