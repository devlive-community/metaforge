<template>
  <MfModal v-model:open="visible"
           width="60%"
           :title="title">
    <div class="relative w-full justify-center items-center" style="height: 60vh; overflow: auto;">
      <div v-if="info">
        <VisualView class="h-full"
                    :code="info.dataset?.code"
                    :type="info.type"
                    :configuration="JSON.parse(info.configure as string)"
                    :query="info.type === 'DATASET' ? JSON.parse(info.query as string) : info.query"
                    :original="info?.source?.code"/>
      </div>
    </div>

    <template #footer>
      <MfButton @click="onCancel">
        {{ $t('common.cancel') }}
      </MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ReportModel } from '@/model/report'
import VisualView from '@/views/components/visual/VisualView.vue'

defineOptions({ name: 'ReportView' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: ReportModel | null }>(), {
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

if (props.info) {
  title.value = `${ t('report.common.view').replace('$VALUE', String(props.info.name)) }`
}

const onCancel = () => {
  visible.value = false
}
</script>
