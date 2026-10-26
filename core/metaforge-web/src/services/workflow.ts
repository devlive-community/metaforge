import { BaseService } from '@/services/base'
import { ResponseModel } from '@/model/response.ts'
import { HttpUtils } from '@/utils/http.ts'

const DEFAULT_PATH = '/api/v1/workflow'

class WorkflowService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    getLogger(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/log/${ code }`)
    }

    stop(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/stop/${ code }`)
    }

    restart(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/restart/${ code }`)
    }
}

export default new WorkflowService()
