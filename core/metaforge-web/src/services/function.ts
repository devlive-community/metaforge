import { BaseService } from '@/services/base'
import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'
import { FunctionImportModel } from '@/model/function'

const DEFAULT_PATH = '/api/v1/function'

class FunctionService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    import(configure: FunctionImportModel): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${DEFAULT_PATH}/import`, configure)
    }

    getByPlugin(plugin: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${DEFAULT_PATH}/list/${plugin}`)
    }
}

export default new FunctionService()