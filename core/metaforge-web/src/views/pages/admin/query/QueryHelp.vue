<template>
  <MfModal v-model:open="visible"
           width="40%"
           :title="$t('query.common.help')"
           @close="onCancel">
    <MfTabs v-model:active-key="activeTab" @change="onChange">
      <MfTabPane v-for="item in helpType" :key="getEnumName(item)" :item-key="getEnumName(item)" :tab="getEnumName(item)">
        <div class="relative h-full mt-2.5">
          <div v-if="loading" class="flex items-center justify-center py-8">
            <MfSpinner size="lg"/>
          </div>
          <MdPreview v-else-if="helpReplyContent" :modelValue="helpReplyContent" style="padding: 0; width: 100%"/>
        </div>
      </MfTabPane>
    </MfTabs>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import UserService from '@/services/user'
import { UserModel, UserQuestionModel } from '@/model/user'
import { HelpType } from '@/views/pages/admin/query/HelpType'
import MessageService from '@/services/message'

import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

defineOptions({ name: 'QueryHelp' })

const props = withDefaults(defineProps<{
  isVisible?: boolean
  content?: string
  engine?: string
  message?: string
  helpType?: Array<HelpType>
}>(), {
  isVisible: false,
  content: '',
  engine: '',
  message: '',
  helpType: () => []
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const i18n = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const activeTab = ref('ANALYSIS')
const userInfo = ref<UserModel | null>(null)
const helpReplyContent = ref<string | null>(null)

const getEnumName = (value: HelpType): string => HelpType[value]

const onChange = (value: any) => {
  loading.value = true
  const userQuestion: UserQuestionModel = {
    type: 'ChatGPT',
    content: props.content,
    transType: value,
    engine: props.engine,
    error: props.message,
    locale: i18n.locale.value,
    newChat: true
  }
  MessageService.aiReply(userQuestion)
                .then((response) => {
                  if (response.status) {
                    helpReplyContent.value = response.data.content
                  }
                  else {
                    MfMessage.error(response.message)
                  }
                })
                .finally(() => (loading.value = false))
}

const onCancel = () => {
  visible.value = false
}

const handleInitialize = () => {
  UserService.getInfo()
             .then((response) => {
               if (response.status) {
                 userInfo.value = response.data
                 onChange(props.helpType[0] as HelpType)
               }
             })
}

handleInitialize()
</script>
