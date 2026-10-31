<template>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('user.common.list')"
               :columns="headers"
               :data-source="data"
               :loading="loading"
               :page-index="pageIndex"
               :page-size="pageSize"
               :total="dataCount"
               @refresh="initialize"
               @search="onSearch"
               @reset="onSearch"
               @page-change="onPageChange"
               @size-change="onSizeChange">
      <template #actions>
        <MfButton variant="primary" @click="infoPanel.open(null)">
          <Plus :size="16" />
          {{ $t('user.common.create') }}
        </MfButton>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'user'">
          <div class="flex flex-col items-center gap-1 py-1">
            <MfAvatar size="sm" :src="record.avatarConfigure?.path" :alt="record.username"/>
            <span class="max-w-full truncate text-xs text-content" :title="accountLabel(record)">
              <template v-if="record.name">{{ record.name }}: </template>{{ record.username }}
            </span>
          </div>
        </template>
        <template v-else-if="column.key === 'role'">
          <template v-if="record.roles?.length">
            <MfTag v-for="role in record.roles" :key="role.id">{{ role.name }}</MfTag>
          </template>
          <span v-else class="text-tertiary">-</span>
        </template>
        <template v-else-if="column.key === 'active'">
          <MfSwitch :model-value="record.active"
                    :disabled="record.system || record.id === currentUserId"
                    @update:model-value="(value: boolean) => onChangeActive(record, value)"/>
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace :wrap="false" :size="2">
            <MfTooltip :content="$t('user.common.assignRole')">
              <MfButton variant="ghost" size="sm" shape="circle" @click="rolePanel.open(record)">
                <ArrowUp :size="15" />
              </MfButton>
            </MfTooltip>

            <MfTooltip :content="$t('user.common.resetPassword')">
              <MfButton variant="ghost" size="sm" shape="circle" @click="passwordPanel.open(record)">
                <KeyRound :size="15" />
              </MfButton>
            </MfTooltip>

            <MfTooltip :content="$t('common.editData')">
              <MfButton variant="ghost" size="sm" shape="circle" @click="infoPanel.open(record)">
                <Pencil :size="15" />
              </MfButton>
            </MfTooltip>

            <MfTooltip :content="$t('user.common.delete')">
              <MfButton variant="ghost" size="sm" shape="circle" :disabled="record.system || record.id === currentUserId" @click="deletePanel.open(record)">
                <Trash2 :size="15" />
              </MfButton>
            </MfTooltip>
          </MfSpace>
        </template>
      </template>
    </DataTable>

  <UserRole v-if="rolePanel.visible.value"
            :is-visible="rolePanel.visible.value"
            :info="rolePanel.target.value"
            @close="rolePanel.close()"/>

  <UserPassword v-if="passwordPanel.visible.value"
                :is-visible="passwordPanel.visible.value"
                :info="passwordPanel.target.value"
                @close="passwordPanel.close()"/>

  <UserDelete v-if="deletePanel.visible.value"
              :is-visible="deletePanel.visible.value"
              :info="deletePanel.target.value"
              @close="deletePanel.close()"/>

  <UserInfo v-if="infoPanel.visible.value"
            :is-visible="infoPanel.visible.value"
            :info="infoPanel.target.value"
            @close="infoPanel.close()"/>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useHeaders } from './UserUtils'
import { UserModel } from '@/model/user'
import DataTable from '@/views/components/table/DataTable.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import { usePagedList } from '@/composables/usePagedList'
import { usePanelState } from '@/composables/usePanelState'
import UserInfo from '@/views/pages/system/user/UserInfo.vue'
import UserDelete from '@/views/pages/system/user/UserDelete.vue'
import UserRole from '@/views/pages/system/user/components/UserRole.vue'
import UserPassword from '@/views/pages/system/user/components/UserPassword.vue'
import UserService from '@/services/user'
import { useUserStore } from '@/stores/user'
import { MfMessage } from 'metaforge-ui'
import { ArrowUp, KeyRound, Pencil, Plus, Trash2 } from '@lucide/vue'

defineOptions({ name: 'UserHome' })

const tableFilters = useTableFilters('user')
const { headers } = useHeaders()
const { userInfo } = useUserStore()


const currentUserId = userInfo?.id

const rolePanel = usePanelState<UserModel>(() => initialize())
const passwordPanel = usePanelState<UserModel>(() => initialize())
const deletePanel = usePanelState<UserModel>(() => initialize())
const infoPanel = usePanelState<UserModel>(() => initialize())

const accountLabel = (record: any) => (record.name ? `${ record.name }: ${ record.username }` : String(record.username))

const {
    loading,
    data,
    pageIndex,
    pageSize,
    dataCount,
    initialize,
    onSearch,
    onPageChange,
    onSizeChange
  } = usePagedList<any>(filter => UserService.getAll(filter))

const onChangeActive = (record: any, value: boolean) => {
  UserService.changeActive(record.code, value)
             .then((response) => {
               if (response.status) {
                 record.active = value
               }
               else {
                 MfMessage.error(response.message)
               }
             })
}

onMounted(() => initialize())
</script>
