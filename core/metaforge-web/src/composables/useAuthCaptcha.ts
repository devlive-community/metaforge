import { ref } from 'vue'
import CaptchaService from '@/services/captcha'

/** The arithmetic captcha: a new image per timestamp; the answer goes with the form. */
export function useAuthCaptcha() {
  const timestamp = ref(0)
  const image = ref('')
  const loading = ref(false)

  async function refresh() {
    loading.value = true
    timestamp.value = Date.now()
    const response = await CaptchaService.getCaptcha(timestamp.value)
    loading.value = false
    image.value = response.status && response.data ? String(response.data.image || '') : ''
  }

  return { timestamp, image, loading, refresh }
}
