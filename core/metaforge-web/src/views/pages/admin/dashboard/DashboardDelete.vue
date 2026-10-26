<template>
  <MfModal v-model:open="visible"
           :title="$t('dashboard.common.delete') + ' [ ' + data?.name + ' ]'"
           @close="onCancel">
    <MfSpace direction="vertical" fill :style="{ width: '100%' }">
      <MfAlert variant="danger" :title="$t('dashboard.tip.deleteTip1')"/>
      <MfAlert variant="danger" :title="$t('dashboard.tip.deleteTip2')"/>
      <MfAlert variant="danger" :title="$t('dashboard.tip.deleteTip3').replace('$NAME', data?.name as string)"/>
    </MfSpace>

    <MfForm :model="formState" class="mt-3" @finish="onSubmit">
      <MfFormItem name="name"
                  :rules="[
                        { required: true, message: $t('dashboard.validator.name.required') },
                        { validator: validateMatch }
                  ]">
        <MfInput v-model="formState.name" :placeholder="$t('dashboard.placeholder.name')"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfSpace>
          <MfButton @click="onCancel">
            {{ $t('common.cancel') }}
          </MfButton>

          <MfButton variant="danger" type="submit" :loading="loading">
            {{ $t('dashboard.common.delete') }}
          </MfButton>
        </MfSpace>
      </div>
    </MfForm>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import DashboardService from '@/services/dashboard'
import { DashboardModel } from '@/model/dashboard'

defineOptions({ name: 'DashboardDelete' })

const props = withDefaults(defineProps<{ isVisible?: boolean; data?: DashboardModel | null }>(), {
  isVisible: false,
  data: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const formState = ref({ name: '' })

const validateMatch = (value: string) => {
  if (value !== String(props.data?.name)) {
    return t('dashboard.validator.name.match').replace('$VALUE', String(props.data?.name))
  }
  return true
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  if (props.data) {
    loading.value = true
    DashboardService.deleteByCode(props.data.code)
                    .then((response) => {
                      if (response.status) {
                        MfMessage.success(`${ t('dashboard.common.delete') } [ ${ props.data?.name } ] ${ t('common.successfully') }`)
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
