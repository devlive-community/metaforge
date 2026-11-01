import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'
import type { CodePurpose } from '@/model/system-setting'

const DEFAULT_PATH = '/api/auth'

/** What the sign-in pages offer, and the email code and password reset flows. */
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
}

export default new AuthService()
