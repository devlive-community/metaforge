<template>
  <div class="relative">
    <GridLayout ref="refLayout"
                :layout="layouts"
                :responsive="true"
                :col-num="12"
                :row-height="60"
                :is-draggable="false"
                :is-resizable="false"
                :vertical-compact="true"
                :use-css-transforms="true">
      <GridItem v-for="item in layouts"
                :key="item.i"
                :x="item.x"
                :y="item.y"
                :w="item.w"
                :h="item.h"
                :i="item.i"
                :min-h="3"
                :is-resizable="false"
                :min-w="3">
        <MfCard class="h-full w-full">
          <template #header>{{ item.title ? item.title : $t('dataset.common.notSpecifiedTitle') }}</template>

          <template #extra>
            <MfTooltip v-if="item.description" :content="item.description">
              <MfButton variant="ghost" size="sm" class="rounded-full! text-danger!">
                <DeleteOutlined :style="{ fontSize: '15px' }"/>
              </MfButton>
            </MfTooltip>
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
</template>

<script setup lang="ts">
import { GridItem, GridLayout } from 'vue3-grid-layout-next'
import VisualView from '@/views/components/visual/VisualView.vue'
import { DeleteOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'DashboardView' })

withDefaults(defineProps<{ layouts?: any[] }>(), {
  layouts: () => []
})

const columnNumber = 12
const rowHeight = 70

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
</script>
