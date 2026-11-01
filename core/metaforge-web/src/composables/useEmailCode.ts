import { computed, onScopeDispose, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import AuthService from '@/services/auth'
import type { CodePurpose } from '@/model/system-setting'

/** Mails a code and counts down until another may be asked for. */
export function useEmailCode(purpose: CodePurpose) {
  const { t } = useI18n()
  const sending = ref(false)
  const seconds = ref(0)
  let timer: ReturnType<typeof setInterval> | undefined

  const label = computed(() => seconds.value > 0 ? t('auth.code.resend', { seconds: seconds.value }) : t('auth.code.send'))
  const disabled = computed(() => sending.value || seconds.value > 0)

  function countdown(from: number) {
    clearInterval(timer)
    seconds.value = from
    timer = setInterval(() => {
      seconds.value--
      if (seconds.value <= 0) clearInterval(timer)
    }, 1000)
  }

  // The page keeps the button disabled until its email field holds a valid address.
  async function send(email: string) {
    sending.value = true
    const response = await AuthService.sendCode(email.trim(), purpose)
    sending.value = false
    if (!response.status) { MfMessage.error(response.message); return false }
    MfMessage.success(t('auth.code.sent'))
    countdown(Number(response.data) || 60)
    return true
  }

  onScopeDispose(() => clearInterval(timer))
  return { sending, seconds, label, disabled, send }
}
