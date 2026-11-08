<template>
  <MfEmpty v-if="!text.trim()" class="h-full" size="sm" :title="$t('dashboard.studio.enterText')"/>
  <article v-else class="h-full space-y-2 overflow-auto whitespace-pre-wrap break-words">
    <template v-for="block in blocks" :key="block.key">
      <component :is="'h'+block.level" v-if="block.kind==='heading'" class="font-semibold" :style="{fontSize:(1.75-(block.level||1)*.12)+'em'}"><Inline :text="block.text"/></component>
      <pre v-else-if="block.kind==='code'" class="overflow-auto rounded-mf-md bg-surface-hover p-3 font-mono text-xs"><code>{{ block.text }}</code></pre>
      <component :is="block.ordered?'ol':'ul'" v-else-if="block.kind==='list'" class="space-y-1 pl-5" :class="block.ordered?'list-decimal':'list-disc'"><li v-for="(item,j) in block.items" :key="j"><Inline :text="item"/></li></component>
      <blockquote v-else-if="block.kind==='quote'" class="border-l-2 border-primary pl-3 text-content-secondary"><Inline :text="block.text"/></blockquote>
      <p v-else><Inline :text="block.text"/></p>
    </template>
  </article>
</template>
<script setup lang="ts">
import {computed,defineComponent,h} from 'vue'
import {markdownBlocks,markdownInline} from '@/model/dashboard-extended'
const props=defineProps<{text:string}>()
const blocks=computed(()=>markdownBlocks(props.text).map((block,key)=>({...block,key})))
const Inline=defineComponent({props:{text:{type:String,required:true}},setup:inner=>()=>markdownInline(inner.text).map(token=>{
  if(token.kind==='text')return token.text
  const tag=token.kind==='bold'?'strong':token.kind==='emphasis'?'em':token.kind==='code'?'code':'a'
  return h(tag,token.kind==='link'?{href:token.href,target:'_blank',rel:'noopener noreferrer',class:'text-primary underline'}:token.kind==='code'?{class:'rounded-mf-sm bg-surface-hover px-1 font-mono'}:{},token.text)
})})
</script>
