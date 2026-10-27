<template>
  <MfDrawer v-model:open="visible" :title="title" width="40%">
    <div v-if="loading" class="flex items-center justify-center py-8">
      <MfSpinner size="lg"/>
    </div>
    <MfForm v-else-if="formState" id="snippet-info-form" :model="formState" layout="vertical" @finish="onSubmit">
      <MfFormItem name="name"
                  :label="$t('common.name')"
                  :rules="[{ required: true, message: $t('common.name') }]">
        <MfInput v-model="formState.name"/>
      </MfFormItem>

      <MfFormItem name="description" :label="$t('common.description')">
        <MfTextarea v-model="formState.description"/>
      </MfFormItem>

      <MfFormItem name="content" :label="$t('common.content')">
        <AceEditor :value="formState.context" @update:value="formState.context = $event"/>
      </MfFormItem>

    </MfForm>

    <template #footer>
      <MfSpace>
        <MfButton variant="danger" @click="onCancel()">
          {{ $t('common.cancel') }}
        </MfButton>
        <MfButton variant="primary" type="submit" form="snippet-info-form" :loading="loading" :disabled="loading">
          {{ $t('common.save') }}
        </MfButton>
      </MfSpace>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { cloneDeep } from 'lodash'
import { SnippetModel, SnippetRequest } from '@/model/snippet'
import AceEditor from '@/views/components/editor/AceEditor.vue'
import SnippetService from '@/services/snippet'

defineOptions({ name: 'SnippetInfo' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: SnippetModel | null }>(), {
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
const formState = ref<SnippetModel>(null as unknown as SnippetModel)
const title = ref<string | null>(null)

const handleInitialize = () => {
  title.value = t('snippet.common.create')
  if (props.info) {
    formState.value = cloneDeep(props.info)
    if (props.info.id) {
      title.value = `${ t('snippet.common.modify').replace('$VALUE', props.info.name as string) }`
    }
  }
  else {
    formState.value = SnippetRequest.of()
  }
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  loading.value = true
  SnippetService.saveOrUpdate(formState.value)
                .then((response) => {
                  if (response.status) {
                    MfMessage.success(t('snippet.tip.createSuccess').replace('$VALUE', formState.value.name as string))
                    onCancel()
                  }
                })
                .finally(() => (loading.value = false))
}

handleInitialize()
</script>
