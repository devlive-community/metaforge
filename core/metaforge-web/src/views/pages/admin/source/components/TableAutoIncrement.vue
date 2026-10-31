<template>
  <MfModal v-model:open="localVisible"
           :title="$t('source.common.resetAutoIncrement')"
           :close-on-overlay="false"
           @close="onCancel">
    <MfForm :model="formState" layout="vertical" @finish="onSubmit">
      <MfFormItem name="autoIncrement" :label="$t('source.common.resetTo')">
        <MfInputNumber v-model="formState.autoIncrement"
                       :min="1"
                       :style="{ width: '100%' }"
                       :placeholder="$t('snippet.placeholder.name')"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfSpace>
          <MfButton @click="onCancel">
            {{ $t('common.cancel') }}
          </MfButton>

          <MfButton variant="primary" type="submit" :loading="loading">
            {{ $t('common.apply') }}
          </MfButton>
        </MfSpace>
      </div>
    </MfForm>
  </MfModal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useSourceManageRoute as useRoute } from '@/composables/useSourceManageRoute'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata.ts'

const emit = defineEmits(['close'])
const route = useRoute()
const { t } = useI18n()

const props = withDefaults(defineProps<{
  visible: boolean
  info?: any
}>(), {
  visible: false
})

const localVisible = ref(props.visible)
const loading = ref(false)
const formState = ref({ autoIncrement: props.info?.object_auto_increment ?? 1 })

const onCancel = () => emit('close')

const onSubmit = () => {
  const code = route?.params.source as string
  const database = route?.params.database as string
  const table = route?.params.table as string

  if (props.info && code && database && table) {
    loading.value = true
    const configure = {
      autoIncrement: formState.value.autoIncrement
    }
    MetadataService.updateAutoIncrement(code, database, table, configure)
                   .then((response) => {
                     if (response.status && response.data && response.data.isSuccessful) {
                       MfMessage.success(t('source.tip.resetAutoIncrementSuccess').replace('$VALUE', String(formState.value.autoIncrement)))
                       onCancel()
                     }
                     else {
                       MfMessage.error(response.message)
                     }
                   })
                   .finally(() => (loading.value = false))
  }
}
</script>
