import { ExecuteModel } from '@/model/execute';
import { BaseService } from '@/services/base'
import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/execute'

export class ExecuteService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    /**
     * Executes the given configuration and returns a Promise that resolves to a ResponseModel.
     *
     * @param {ExecuteModel} configure - the configuration to be executed
     * @param {any} cancelToken - a token to cancel the request
     * @return {Promise<ResponseModel>} a Promise that resolves to a ResponseModel
     */
    execute(configure: ExecuteModel, cancelToken: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${DEFAULT_PATH}`, JSON.stringify(configure), cancelToken)
    }
}

export default new ExecuteService()