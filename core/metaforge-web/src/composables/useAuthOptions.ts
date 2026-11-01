import { ref } from 'vue'
import AuthService from '@/services/auth'
import type { AuthOptions } from '@/model/system-setting'

const options = ref<AuthOptions | null>(null)
let pending: Promise<void> | null = null

/**
 * What the sign-in, sign-up and reset pages offer, as administrators set it in system settings.
 * Loaded once per visit to the auth pages; the pages render their forms once it is known.
 */
export function useAuthOptions() {
  function load() {
    pending ||= AuthService.options().then(response => {
      if (response.status) options.value = { ...response.data, providers: response.data?.providers || [] }
    }).finally(() => { pending = null })
    return pending
  }
  if (!options.value) load()
  return { options, load }
}
