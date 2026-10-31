import { BaseService } from '@/services/base'
import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'
import { FunctionImportRequest } from '@/model/function'

const DEFAULT_PATH = '/api/v1/function'

class FunctionService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    /**
     * Structured knowledge import: entries do not have to share the same fields,
     * unknown keys are stored server side as plugin-specific attributes.
     */
    import(configure: FunctionImportRequest): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${DEFAULT_PATH}/import`, configure)
    }

    /** Fetch a remote document through the server so CORS does not block the preview. */
    previewUrl(url: string): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${DEFAULT_PATH}/import/preview`, { url })
    }

    getByPlugin(plugin: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${DEFAULT_PATH}/list/${plugin}`)
    }
}

export default new FunctionService()
