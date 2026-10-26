<template>
  <MfBreadcrumb>
    <MfBreadcrumbItem>
      <router-link to="/">{{ $t('common.home') }}</router-link>
    </MfBreadcrumbItem>
    <MfBreadcrumbItem v-for="item in breadcrumbs" :key="item.path">
      <router-link :to="item.path">
        {{ $t(`${ item.meta.title }`) }}
      </router-link>
    </MfBreadcrumbItem>
  </MfBreadcrumb>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

interface BreadcrumbModel
{
  path: string
  meta: {
    title: string
    isRoot: boolean
  }
}

defineOptions({ name: 'LayoutBreadcrumb' })

const route = useRoute()
const breadcrumbs = ref<BreadcrumbModel[]>([])

const getBreadcrumb = () => {
  breadcrumbs.value = []
  route.matched
       .filter(item => item.meta.title)
       .forEach(item => {
         breadcrumbs.value.push({
           path: item.path,
           meta: {
             title: item.meta.title as string,
             isRoot: item.meta.isRoot as boolean
           }
         })
       })
}

watch(() => route.path, () => getBreadcrumb())

getBreadcrumb()
</script>
