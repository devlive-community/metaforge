<template>
  <SafeMarkdown v-if="widget.kind==='markdown'" :text="widget.text" :style="textStyle"/>
  <template v-else-if="widget.kind==='codeBlock'">
    <MfEmpty v-if="!widget.text.trim()" class="h-full" size="sm" :title="$t('dashboard.studio.enterText')"/>
    <SqlCodeBlock v-else-if="settings.language==='sql'" :code="widget.text" class="h-full overflow-auto" :style="textStyle"/>
    <pre v-else :style="textStyle" class="h-full overflow-auto whitespace-pre-wrap break-words rounded-mf-md bg-surface-hover p-3 font-mono text-xs"><code>{{ code }}</code></pre>
  </template>
  <div v-else-if="widget.kind==='worldClock'" class="flex h-full flex-col items-center justify-center gap-2">
    <span v-if="widget.text" class="max-w-full truncate text-sm font-medium" :title="widget.text">{{ widget.text }}</span>
    <DigitalClock :show-date="widget.clockDate" :timezone="settings.timezone" :clock-appearance="{ ...textStyle, fontSize: settings.fontSize * 1.5 + 'px' }"/>
    <span class="text-xs text-content-tertiary">{{ settings.timezone }}</span>
  </div>
  <ImageCompare v-else-if="widget.kind==='imageCompare'" :before="items[0]?.image||''" :after="items[1]?.image||''" :before-label="items[0]?.label" :after-label="items[1]?.label" :initial="settings.comparePosition" :style="textStyle"/>
  <MfEmpty v-else-if="!items.length" class="h-full" size="sm" :title="$t('dashboard.studio.addContentItems')"/>
  <div v-else-if="widget.kind==='iconLinks'" class="grid h-full content-start gap-2 overflow-auto" :style="{gridTemplateColumns:`repeat(${layout.columns},minmax(0,1fr))`}">
    <a v-for="item in items" :key="item.id" :href="item.url||undefined" target="_blank" rel="noopener noreferrer" class="flex min-w-0 flex-col items-center justify-center gap-2 rounded-mf-md border border-line p-3 text-center text-sm hover:bg-surface-hover focus-visible:outline-2 focus-visible:outline-primary" :style="textStyle">
      <img v-if="item.image" :src="item.image" alt="" class="size-9 object-contain"/><Link v-else :size="26" class="text-primary"/>
      <MfTooltip :content="item.label" class="min-w-0 max-w-full"><span class="block truncate">{{ item.label }}</span></MfTooltip>
      <p v-if="item.text" class="line-clamp-2 text-xs text-content-tertiary">{{ item.text }}</p>
    </a>
  </div>
  <ol v-else class="flex h-full min-h-0 gap-3 overflow-auto" :class="settings.direction==='horizontal'?'flex-row':'flex-col'">
    <li v-for="(item,index) in items" :key="item.id" class="relative flex min-w-0 gap-3" :class="settings.direction==='horizontal'?'min-w-32 flex-1 flex-col items-center text-center':'items-start'">
      <span class="grid size-8 shrink-0 place-items-center rounded-full border text-sm" :class="index<=active?'border-primary bg-primary/10 text-primary':'border-line text-content-tertiary'"><CircleCheck v-if="index<active" :size="19"/><span v-else>{{ index+1 }}</span></span>
      <div class="min-w-0 pb-3" :style="textStyle"><strong class="block break-words" :class="index===active?'text-primary':'text-content'">{{ item.label }}</strong><p v-if="item.text" class="mt-1 whitespace-pre-wrap break-words text-xs text-content-tertiary">{{ item.text }}</p></div>
    </li>
  </ol>
</template>
<script setup lang="ts">
import {computed} from 'vue'
import {CircleCheck,Link} from '@lucide/vue'
import {dashboardContentSettings,dashboardLayoutSettings,dashboardResourceUrl,type DashboardWidget} from '@/model/dashboard-layout'
import SqlCodeBlock from '@/views/components/sql/SqlCodeBlock.vue'
import SafeMarkdown from '@/views/components/content/SafeMarkdown.vue'
import ImageCompare from '@/views/components/content/ImageCompare.vue'
import DigitalClock from '@/views/components/content/DigitalClock.vue'
const props=defineProps<{widget:Pick<DashboardWidget, 'clockDate' | 'color' | 'contentOptions' | 'kind' | 'layoutOptions' | 'links' | 'text'>}>()
const settings=computed(()=>dashboardContentSettings(props.widget.contentOptions)),layout=computed(()=>dashboardLayoutSettings(props.widget.layoutOptions))
const items=computed(()=>{const items=(props.widget.links||[]).map(item=>({...item,url:dashboardResourceUrl(item.url),image:dashboardResourceUrl(item.image || '')}));return props.widget.kind==='iconLinks'?items.filter(item=>item.url):items})
const active=computed(()=>Math.min(settings.value.activeIndex,Math.max(0,items.value.length-1)))
const textStyle=computed(()=>({fontSize:settings.value.fontSize+'px',lineHeight:settings.value.lineHeight,fontWeight:settings.value.fontWeight==='bold'?700:settings.value.fontWeight==='medium'?500:400,color:props.widget.color||undefined}))
const code=computed(()=>{if(settings.value.language!=='json')return props.widget.text;try{return JSON.stringify(JSON.parse(props.widget.text),null,2)}catch{return props.widget.text}})
</script>
