<template>
  <div class="relative h-screen">
    <div v-if="loading" class="flex items-center justify-center py-8">
      <MfSpinner size="lg" label="Loading..."/>
    </div>

    <ErDiagram v-else-if="!loading && options" :options="options"/>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import ErDiagram from '@/views/components/diagram/ErDiagram.vue'
import { ErDiagramOptions } from '@/views/components/diagram/ErDiagramOptions.ts'
import MetadataService from '@/services/metadata.ts'

defineOptions({ name: 'SourceTableErDiagram' })

const route = useRoute()

const loading = ref(false)
const options = ref<ErDiagramOptions>(null as unknown as ErDiagramOptions)

const handleInitialize = () => {
  const code = route?.params.source as string
  const database = route?.params.database as string
  const table = route?.params.table as string

  if (code && database && table) {
    loading.value = true
    MetadataService.getColumnsByTable(code, database, table)
                   .then((response) => {
                     if (response.status && response.data && response.data.isSuccessful) {
                       const tableName = response.data[0]
                       options.value = new ErDiagramOptions()
                       options.value.table = { id: tableName, name: tableName }
                       options.value.columns = response.data.columns
                                                       .filter((col: any) => col.type_name === 'column')
                                                       .map((col: any) => ({ id: col.object_name, name: col.object_name, type: col.object_data_type }))
                     }
                     else {
                       MfMessage.error(response.message)
                     }
                   })
                   .finally(() => (loading.value = false))
  }
}

watch(() => route?.params.table, () => handleInitialize())

handleInitialize()
</script>
