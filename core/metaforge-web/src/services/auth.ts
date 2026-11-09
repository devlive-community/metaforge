import { ResponseModel } from '@/model/response'
import { apiBaseUrl, HttpUtils } from '@/utils/http'
import type { CodePurpose, OAuthProviderId } from '@/model/system-setting'

const DEFAULT_PATH = '/api/auth'

/** What the sign-in pages offer, the email code and password reset flows, and third-party sign-in. */
class AuthService
{
    options(): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/options`)
    }

    sendCode(email: string, purpose: CodePurpose): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/code`, { email, purpose })
    }

    reset(body: { email: string; code: string; password: string }): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/reset`, body)
    }

    /** The server page that sends the browser on to the provider; `redirect` is where to return after signing in. */
    authorizeUrl(provider: OAuthProviderId, redirect?: string): string
    {
        const query = redirect ? `?redirect=${ encodeURIComponent(redirect) }` : ''
        return `${ apiBaseUrl() }${ DEFAULT_PATH }/oauth/${ provider }/authorize${ query }`
    }

    /** Exchanges the ticket a third-party sign-in came back with for the token, once. */
    redeem(ticket: string): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/oauth/ticket`, { ticket })
    }
}

export default new AuthService()
