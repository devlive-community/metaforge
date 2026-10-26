import { AuthResponse } from '@/model/user/response/auth'
import Common from '@/utils/common'

export class TokenUtils
{
    /**
     * Get the authenticated user from local storage.
     *
     * @return {AuthResponse | undefined} The authenticated user object or undefined if not found.
     */
    public static getAuthUser(): AuthResponse | undefined
    {
        try {
            return JSON.parse(localStorage.getItem(Common.token) || '{}') as AuthResponse
        }
        catch (error) {
            return undefined
        }
    }

    /**
     * Get user menu from local storage.
     *
     * @return {any} The user menu object parsed from local storage, or undefined if an error occurs.
     */
    public static getUserMenu(): any
    {
        try {
            return JSON.parse(localStorage.getItem(Common.menu) || '{}')
        }
        catch (error) {
            return undefined
        }
    }
}
