import { BaseService } from '@/services/base'
import { FilterModel } from '@/model/filter'
import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/audit/plugin'

class AuditService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    getPluginAudits(filter: FilterModel): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }`, filter)
    }

    getData(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/data/${ code }`)
    }
}

export default new AuditService()
