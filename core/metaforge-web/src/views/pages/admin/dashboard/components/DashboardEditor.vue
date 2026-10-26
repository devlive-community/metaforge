<template>
  <MfCard :title="title">
    <template #extra>
      <MfSpace>
        <MfButton @click="visibleAddReport(true)">
          {{ $t('dashboard.common.addReport') }}
        </MfButton>

        <MfButton variant="primary" @click="visibleSave(true)">
          {{ $t('common.save') }}
        </MfButton>
      </MfSpace>
    </template>

    <div class="min-h-screen">
      <GridLayout ref="refLayout"
                  :layout="layouts"
                  :responsive="true"
                  :col-num="12"
                  :row-height="60"
                  :is-draggable="true"
                  :is-resizable="true"
                  :vertical-compact="true"
                  :use-css-transforms="true">
        <GridItem v-for="item in layouts"
                  :ref="el => set$Children(el)"
                  :key="item.i"
                  :x="item.x"
                  :y="item.y"
                  :w="item.w"
                  :h="item.h"
                  :i="item.i"
                  :min-h="3"
                  :min-w="3"
                  @resized="onResize">
          <MfCard class="h-full w-full">
            <template #header>{{ item.title ? item.title : $t('dataset.common.notSpecifiedTitle') }}</template>

            <template #extra>
              <MfButton variant="ghost" size="sm" class="rounded-full! text-danger!" @click="onRemove(item.i)">
                <DeleteOutlined :style="{ fontSize: '15px' }"/>
              </MfButton>
            </template>

            <VisualView v-if="item.original"
                        :width="calculateWidth(item)"
                        :height="calculateHeight(item)"
                        :code="item.node.code"
                        :configuration="JSON.parse(item.node.configure)"
                        :type="item.original?.type"
                        :query="item.original.type === 'DATASET' ? JSON.parse(item.original.query as string) : item.original.query"
                        :original="item?.original?.source?.code"/>

            <VisualView v-else
                        :width="calculateWidth(item)"
                        :height="calculateHeight(item)"
                        :code="item.node.code"
                        :configuration="JSON.parse(item.node.configure)"
                        :query="JSON.parse(item.node.query)"/>
          </MfCard>
        </GridItem>
      </GridLayout>
    </div>
  </MfCard>

  <MfModal v-model:open="configureVisible" :title="$t('common.configure')">
    <MfForm v-if="configureVisible && formState" :model="formState" layout="vertical" @finish="onSubmit">
      <MfFormItem name="name"
                  :label="$t('common.name')"
                  :rules="[{ required: true, message: $t('common.name') }]">
        <MfInput v-model="formState.name"/>
      </MfFormItem>

      <MfFormItem name="description" :label="$t('common.description')">
        <MfTextarea v-model="formState.description"/>
      </MfFormItem>

      <MfFormItem name="avatar" :label="$t('common.avatar')">
        <CropperHome :pic="formState?.avatar?.path" @update:value="onCropper"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfSpace>
          <MfButton @click="configureVisible = false">
            {{ $t('common.cancel') }}
          </MfButton>

          <MfButton variant="primary" type="submit" :loading="loading" :disabled="loading">
            {{ $t('common.save') }}
          </MfButton>
        </MfSpace>
      </div>
    </MfForm>
  </MfModal>

  <ChartContainer v-if="dataReportVisible"
                  :is-visible="dataReportVisible"
                  @change="onChange"
                  @close="visibleAddReport(false)"/>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import { GridItem, GridLayout } from 'vue3-grid-layout-next'
import DashboardService from '@/services/dashboard'
import { ReportModel } from '@/model/report.ts'
import VisualView from '@/views/components/visual/VisualView.vue'
import { DashboardModel, DashboardRequest } from '@/model/dashboard.ts'
import CropperHome from '@/views/components/cropper/CropperHome.vue'
import UploadService from '@/services/upload'
import ChartContainer from '@/views/pages/admin/dashboard/components/ChartContainer.vue'
import { DeleteOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'DashboardEditor' })

const props = withDefaults(defineProps<{ info?: DashboardModel | null }>(), { info: null })

const { t } = useI18n()
const router = useRouter()

const title = ref<string | null>(null)
const mapCache = new Map()
const columnNumber = 12
const layouts = ref<any[]>([])
const loading = ref(false)
const configureVisible = ref(false)
const formState = ref<DashboardModel | null>(null)
const dataReportVisible = ref(false)
const rowHeight = 60

const handleInitialize = () => {
  setTimeout(() => {
    if (!props.info) {
      title.value = t('dashboard.common.create')
      formState.value = DashboardRequest.of()
    }
    else {
      title.value = t('dashboard.common.modifyInfo').replace('$VALUE', String(props.info.name))
      formState.value = {
        code: props.info.code,
        name: props.info.name,
        configure: props.info.configure,
        version: props.info.version,
        description: props.info.description,
        avatar: props.info.avatar,
        reports: props.info.reports?.map(report => ({ code: report.code })) || []
      }
      layouts.value = JSON.parse(String(props.info.configure))
    }
  }, 300)
}

// Modified resize handler
const onResize = (i: string | number, w: number, h: number) => {
  const node = layouts.value.find((obj: { i: string }) => obj.i === i)
  if (node) {
    node.w = w
    node.h = h
  }
}

const onRemove = (i: string | number) => {
  layouts.value = layouts.value.filter((obj: { i: string }) => obj.i !== i)
}

// The method of calculating the width
const calculateWidth = (item: any): string => {
  const widthPercentage = (item.w * (100 / columnNumber))
  return `calc(${ widthPercentage }% - rem)` // Subtract margins
}

// How to calculate the height
const calculateHeight = (item: any): string => {
  const totalHeight = item.h * rowHeight
  return `${ totalHeight - 48 }px` // Subtract the height of the card head
}

const onCropper = (value: any) => {
  const configure = {
    code: formState.value?.code,
    mode: 'DASHBOARD',
    file: value
  }
  UploadService.upload(configure)
               .then((response) => {
                 if (response.status) {
                   if (formState.value) {
                     formState.value.avatar = response.data
                   }
                   MfMessage.success(t('common.successfully'))
                 }
                 else {
                   MfMessage.error(response.message)
                 }
               })
}

const onChange = (nodes: ReportModel[]) => {
  nodes.forEach((node: ReportModel) => {
    const newItem = {
      x: 0,
      y: 0,
      w: 3,
      h: 4,
      i: 'new-' + Date.now(),
      title: node.name,
      node: {
        id: node.code,
        configure: node.configure,
        code: node.dataset?.code,
        query: node?.query
      },
      original: node
    }
    layouts.value.push(newItem)
  })
}

const onSubmit = () => {
  if (formState.value) {
    formState.value.configure = JSON.stringify(layouts.value)
    layouts.value.forEach((item: { original: { code: any } }) => {
      formState.value?.reports?.push({ code: item.original.code })
    })
    formState.value.version = '1.0'
    loading.value = true
    DashboardService.saveOrUpdate(formState.value)
                    .then((response) => {
                      if (response.status) {
                        MfMessage.success(t('dashboard.tip.publishSuccess').replace('$VALUE', String(formState.value?.name)))
                        if (response.data) {
                          router.push(`/admin/dashboard/preview/${ response.data?.code }`)
                        }
                        else {
                          router.push('/console/dashboard')
                        }
                      }
                      else {
                        MfMessage.error(response.message)
                      }
                    })
                    .finally(() => (loading.value = false))
  }
}

const visibleSave = (opened: boolean) => {
  configureVisible.value = opened
}

const visibleAddReport = (opened: boolean) => {
  dataReportVisible.value = opened
}

const set$Children = (vm: any) => {
  if (vm && vm.i) {
    mapCache.set(vm.i, vm)
  }
}

handleInitialize()
</script>
