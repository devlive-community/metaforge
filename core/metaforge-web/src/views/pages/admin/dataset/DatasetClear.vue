<template>
  <MfModal v-model:open="visible" :title="`[ ${ info?.name } ] ${ $t('dataset.common.clearData') }`">
    <MfAlert :title="$t('dataset.tip.clearData')"/>

    <div class="mt-2.5 grid grid-cols-1 sm:grid-cols-2 gap-2">
      <MfCard :title="$t('dataset.common.totalRows')">
        <div class="mt-3 flex items-center p-4 text-center">{{ info?.totalRows }}</div>
      </MfCard>

      <MfCard :title="$t('dataset.common.totalSize')">
        <div class="mt-3 flex items-center p-4 text-center">{{ info?.totalSize }}</div>
      </MfCard>
    </div>

    <template #footer>
      <MfSpace>
        <MfButton @click="onCancel">{{ $t('common.cancel') }}</MfButton>
        <MfButton variant="danger" :disabled="loading" :loading="loading" @click="onSubmit">
          {{ $t('dataset.common.clearData') }}
        </MfButton>
      </MfSpace>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import DatasetService from '@/services/dataset'
import { DatasetModel } from '@/model/dataset'

defineOptions({ name: 'DatasetClear' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: DatasetModel | null }>(), {
  isVisible: false,
  info: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  if (props.info) {
    loading.value = true
    DatasetService.clearData(props.info.code)
                  .then((response: { status: boolean }) => {
                    if (response.status) {
                      MfMessage.success(`${ t('dataset.common.clearData') } [ ${ props.info?.name } ] ${ t('common.successfully') }`)
                      onCancel()
                    }
                    else {
                      MfMessage.error(`${ t('dataset.common.clearData') } [ ${ props.info?.name } ] ${ t('common.fail') }`)
                    }
                  })
                  .finally(() => (loading.value = false))
  }
}
</script>
