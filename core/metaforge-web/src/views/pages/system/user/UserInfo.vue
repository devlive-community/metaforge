<template>
  <MfDrawer v-model:open="visible" :title="title">
    <UserForm class="mt-3" :info="info" @close="handlerCancel"/>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { UserModel } from '@/model/user.ts'
import UserForm from '@/views/pages/system/user/components/UserForm.vue'

defineOptions({ name: 'UserInfo' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: UserModel | null }>(), {
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
  title.value = t('user.common.edit').replace('$VALUE', props.info.username as string)
}
else {
  title.value = t('user.common.create')
}

const handlerCancel = () => {
  visible.value = false
}
</script>
