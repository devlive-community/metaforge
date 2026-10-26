<template>
  <div class="flex flex-col min-h-screen">
    <div class="flex flex-1 min-h-0">
      <aside class="w-[280px] shrink-0">
        <MfCard :title="$t('common.chat')">
          <template #extra>
            <MfButton variant="ghost" size="sm" class="rounded-full!" @click="visibleInfo(true)">
              <PlusOutlined :style="{ fontSize: '15px' }"/>
            </MfButton>
          </template>

          <div class="relative min-h-screen">
            <div v-if="loading" class="flex items-center justify-center py-8">
              <MfSpinner size="lg"/>
            </div>
            <MfRadioGroup v-else v-model="toggleValue" variant="button" class="dc-chat-radio flex flex-col! items-stretch! p-1 gap-1.5" :style="{ width: '100%' }" @change="onChange">
              <div class="grid gap-2">
                <div v-for="item of data" :key="item.id">
                  <MfRadio class="w-full! justify-start!" :value="item.id">
                    <div class="flex items-center space-x-4">
                      <MfAvatar size="sm" :src="item.avatar" :alt="item.name"/>

                      <div class="text-sm font-medium leading-none">{{ item.name }}</div>
                    </div>
                  </MfRadio>
                </div>
              </div>
            </MfRadioGroup>
          </div>
        </MfCard>
      </aside>

      <main class="flex-1 min-w-0">
        <div v-if="dataInfo">
          <MfCard>
            <template #header>
              <div class="flex flex-row items-center justify-between">
                <div class="flex items-center space-x-4">
                  <MfAvatar :src="dataInfo.avatar" :alt="dataInfo.name"/>
                  <div>
                    <p class="text-sm font-medium leading-none">{{ dataInfo.name }}</p>
                    <p class="text-sm text-muted-foreground">{{ dataInfo.description }}</p>
                  </div>
                </div>
              </div>
            </template>

            <template #extra>
              <div class="flex h-5 items-center space-x-4 text-sm">
                <div>
                  Prompt Tokens: {{ promptTokens }}
                </div>
                <span class="self-stretch w-px bg-line"/>
                <div>
                  Completion Tokens: {{ completionTokens }}
                </div>
                <span class="self-stretch w-px bg-line"/>
                <div>
                  Total Tokens: {{ totalTokens }}
                </div>
              </div>
            </template>

            <div class="relative min-h-screen">
              <div v-if="loadingMessages" class="flex items-center justify-center py-8">
                <MfSpinner size="lg"/>
              </div>
              <div v-else class="w-full overflow-y-auto overflow-x-hidden h-full flex flex-col">
                <div ref="messagesContainer" class="w-full overflow-y-auto overflow-x-hidden flex flex-col" style="height: 550px;">
                  <div v-for="(item, index) in messages"
                       :key="`msg-${ index }`"
                       :class="cn('flex flex-col gap-2 p-4 whitespace-pre-wrap',
                                item.type === 'question' ? 'items-end' : 'items-start'
                          )">
                    <div class="flex gap-3 items-center">
                      <span class="bg-accent p-3 rounded-md max-w-lg">{{ item.content }}</span>
                    </div>

                    <div v-if="item.type === 'answer'" class="flex text-sm text-muted-foreground mt-0.5 space-x-2">
                      <div>Model: {{ item.model }}</div>
                      <span class="self-stretch w-px bg-line"/>
                      <div>Prompt Tokens: {{ item.promptTokens }}</div>
                      <span class="self-stretch w-px bg-line"/>
                      <div>Completion Tokens: {{ item.completionTokens }}</div>
                      <span class="self-stretch w-px bg-line"/>
                      <div>Total Tokens: {{ item.totalTokens }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <template #footer>
              <div class="p-2 flex justify-between w-full items-center gap-2">
                <MfTextarea v-model="inputValue" placeholder="Type a message ..."/>

                <MfButton variant="primary" :loading="submitting" :disabled="!inputValue || submitting" @click="onSubmit">
                  <SendOutlined :style="{ fontSize: '18px' }"/>
                </MfButton>
              </div>
            </template>
          </MfCard>
        </div>
      </main>
    </div>
  </div>

  <ChatInfo v-if="dataVisible" :is-visible="dataVisible" @close="visibleInfo($event)"/>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { MfMessage } from 'metaforge-ui'
import { FilterModel } from '@/model/filter.ts'
import ChatService from '@/services/chat.ts'
import ChatInfo from '@/views/pages/admin/chat/ChatInfo.vue'
import { ChatModel } from '@/model/chat.ts'
import { toNumber } from 'lodash'
import { cn } from '@/lib/utils.ts'
import { MessageModel } from '@/model/message.ts'
import MessageService from '@/services/message.ts'
import { PlusOutlined, SendOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'ChatHome' })

const filter: FilterModel = new FilterModel()

const loading = ref(true)
const data = ref<ChatModel[]>([])
const dataInfo = ref<ChatModel | null>(null)
const dataVisible = ref(false)
const loadingMessages = ref(false)
const messages = ref<MessageModel[]>([])
const promptTokens = ref(0)
const completionTokens = ref(0)
const totalTokens = ref(0)
const submitting = ref(false)
const toggleValue = ref<any>(null)
const inputValue = ref('')
const messagesContainer = ref<any>(null)

const handleInitialize = () => {
  loading.value = true
  ChatService.getAll(filter)
             .then((response) => {
               if (response.status) {
                 data.value = response.data.content
               }
               else {
                 MfMessage.error(response.message)
               }
             })
             .finally(() => (loading.value = false))
}

const visibleInfo = (opened: boolean) => {
  dataVisible.value = opened
  if (!opened) {
    handleInitialize()
  }
}

const handlerGoBottom = () => {
  const scrollElem = messagesContainer.value as any
  setTimeout(() => {
    scrollElem?.scrollTo({ top: scrollElem.scrollHeight, behavior: 'smooth' })
  }, 0)
}

const counterToken = () => {
  const answers = messages.value.filter(message => message.type === 'answer')
  promptTokens.value = answers.reduce((sum, message) => sum + toNumber(message.promptTokens), 0)
  completionTokens.value = answers.reduce((sum, message) => sum + toNumber(message.completionTokens), 0)
  totalTokens.value = answers.reduce((sum, message) => sum + toNumber(message.totalTokens), 0)
}

const onChange = (event: any) => {
  const value = event?.target ? event.target.value : event
  dataInfo.value = data.value.find(item => item.id === toNumber(value)) as unknown as ChatModel
  loadingMessages.value = true
  ChatService.getMessages(toNumber(value))
             .then((response) => {
               messages.value = response.data
             })
             .finally(() => {
               loadingMessages.value = false
               counterToken()
               handlerGoBottom()
             })
}

const onSubmit = () => {
  submitting.value = true
  const msg = {
    content: inputValue.value,
    chat: dataInfo.value,
    type: 'question'
  } as any
  messages.value.push(msg)
  handlerGoBottom()
  MessageService.saveOrUpdate(msg)
                .then((response) => {
                  if (response.status) {
                    messages.value.push(response.data)
                    inputValue.value = ''
                  }
                  else {
                    MfMessage.error(response.message)
                  }
                })
                .finally(() => {
                  submitting.value = false
                  counterToken()
                  handlerGoBottom()
                })
}

handleInitialize()
</script>
