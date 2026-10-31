import { computed, reactive, toRefs } from 'vue'
import { useRoute } from 'vue-router'
import { sourceManageContext } from '@/model/source-manage'

/** Keep legacy dialogs reactive while the canonical workspace uses query parameters. */
export function useSourceManageRoute() {
  const route = useRoute()
  const context = computed(() => sourceManageContext(route))
  return reactive({ ...toRefs(route),
    params: computed(() => ({ ...route.params, source: context.value.source, database: context.value.database, table: context.value.table })),
    meta: computed(() => ({ ...route.meta, type: context.value.tab })),
  })
}
