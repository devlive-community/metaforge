<template>
  <MfCard>
    <template #header>
      <MfSpace>
        <MfButton>
          <router-link :to="`/admin/dataset/info/source/${configure.code}?tempId=${tempId}`" target="_blank">
              <span class="flex items-center">
                <PlusOutlined :style="{ fontSize: '15px' }"/>
                <span>{{ $t('common.dataset') }}</span>
              </span>
          </router-link>
        </MfButton>

        <MfButton @click="visualVisible = true">
          <BarChartOutlined :style="{ fontSize: '15px' }"/>
          <span>{{ $t('dataset.common.visual') }}</span>
        </MfButton>

        <MfTooltip class="mt-1" :content="$t('query.tip.pageShow')">
          <MfSwitch v-model="isPage" @change="onChange"/>
        </MfTooltip>

        <MfTooltip :content="$t('query.tip.smallTips')">
          <MfButton variant="ghost" shape="circle">
            <QuestionCircleOutlined :style="{ fontSize: '15px' }"/>
          </MfButton>
        </MfTooltip>
      </MfSpace>
    </template>

    <ag-grid-vue v-if="type === 'table'"
                 class="ag-theme-metaforge"
                 :key="timestamp"
                 :style="{width: configure.width + 'px', height: configure.height + 'px', 'margin-top': '2px'}"
                 :pagination="isPage"
                 :columnDefs="columnDefs"
                 :rowData="configure.columns"
                 :gridOptions="gridOptions as any"/>
  </MfCard>

  <GridVisual :is-visible="visualVisible" :configure="configure" @close="visualVisible = $event"/>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { AgGridVue } from 'ag-grid-vue3'
import 'ag-grid-community/styles/ag-grid.css'
import './ag-theme-metaforge.css'
import { useI18n } from 'vue-i18n'
import { GridConfigure } from '@/views/components/grid/GridConfigure'
import GridOptions from '@/views/components/grid/GridOptions'
import { GridColumn } from '@/views/components/grid/GridColumn'
import { ObjectUtils } from '@/utils/object'
import GridVisual from '@/views/components/grid/GridVisual.vue'
import { BarChartOutlined, PlusOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'GridTable' })

const props = withDefaults(defineProps<{ configure?: GridConfigure }>(), {
  configure: () => null as unknown as GridConfigure
})

const gridOptions = GridOptions.createDefaultOptions(useI18n())
const timestamp = ref(ObjectUtils.getTimestamp())

const columnDefs = ref<GridColumn[]>([])
const isPage = ref(true)
const type = ref('table')
const visualVisible = ref(false)
const tempId = ref<string | null>(null)

const handleInitialize = () => {
  if (props.configure) {
    const id = Math.random().toString(36).substring(2)
    tempId.value = id
    localStorage.setItem(`QueryContent_${ id }`, props.configure.query)
    props.configure.headers!.forEach((header: string) => {
      const columnDef: GridColumn = { headerName: header, field: header }
      columnDefs.value.push(columnDef)
    })
  }
}

const onChange = (value: boolean) => {
  timestamp.value = ObjectUtils.getTimestamp()
  isPage.value = value
}

handleInitialize()
</script>
