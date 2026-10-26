<template>
  <div v-if="!originalDatabase">
    <MfAlert variant="warning" :title="$t('source.tip.notSelectedNode')"/>
  </div>

  <MfTabs v-else v-model:active-key="selectTab" @change="onChange">
    <MfTabPane item-key="info" :tab="$t('source.common.info')">
      <template #tab>
        <span class="inline-flex items-center gap-1.5">
          <InfoCircleOutlined :style="{ fontSize: '16px' }"/>
          {{ $t('source.common.info') }}
        </span>
      </template>
      <RouterView/>
    </MfTabPane>

    <MfTabPane item-key="structure" :tab="$t('source.common.structure')" :disabled="!originalTable">
      <template #tab>
        <span class="inline-flex items-center gap-1.5">
          <AppstoreOutlined :style="{ fontSize: '16px' }"/>
          {{ $t('source.common.structure') }}
        </span>
      </template>
      <RouterView/>
    </MfTabPane>

    <MfTabPane item-key="data" :tab="$t('source.common.tableData')" :disabled="!originalTable">
      <template #tab>
        <span class="inline-flex items-center gap-1.5">
          <TableOutlined :style="{ fontSize: '16px' }"/>
          {{ $t('source.common.tableData') }}
        </span>
      </template>
      <RouterView/>
    </MfTabPane>

    <MfTabPane item-key="statement" :tab="$t('source.common.statement')" :disabled="!originalTable">
      <template #tab>
        <span class="inline-flex items-center gap-1.5">
          <CloudServerOutlined :style="{ fontSize: '16px' }"/>
          {{ $t('source.common.statement') }}
        </span>
      </template>
      <RouterView/>
    </MfTabPane>

    <MfTabPane item-key="erDiagram" :tab="$t('source.common.erDiagram')" :disabled="!originalTable">
      <template #tab>
        <span class="inline-flex items-center gap-1.5">
          <CloudOutlined :style="{ fontSize: '16px' }"/>
          {{ $t('source.common.erDiagram') }}
        </span>
      </template>
      <RouterView/>
    </MfTabPane>
  </MfTabs>
</template>

<script lang="ts" setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AppstoreOutlined, CloudOutlined, CloudServerOutlined, InfoCircleOutlined, TableOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'MetadataContent' })

const route = useRoute()
const router = useRouter()

const selectTab = ref('')
const originalSource = ref<string | null>(null)
const originalDatabase = ref<string | null>(null)
const originalTable = ref<string | undefined>(undefined)

const handleInitialize = () => {
  originalSource.value = String(route.params?.source || '')
  originalDatabase.value = String(route.params?.database || '')
  originalTable.value = route.params?.table ? String(route.params.table) : undefined
  selectTab.value = String(route.meta.type || '')
}

const onChange = () => router.push(`/admin/source/${ originalSource.value }/d/${ originalDatabase.value }/t/${ selectTab.value }/${ originalTable.value }`)

watch(
    () => route.params.table,
    () => handleInitialize()
)

onMounted(() => handleInitialize())
</script>
