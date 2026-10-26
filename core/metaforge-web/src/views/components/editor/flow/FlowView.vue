<template>
  <div>
    <div style="height: 500px;">
      <VueFlow v-if="info" v-model="info.elements" :default-viewport="info.transform" :min-zoom="0.2" :max-zoom="4" :fit-view-on-init="info.fitViewOnInit">
        <Background pattern-color="#aaa" :gap="8"/>
      </VueFlow>
    </div>
  </div>
</template>
<script lang="ts">
import './style.css'
import { defineComponent } from 'vue'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Configuration } from '@/views/components/editor/flow/Configuration'

export default defineComponent({
  name: 'FlowView',
  props: {
    info: {
      type: Object as () => Configuration
    }
  },
  components: { VueFlow, Background }
})
</script>
