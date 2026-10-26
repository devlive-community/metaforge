<template>
  <div class="relative min-h-screen">
    <MfSkeleton v-if="loading"/>

    <MfTable v-else-if="data && data.length > 0"
             :columns="headers"
             :data-source="data"
             row-key="object_name"
             :empty-text="$t('common.noData')">
      <template #cell-object_nullable="{ record }">
        <MfSwitch v-model="record.object_nullable" size="sm" disabled/>
      </template>
    </MfTable>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import { createHeaders } from '@/views/pages/admin/source/components/TableUtils.ts'
import MetadataService from '@/services/metadata'

const { t } = useI18n()
const route = useRoute()

const headers = createHeaders({ t })

const loading = ref(false)
const data = ref<Array<any>>([])

const handleInitialize = () => {
  const code = route.params.source
  const database = route.params.database
  const table = route.params.table

  if (code && database && table) {
    loading.value = true
    MetadataService.getColumnsByTable(code as string, database as string, table as string)
                   .then((response) => {
                     if (response.status && response.data && response.data.isSuccessful) {
                       data.value = response.data.columns
                                            .filter((col: any) => col.type_name === 'column')
                     }
                     else {
                       MfMessage.error(response.message)
                     }
                   })
                   .finally(() => (loading.value = false))
  }
}

watch(
    () => route.params.table,
    () => handleInitialize()
)

onMounted(() => handleInitialize())
</script>
