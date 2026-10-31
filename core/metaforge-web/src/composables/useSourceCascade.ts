import { onMounted, ref, watch } from 'vue'
import { FilterModel } from '@/model/filter'
import SourceService from '@/services/source'
import MetadataService from '@/services/metadata'

/**
 * 数据源 → 数据库 → 数据表 三级级联选择（Vue3 Composable）。
 *
 * 导入/导出/对比等工具页共用：切换数据源自动重载库，切换库自动重载表；
 * 库/表随之清空。表列表仅保留基础表（type_name === 'table'）。
 */
export function useSourceCascade()
{
  const sourceCode = ref('')
  const databaseName = ref('')
  const tableName = ref('')
  const sourceOptions = ref<{ label: string; value: string }[]>([])
  const databaseOptions = ref<{ label: string; value: string }[]>([])
  const tableOptions = ref<{ label: string; value: string }[]>([])

  const loadDatabases = () => {
    databaseName.value = ''
    tableName.value = ''
    databaseOptions.value = []
    tableOptions.value = []
    if (!sourceCode.value) return
    MetadataService.getDatabaseBySource(sourceCode.value)
                   .then(response => {
                     if (response.status) {
                       databaseOptions.value = (response.data?.columns || [])
                           .map((item: any) => ({ label: item.object_name || item.schema_name, value: item.object_name || item.schema_name }))
                           .filter((item: any) => item.value)
                     }
                   })
  }

  const loadTables = () => {
    tableName.value = ''
    tableOptions.value = []
    if (!sourceCode.value || !databaseName.value) return
    MetadataService.getTablesByDatabase(sourceCode.value, databaseName.value)
                   .then(response => {
                     if (response.status) {
                       tableOptions.value = (response.data?.columns || [])
                           .filter((item: any) => item.type_name === 'table')
                           .map((item: any) => ({ label: item.object_name, value: item.object_name }))
                     }
                   })
  }

  watch(sourceCode, () => loadDatabases())
  watch(databaseName, () => loadTables())

  onMounted(() => {
    SourceService.getAll(new FilterModel())
                 .then(response => {
                   if (response.status) {
                     sourceOptions.value = (response.data.content || [])
                         .map((item: any) => ({ label: item.name, value: item.code }))
                   }
                 })
  })

  return { sourceCode, databaseName, tableName, sourceOptions, databaseOptions, tableOptions }
}
