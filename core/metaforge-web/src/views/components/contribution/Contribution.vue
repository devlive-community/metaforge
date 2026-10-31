<template>
  <div class="dc-contribution flex flex-col gap-2">
    <div class="flex gap-2 overflow-x-auto pb-1">
      <div v-if="showWeek" class="flex flex-col gap-1.5 justify-end text-[10px] text-gray-500" :style="{ paddingTop: showMonth ? '18px' : 0 }">
        <div class="h-[14px]"></div>
        <div v-for="(label, index) in weekdayLabels"
             :key="index"
             class="h-[14px] leading-[14px]">{{ index % 2 === 0 ? label : '' }}</div>
      </div>

      <div class="flex flex-col gap-1.5">
        <div v-if="showMonth" class="relative h-[18px] text-[10px] text-gray-500">
          <span v-for="(month, index) in monthLabels"
                :key="index"
                class="absolute"
                :style="{ left: `${ month.offset }px` }">{{ month.label }}</span>
        </div>

        <div class="flex gap-1.5" :style="{ gap: `${ cellGap }px` }">
          <div v-for="(week, weekIndex) in weeks"
               :key="weekIndex"
               class="flex flex-col"
               :style="{ gap: `${ cellGap }px` }">
            <MfTooltip v-for="(day, dayIndex) in week"
                       :key="dayIndex"
                       :content="day ? tooltipText(day) : ''">
              <div class="rounded-[2px] cursor-pointer"
                   :style="{
                       width: `${ cellSize }px`,
                       height: `${ cellSize }px`,
                       backgroundColor: day ? colorFor(day.count) : 'transparent'
                   }"
                   @click="day && onSelect(day)">
              </div>
            </MfTooltip>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showLegend" class="flex items-center gap-1 justify-end text-[10px] text-gray-500">
      <span>Less</span>
      <span v-for="(color, index) in colorScheme"
            :key="index"
            class="rounded-[2px] inline-block"
            :style="{ width: '10px', height: '10px', backgroundColor: color }"></span>
      <span>More</span>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, computed, PropType } from 'vue'

interface ContributionData
{
  date: string
  count: number
}

export default defineComponent({
  name: 'DcContribution',
  props: {
    data: {
      type: Array as PropType<ContributionData[]>,
      default: () => []
    },
    colorScheme: {
      type: Array as PropType<string[]>,
      default: () => ['#EDF2F0', '#B9EAD9', '#87D9BE', '#49C399', '#0A8F67']
    },
    yearCount: {
      type: Number,
      default: 1
    },
    showLegend: {
      type: Boolean,
      default: true
    },
    showWeek: {
      type: Boolean,
      default: true
    },
    showMonth: {
      type: Boolean,
      default: true
    },
    cellSize: {
      type: Number,
      default: 16
    },
    cellGap: {
      type: Number,
      default: 4
    }
  },
  emits: ['onSelect'],
  setup(props, { emit })
  {
    const days = computed(() => {
      const result: Array<{ date: string; count: number; weekday: number }> = []
      const end = new Date()
      const start = new Date()
      start.setFullYear(end.getFullYear() - props.yearCount)

      const cursor = new Date(start)
      while (cursor <= end) {
        const key = cursor.toISOString().split('T')[0]
        const matched = props.data.find((item) => item.date === key)
        result.push({
          date: key,
          count: matched ? matched.count : 0,
          weekday: cursor.getDay()
        })
        cursor.setDate(cursor.getDate() + 1)
      }
      return result
    })

    const weeks = computed(() => {
      const result: Array<Array<{ date: string; count: number; weekday: number } | null>> = []
      let week: Array<{ date: string; count: number; weekday: number } | null> = Array(7).fill(null)

      const firstWeekday = days.value[0]?.weekday || 0
      for (let index = 0; index < firstWeekday; index++) {
        week[index] = null
      }

      days.value.forEach((day, index) => {
        week[day.weekday] = day
        if (day.weekday === 6 || index === days.value.length - 1) {
          result.push([...week])
          week = Array(7).fill(null)
        }
      })
      return result
    })

    const monthLabels = computed(() => {
      const labels = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
      const result: Array<{ label: string; offset: number }> = []
      let currentMonth = -1
      let weekIndex = 0

      days.value.forEach((day, index) => {
        if (index % 7 === 0) {
          const month = new Date(day.date).getMonth()
          if (month !== currentMonth) {
            result.push({ label: labels[month], offset: weekIndex * (props.cellSize + props.cellGap) })
            currentMonth = month
          }
          weekIndex++
        }
      })
      return result
    })

    const weekdayLabels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

    const colorFor = (count: number): string => {
      if (count <= 0) {
        return props.colorScheme[0]
      }
      if (count <= 3) {
        return props.colorScheme[1]
      }
      if (count <= 6) {
        return props.colorScheme[2]
      }
      if (count <= 9) {
        return props.colorScheme[3]
      }
      return props.colorScheme[4]
    }

    const tooltipText = (day: { date: string; count: number }): string => {
      // day.date 已是 ISO yyyy-MM-dd，直接展示，避免 locale 的斜杠格式
      return `${ day.date }: ${ day.count }`
    }

    const onSelect = (day: { date: string; count: number }) => {
      emit('onSelect', day)
    }

    return { days, weeks, monthLabels, weekdayLabels, colorFor, tooltipText, onSelect }
  }
})
</script>
