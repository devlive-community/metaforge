<template>
  <span>{{ element?.aliasName ? element?.aliasName : element?.name }}</span>
  <span v-if="element?.expression">
      (
      <span v-if="element.expression === Expression.SUM" class="font-semibold">
        {{ $t('dataset.common.columnExpressionSum') }}
      </span>
      <span v-else-if="element.expression === Expression.COUNT" class="font-semibold">
        {{ $t('dataset.common.columnExpressionCount') }}
      </span>
      <span v-else-if="element.expression === Expression.MAX" class="font-semibold">
        {{ $t('dataset.common.columnExpressionMax') }}
      </span>
      <span v-else-if="element.expression === Expression.MIN" class="font-semibold">
        {{ $t('dataset.common.columnExpressionMin') }}
      </span>
      <span v-else-if="element.expression === Expression.AVG" class="font-semibold">
        {{ $t('dataset.common.columnExpressionAvg') }}
      </span>
      )
    </span>
</template>
<script lang="ts">
import { defineComponent, PropType } from 'vue'
import { Expression } from '@/views/pages/admin/dataset/Expression'

interface ColumnMetric
{
  name: string
  expression: Expression
  aliasName: string
}

export default defineComponent({
  name: 'DatasetColumnMetric',
  props: {
    element: {
      type: Object as unknown as PropType<ColumnMetric>
    }
  },
  computed: {
    Expression()
    {
      return Expression
    }
  }
})
</script>
