import { BaseService } from '@/services/base'
import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/i18n'

class I18nService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    getLocale(locale: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ locale }`)
    }
}

export default new I18nService()
