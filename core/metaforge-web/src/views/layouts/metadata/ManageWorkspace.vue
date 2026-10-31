<template>
  <MetadataContainer v-if="route.query.type === 'source' && typeof route.query.id === 'string' && route.query.id"/>
  <MfEmpty v-else variant="initial" :title="$t('source.manage.invalidRoute')"/>
</template>
<script setup lang="ts">
import { useRoute } from 'vue-router'
import MetadataContainer from './MetadataContainer.vue'
const route = useRoute()
</script>
