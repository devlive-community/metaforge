<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { Check, Info } from '@lucide/vue'
import { useI18n } from 'vue-i18n'
const props = defineProps<{ error?: string, title: string, step: number, steps: string[], busy: boolean, origin: string, saving?: boolean, validating?: boolean }>()
const emit = defineEmits<{ close: [], previous: [], next: [], save: [], back: [step: number] }>()
const { t } = useI18n()
const modal = ref<{scrollToTop:()=>void} | null>(null)
watch(()=>[props.step,props.error],()=>nextTick(()=>modal.value?.scrollToTop()))
</script>
<template>
  <MfModal ref="modal" :open="true" :title="title" :subtitle="t('dataset.prototype.stepDescription', [step + 1, steps[step]])" width="1180px" panel-class="md:w-[80vw]!" height="min(900px, calc(100dvh - 96px))" body-class="px-6 py-6" overlay-class="backdrop-blur-[2px]" :close-on-overlay="false" :close-on-esc="!saving" :close-disabled="saving" trap-focus @close="emit('close')">
    <template #after-header>
      <ol class="flex shrink-0 items-center justify-center border-b border-line px-4 py-5 sm:px-10 sm:py-6" :aria-label="t('dataset.editor.steps')">
        <template v-for="(label, index) in steps" :key="label">
          <li class="min-w-0">
            <button type="button" class="flex flex-col items-center gap-1.5 whitespace-nowrap text-xs sm:flex-row sm:gap-2.5 sm:text-sm" :class="index <= step ? 'font-semibold text-primary' : 'text-content-tertiary'" :disabled="index > step || busy" :aria-current="index === step ? 'step' : undefined" @click="emit('back', index)">
              <span class="flex size-7 shrink-0 items-center justify-center rounded-mf-full border" :class="index === step ? 'border-primary bg-primary text-on-primary' : index < step ? 'border-primary/20 bg-primary/10 text-primary' : 'border-line bg-surface text-content-tertiary'"><Check v-if="index < step" :size="14"/><span v-else>{{ index + 1 }}</span></span>{{ label }}
            </button>
          </li>
          <li v-if="index < steps.length - 1" aria-hidden="true" class="mx-2 h-px min-w-2 flex-1 bg-line sm:mx-5 sm:max-w-16"/>
        </template>
      </ol>
    </template>
    <slot/>
    <template #footer>
      <div class="flex w-full flex-wrap items-center justify-between gap-3">
        <span class="flex items-center gap-2 text-xs text-content-tertiary"><Info :size="14"/>{{ origin }}</span>
        <div class="ml-auto flex items-center gap-2">
          <MfButton variant="ghost" :disabled="saving" @click="emit('close')">{{ t('common.cancel') }}</MfButton>
          <MfButton v-if="step > 0" :disabled="busy" @click="emit('previous')">{{ t('dataset.editor.previous') }}</MfButton>
          <MfButton v-if="step < 3" variant="primary" :loading="validating" :disabled="busy" @click="emit('next')">{{ t('dataset.editor.next') }}<span aria-hidden="true">→</span></MfButton>
          <MfButton v-else variant="primary" :loading="saving" :disabled="busy" @click="emit('save')"><Check :size="15"/>{{ t('common.save') }}</MfButton>
        </div>
      </div>
    </template>
  </MfModal>
</template>
