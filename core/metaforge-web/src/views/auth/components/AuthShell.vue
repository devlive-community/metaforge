<template>
  <BaseLayout>
    <div class="relative flex min-h-screen overflow-hidden bg-linear-to-br from-primary/10 via-surface to-primary/5">
      <!-- Soft waves along the bottom edge. -->
      <svg class="pointer-events-none absolute inset-x-0 bottom-0 h-40 w-full" viewBox="0 0 1440 160" preserveAspectRatio="none" aria-hidden="true">
        <path d="M0 96 C240 40 480 150 760 104 C1040 58 1240 120 1440 72 L1440 160 L0 160 Z" class="fill-primary/10"/>
        <path d="M0 128 C300 84 560 160 860 126 C1120 96 1300 140 1440 112 L1440 160 L0 160 Z" class="fill-primary/15"/>
      </svg>

      <div class="relative mx-auto flex w-full max-w-6xl items-center justify-center gap-16 px-4 py-10 lg:justify-between lg:px-10">
        <section class="hidden max-w-sm flex-col lg:flex">
          <h1 class="m-0 text-5xl font-bold tracking-tight text-content">MetaForge</h1>
          <p class="m-0 mt-5 text-2xl font-semibold leading-snug text-content">{{ $t('auth.brand.slogan') }}</p>
          <ul class="m-0 mt-8 flex list-none flex-col gap-4 p-0">
            <li v-for="key in FEATURES" :key="key" class="flex items-center gap-3 text-base text-content-secondary">
              <span class="inline-flex size-6 shrink-0 items-center justify-center rounded-full bg-primary/15 text-primary"><Check :size="14" :stroke-width="3"/></span>
              {{ $t(`auth.brand.feature.${key}`) }}
            </li>
          </ul>
          <AuthIllustration class="mt-10"/>
        </section>

        <main class="w-full max-w-[29rem] rounded-mf-lg border border-line/60 bg-surface/95 px-6 py-8 shadow-mf-lg backdrop-blur sm:px-10">
          <div class="flex justify-end">
            <AuthLanguage/>
          </div>

          <div class="mt-2 flex flex-col items-center">
            <div class="flex items-center gap-3">
              <img src="/static/images/logo.png" alt="" class="size-12 rounded-mf-lg object-contain"/>
              <span class="text-3xl font-bold text-content">MetaForge</span>
            </div>
            <p class="m-0 mt-3 text-sm text-content-secondary">{{ $t('auth.brand.tagline') }}</p>
          </div>

          <!-- Sign-in and sign-up are tabs; sign-up only while registration is open. -->
          <nav v-if="tab !== 'reset'" class="mt-8 grid border-b border-line" :class="registration ? 'grid-cols-2' : 'grid-cols-1'">
            <router-link v-for="item in tabs" :key="item" :to="{ path: `/auth/${item}`, query: redirectQuery }" replace
                         class="-mb-px border-b-2 pb-3 text-center text-lg font-semibold no-underline transition-colors"
                         :class="item === tab ? 'border-primary text-primary' : 'border-transparent text-content-secondary hover:text-content'">
              {{ $t(`auth.tab.${item}`) }}
            </router-link>
          </nav>
          <h2 v-else class="m-0 mt-8 border-b border-line pb-3 text-center text-lg font-semibold text-primary">{{ $t('auth.reset.title') }}</h2>

          <div class="mt-7">
            <div v-if="!options" class="flex items-center justify-center py-16"><MfSpinner/></div>
            <slot v-else :options="options"/>
          </div>
        </main>
      </div>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { Check } from '@lucide/vue'
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'
import AuthIllustration from './AuthIllustration.vue'
import AuthLanguage from './AuthLanguage.vue'
import { useAuthOptions } from '@/composables/useAuthOptions'
import { useRouteRedirect } from '@/composables/useRouteRedirect'

/** The sign-in, sign-up and reset pages: brand panel on the left, the card with the form on the right. */
const props = defineProps<{ tab: 'signin' | 'signup' | 'reset' }>()
const FEATURES = ['sources', 'convert', 'transfer', 'plugins'] as const

// Refreshed on every visit, so a change in system settings shows without a reload.
const { options, load } = useAuthOptions()
onMounted(load)
const { query: redirectQuery } = useRouteRedirect()
const registration = computed(() => !!options.value?.registration || props.tab === 'signup')
const tabs = computed(() => registration.value ? ['signin', 'signup'] as const : ['signin'] as const)
</script>
