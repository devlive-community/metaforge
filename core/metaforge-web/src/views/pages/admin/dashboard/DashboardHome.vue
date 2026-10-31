<template>
  <MfCard :title="$t('dashboard.common.list')">
    <template #extra>
      <MfTooltip :content="$t('dashboard.common.create')">
        <router-link to="/dashboard/info" target="_blank">
          <MfButton variant="primary" size="sm" shape="circle">
            <Plus :size="15" />
          </MfButton>
        </router-link>
      </MfTooltip>
    </template>

    <div class="mb-3 min-h-screen p-2">
      <div v-if="loading" class="flex items-center justify-center py-16">
        <MfSpinner size="lg"/>
      </div>
      <template v-else>
        <div class="grid grid-cols-4 gap-4">
          <div v-for="item in data" :key="item.code">
            <MfCard class="mb-4">
              <template #header>
                <router-link :to="`/dashboard/preview/${item.code}`" target="_blank">{{ item.name }}</router-link>
              </template>

              <template #extra>
                <MfSpace class="items-center">
                  <div v-if="item.description" class="cursor-pointer mt-1">
                    <MfTooltip :content="item.description">
                      <Info :size="20" />
                    </MfTooltip>
                  </div>

                  <MfDropdown trigger="click" placement="bottomRight">
                    <MfButton variant="ghost" size="sm" shape="circle">
                      <Settings :size="18" />
                    </MfButton>

                    <template #overlay>
                      <MfMenu>
                        <MfMenuItem>
                          <router-link :to="`/dashboard/info/${item.code}`" target="_blank" class="flex items-center space-x-2">
                            <Pencil :size="15" />
                            <span>{{ $t('dashboard.common.modify') }}</span>
                          </router-link>
                        </MfMenuItem>

                        <MfMenuItem @click="visibleDelete(true, item)">
                          <div class="flex items-center space-x-2">
                            <Trash2 :size="15" />
                            <span>{{ $t('dashboard.common.delete') }}</span>
                          </div>
                        </MfMenuItem>
                      </MfMenu>
                    </template>
                  </MfDropdown>
                </MfSpace>
              </template>

              <div class="p-3">
                <div class="aspect-video w-full relative overflow-hidden rounded-md flex items-center justify-center">
                  <img class="w-32 h-32 object-cover"
                       :src="`${item.avatar?.path ? item.avatar.path : '/static/images/dashboard.png'}`"
                       :alt="item.name"/>
                </div>

                <div class="text-xs text-muted-foreground text-right mt-2">{{ item.createTime }}</div>
              </div>
            </MfCard>
          </div>

          <div class="col-span-4">
            <div v-if="!loading && data.length === 0" class="text-center">
              {{ $t('common.noData') }}
            </div>
          </div>
        </div>

        <div v-if="data.length > 0" class="py-2 flex justify-end">
          <MfPagination :current="pageIndex"
                        :page-size="pageSize"
                        :total="dataCount"
                        show-size
                        :page-sizes="[12, 24, 36]"
                        @change="(page: number, size: number) => size !== pageSize ? onSizeChange(page, size) : onPageChange(page)"/>
        </div>
      </template>
    </div>
  </MfCard>

  <DashboardDelete v-if="deleteVisible"
                   :is-visible="deleteVisible"
                   :data="dataInfo"
                   @close="visibleDelete(false, null)"/>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import DashboardService from '@/services/dashboard'
import { FilterModel } from '@/model/filter'
import { DashboardModel } from '@/model/dashboard'
import DashboardDelete from '@/views/pages/admin/dashboard/DashboardDelete.vue'
import { Info, Pencil, Plus, Settings, Trash2 } from '@lucide/vue'

defineOptions({ name: 'DashboardHome' })

const filter: FilterModel = new FilterModel()
filter.size = 12

const loading = ref(false)
const deleteVisible = ref(false)
const data = ref<DashboardModel[]>([])
const pageIndex = ref(1)
const pageSize = ref(12)
const dataCount = ref(0)
const dataInfo = ref<DashboardModel | null>(null)

const handleInitialize = () => {
  loading.value = true
  DashboardService.getAll(filter)
                  .then((response) => {
                    if (response.status) {
                      data.value = response.data.content
                      dataCount.value = response.data.total
                      pageSize.value = response.data.size
                      pageIndex.value = response.data.page
                    }
                  })
                  .finally(() => (loading.value = false))
}

const fetchData = (value: number) => {
  filter.page = value
  filter.size = pageSize.value
  handleInitialize()
}

const onPageChange = (value: number) => fetchData(value)

const onSizeChange = (_current: number, size: number) => {
  pageSize.value = size
  fetchData(pageIndex.value)
}

const visibleDelete = (opened: boolean, info: DashboardModel | null) => {
  deleteVisible.value = opened
  dataInfo.value = info
  if (!opened) {
    handleInitialize()
  }
}

handleInitialize()
</script>
