import { HttpUtils } from '@/utils/http'
import { ResponseModel } from '@/model/response'

const DEFAULT_PATH = '/api/v1/format'

class FormatService
{
    formatSql(configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${DEFAULT_PATH}/sql`, configure)
    }
}

export default new FormatService()