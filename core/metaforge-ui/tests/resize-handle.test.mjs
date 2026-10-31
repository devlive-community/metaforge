import assert from 'node:assert/strict'
import test from 'node:test'
import {h,nextTick,ref} from 'vue'
import {component} from './component.mjs'
import {InteractionEvent,keydown,mountHost} from './interaction-host.mjs'
const Handle=await component('MfResizeHandle')
function mount(reverse=false){
  const width=ref(324), commits=[]
  const host=mountHost(h({setup:()=>()=>h(Handle,{modelValue:width.value,min:280,max:560,reverse,label:'Resize panel','onUpdate:modelValue':value=>width.value=value,onChange:value=>commits.push(value)})}))
  host.document.documentElement={style:{cursor:'auto',userSelect:'text'}}
  const handle=host.find(node=>node.props.role==='separator')
  const start=()=>{
    const event=new InteractionEvent('pointerdown',[handle],{button:0,pointerId:1,clientX:500})
    Object.defineProperty(event,'currentTarget',{value:handle})
    handle.props.onPointerdown(event)
  }
  const pointer=(type,x=500,id=1)=>host.document.defaultView.dispatchEvent(new InteractionEvent(type,[handle],{pointerId:id,clientX:x}))
  return {host,handle,width,commits,start,pointer}
}

test('keyboard controls resize in the panel direction and honor both bounds',async()=>{
  const {host,handle,width,commits}=mount(true)
  try{
    await keydown(host.document,handle,'ArrowLeft');assert.equal(width.value,332)
    await keydown(host.document,handle,'Home');assert.equal(width.value,280)
    await keydown(host.document,handle,'ArrowRight');assert.equal(width.value,280)
    await keydown(host.document,handle,'End');assert.equal(width.value,560)
    assert.deepEqual(commits,[332,280,280,560])
  }finally{host.unmount()}
})

test('pointer resizing clamps the width and releases selection locks on completion',async()=>{
  const {host,width,commits,start,pointer}=mount()
  try{
    start();assert.equal(host.document.documentElement.style.userSelect,'none')
    pointer('pointermove',900);await nextTick();assert.equal(width.value,560)
    pointer('pointermove',200,2);assert.equal(width.value,560)
    pointer('pointerup',900);assert.deepEqual(commits,[560])
    assert.equal(host.document.documentElement.style.cursor,'auto')
    assert.equal(host.document.documentElement.style.userSelect,'text')
    pointer('pointermove',100);assert.equal(width.value,560)
  }finally{host.unmount()}
})

test('Escape restores the width and unmounting during a drag removes its listeners',async()=>{
  const {host,width,commits,start,pointer}=mount(true)
  start();pointer('pointermove',450);await nextTick();assert.equal(width.value,374)
  host.document.defaultView.dispatchEvent(new InteractionEvent('keydown',[],{key:'Escape'}))
  await nextTick();assert.equal(width.value,324);assert.deepEqual(commits,[])
  start();pointer('pointermove',400);await nextTick()
  const document=host.document, window=document.defaultView, savedWidth=width.value
  host.unmount()
  assert.equal(document.documentElement.style.userSelect,'text')
  window.dispatchEvent(new InteractionEvent('pointermove',[],{pointerId:1,clientX:0}))
  assert.equal(width.value,savedWidth)
})
