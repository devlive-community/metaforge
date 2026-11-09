<template>
  <AuthShell tab="oauth">
    <div v-if="!error" class="flex flex-col items-center gap-3 py-12 text-sm text-content-secondary">
      <MfSpinner/>
      <span>{{ $t('auth.oauth.signingIn') }}</span>
    </div>
    <div v-else class="flex flex-col items-center gap-5">
      <MfResult status="warning" :title="$t('auth.oauth.failed')" :sub-title="$t(`auth.oauth.error.${error}`)"/>
      <MfButton variant="primary" size="lg" block @click="back">{{ $t('auth.oauth.back') }}</MfButton>
    </div>
  </AuthShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthService from '@/services/auth'
import { useRouteRedirect } from '@/composables/useRouteRedirect'
import { useSigninSession } from '@/composables/useSigninSession'
import { oauthError } from '@/model/system-setting'
import type { OAuthError } from '@/model/system-setting'
import AuthShell from '../components/AuthShell.vue'

defineOptions({ name: 'AuthOAuth' })

/** Where a third-party sign-in comes back to: the ticket is exchanged for the token once, then the page returns to `redirect`. */
const route = useRoute()
const router = useRouter()
const { query: redirectQuery } = useRouteRedirect()
const { complete } = useSigninSession()
const error = ref<OAuthError | null>(null)

const back = () => router.replace({ path: '/auth/signin', query: redirectQuery.value })

onMounted(async () => {
  const ticket = route.query.ticket
  if (route.query.error || typeof ticket !== 'string' || !ticket) {
    error.value = oauthError(route.query.error)
    return
  }
  const response = await AuthService.redeem(ticket)
  if (!response.status) {
    error.value = 'expired'
    return
  }
  if (!await complete(response.data)) error.value = 'failed'
})
</script>
