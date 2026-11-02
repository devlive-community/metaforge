import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'
import type { AuthSettings, FsSettings, MailSettings, OAuthGroup } from '@/model/system-setting'

const DEFAULT_PATH = '/api/v1/admin/settings'

class SystemSettingService
{
    get(): Promise<ResponseModel>
    {
        return new HttpUtils().get(DEFAULT_PATH)
    }

    save(auth: AuthSettings, mail: MailSettings, oauth: OAuthGroup, fs: FsSettings): Promise<ResponseModel>
    {
        return new HttpUtils().put(DEFAULT_PATH, { auth, mail, oauth, fs })
    }

    /** Sends a test mail with the server as it is in the form. */
    testMail(mail: MailSettings, to: string): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/mail/test`, { mail, to })
    }
}

export default new SystemSettingService()
