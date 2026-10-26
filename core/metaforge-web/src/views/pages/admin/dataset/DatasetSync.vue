<template>
  <MfModal v-model:open="visible" :title="`[ ${ info?.name } ] ${ $t('dataset.common.syncData') }`">
    <MfAlert variant="danger" :title="$t('dataset.tip.syncData')"/>

    <div v-if="loadingFields" class="py-2 text-xs text-gray-500">{{ $t('common.loading') }}</div>

    <div v-else-if="fields.length > 0" class="mt-3 space-y-3">
      <div class="text-xs font-medium">{{ $t('dataset.sync.overrideTitle') }}</div>
      <div v-for="field in fields" :key="field.name" class="space-y-1">
        <label class="text-xs">{{ field.name }}</label>
        <div v-if="field.description" class="text-xs text-gray-500">{{ field.description }}</div>

        <MfInput v-if="field.type === 'STRING'" v-model="overrides[field.name]"/>
        <MfInputNumber v-else-if="field.type === 'NUMBER'" v-model="overrides[field.name]" :style="{ width: '100%' }"/>
        <MfSwitch v-else-if="field.type === 'BOOLEAN'"
                  v-model="booleanProxies[field.name]"
                  @update:model-value="(v: boolean) => onBoolChange(field.name, v)"/>
        <MfInput v-else-if="field.type === 'PASSWORD'" v-model="overrides[field.name]" type="password" show-password/>
      </div>
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
import { MfMessage } from 'metaforge-ui'
import DatasetService from '@/services/dataset'
import { DatasetModel } from '@/model/dataset'

interface PluginConfigureField
{
  name: string
  type: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'PASSWORD'
  defaultValue: string
  description: string
  tunable: boolean
}

defineOptions({ name: 'DatasetSync' })

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
const loadingFields = ref(false)
const fields = ref<PluginConfigureField[]>([])
const overrides = ref<Record<string, string>>({})
const booleanProxies = ref<Record<string, boolean>>({})

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
                      overrides.value[f.name] = f.defaultValue ?? ''
                      if (f.type === 'BOOLEAN') {
                        booleanProxies.value[f.name] = (f.defaultValue + '').toLowerCase() === 'true'
                      }
                    }
                  }
                })
                .finally(() => (loadingFields.value = false))
}

const onBoolChange = (name: string, value: boolean) => {
  overrides.value[name] = value ? 'true' : 'false'
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  if (props.info) {
    loading.value = true
    for (const f of fields.value) {
      if (f.type === 'BOOLEAN') {
        overrides.value[f.name] = booleanProxies.value[f.name] ? 'true' : 'false'
      }
    }
    DatasetService.syncData(props.info.code, overrides.value)
                  .then(response => {
                    if (response.status) {
                      MfMessage.success(`${ t('dataset.common.syncData') } [ ${ props.info?.name } ] ${ t('common.successfully') }`)
                      onCancel()
                    }
                    else {
                      MfMessage.error(`${ t('dataset.common.syncData') } [ ${ props.info?.name } ] ${ t('common.fail') }`)
                    }
                  })
                  .finally(() => (loading.value = false))
  }
}

handleInitialize()
</script>
