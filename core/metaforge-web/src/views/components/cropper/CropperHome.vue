<template>
  <div>
    <MfButton class="p-0 w-full">
      <input ref="uploadInput" type="file" accept="image/jpg, image/jpeg, image/png, image/gif" @change="selectFile"/>
    </MfButton>
  </div>

  <div v-if="result.blobURL || pic" class="mt-2.5 flex justify-center">
    <MfAvatar shape="square"
              :size="160"
              class="bg-transparent"
              :src="result.blobURL ? result.blobURL : propPic">
    </MfAvatar>
  </div>

  <MfModal v-if="isShowModal" v-model:open="isShowModal" :title="$t('common.cropper')">
    <div class="p-0">
      <VuePictureCropper style="max-height: 400px"
                         :boxStyle="{ width: '100%', height: '100%', backgroundColor: '#f8f8f8', margin: 'auto' }"
                         :options="{ viewMode: 1, dragMode: 'crop', }"
                         :img="pic"
                         @ready="ready"/>
    </div>
    <template #footer>
      <div class="space-x-2">
        <MfButton size="sm" @click="isShowModal = false">
          {{ $t('common.cancel') }}
        </MfButton>

        <MfButton size="sm" variant="danger" @click="clear">
          {{ $t('common.clear') }}
        </MfButton>

        <MfButton size="sm" variant="danger" @click="reset">
          {{ $t('common.reset') }}
        </MfButton>

        <MfButton size="sm" variant="primary" @click="getResult">
          {{ $t('common.cropper') }}
        </MfButton>
      </div>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import VuePictureCropper, { cropper } from 'vue-picture-cropper'

defineOptions({ name: 'CropperHome' })

const props = defineProps<{ pic?: string }>()
const emit = defineEmits<{ (e: 'update:value', value: any): void }>()

const propPic = props.pic
const isShowModal = ref<boolean>(false)
const uploadInput = ref<HTMLInputElement | null>(null)
const pic = ref<string>(props.pic as string)
const result = reactive({
  blobURL: ''
})

function selectFile(e: Event)
{
  pic.value = ''
  result.blobURL = ''

  const { files } = e.target as HTMLInputElement
  if (!files || !files.length) {
    return
  }

  const file = files[0]
  const reader = new FileReader()
  reader.readAsDataURL(file)
  reader.onload = () => {
    pic.value = String(reader.result)
    isShowModal.value = true
    if (!uploadInput.value) {
      return
    }
    uploadInput.value.value = ''
  }
}

async function getResult()
{
  if (!cropper) {
    return
  }
  const blob: Blob | null = await cropper.getBlob()
  if (!blob) {
    return
  }
  result.blobURL = URL.createObjectURL(blob)
  isShowModal.value = false
  const file = await cropper.getFile()
  emit('update:value', file)
}

function clear()
{
  if (!cropper) {
    return
  }
  cropper.clear()
}

function reset()
{
  if (!cropper) {
    return
  }
  cropper.reset()
}

function ready()
{
  console.log('Cropper is ready.')
}
</script>
