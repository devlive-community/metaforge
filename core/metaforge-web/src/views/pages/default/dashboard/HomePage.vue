<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import DashboardHome from './DashboardHome.vue'
import HomeScreen from './HomeScreen.vue'
/** The home page: the user's chosen data screen, or the default home (always with ?default=1). */
const route = useRoute(), userStore = useUserStore()
const screen = computed(() => route.query.default ? '' : userStore.homePage)
</script>
<template>
  <HomeScreen v-if="screen" :code="screen"/>
  <DashboardHome v-else/>
</template>
