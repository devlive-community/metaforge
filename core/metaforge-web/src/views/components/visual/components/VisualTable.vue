<template>
  <div ref="content" :style="{width: width, height: height}"/>
</template>
<script lang="ts">
import * as VTable from '@visactor/vtable'
import { createdTableHeader } from '../Utils'
import { cloneDeep } from 'lodash'
import { Configuration } from '../Configuration'
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'VisualTable',
  props: {
    configuration: {
      type: Object as () => Configuration | null
    },
    submitted: {
      type: Boolean,
      default: true
    },
    width: {
      type: String,
      default: () => '100%'
    },
    height: {
      type: String,
      default: () => '400px'
    }
  },
  created()
  {
    setTimeout(() => {
      try {
        if (this.configuration) {
          const columns = createdTableHeader(this.configuration.headers)
          const options = {
            records: this.configuration.columns,
            columns,
            hover: {
              highlightMode: 'row' as const
            },
            tooltip: {
              isShowOverflowTextTooltip: true
            },
            autoFillWidth: true
          }
          const tableInstance = new VTable.ListTable(this.$refs.content as HTMLElement, options as any)
          // Add cell tooltip
          tableInstance.on('mouseenter_cell', (args) => {
            const { col, row } = args
            const rect = tableInstance.getVisibleCellRangeRelativeRect({ col, row })
            tableInstance.showTooltip(col, row, {
              content: `${ tableInstance.getHeaderField(col, row) } : ${ tableInstance.getCellValue(col, row) }`,
              referencePosition: { rect, placement: VTable.TYPES.Placement.right },
              className: 'defineTooltip',
              style: { bgColor: 'black', color: 'white', arrowMark: true }
            })
          })
          if (this.submitted) {
            const cloneOptions = cloneDeep(this.configuration)
            cloneOptions.headers = []
            cloneOptions.columns = []
            this.$emit('change', cloneOptions)
          }
        }
      }
      catch (e) {
        console.warn(e)
      }
    })
  }
})
</script>
