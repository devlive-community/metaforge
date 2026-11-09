import { MfMessage } from 'metaforge-ui'
import { useUserStore } from '@/stores/user'
import UserService from '@/services/user'
import CommonUtils from '@/utils/common'
import { useRouteRedirect } from '@/composables/useRouteRedirect'

/**
 * Finishes a sign-in, by password or through a third-party provider: keeps the token, loads the menus
 * and returns to the page that asked for the sign-in (the `redirect` query).
 */
export function useSigninSession() {
  const userStore = useUserStore()
  const { restore } = useRouteRedirect()

  async function complete(auth: any): Promise<boolean> {
    localStorage.setItem(CommonUtils.token, JSON.stringify(auth))
    userStore.userInfo = auth
    const menus = await UserService.getMenus()
    if (!menus.status) {
      MfMessage.error(menus.message)
      userStore.logout()
      return false
    }
    userStore.updateMenu(menus.data)
    await restore()
    return true
  }

  return { complete }
}
