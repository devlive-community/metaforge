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


    publish(code: string): Promise<ResponseModel> { return new HttpUtils().put(`${DEFAULT_PATH}/publish/${encodeURIComponent(code)}`) }
    catalog(): Promise<ResponseModel> { return new HttpUtils().get(`${DEFAULT_PATH}/catalog`) }
    statistics(): Promise<ResponseModel> { return new HttpUtils().get(`${DEFAULT_PATH}/statistics`) }
    validate(workflow: unknown): Promise<ResponseModel> { return new HttpUtils().post(`${DEFAULT_PATH}/validate`, workflow) }
    getRun(code: string, run: string): Promise<ResponseModel> { return new HttpUtils().get(`${DEFAULT_PATH}/run/${encodeURIComponent(code)}/${encodeURIComponent(run)}`) }
    runs(code: string): Promise<ResponseModel> { return new HttpUtils().get(`${DEFAULT_PATH}/runs/${encodeURIComponent(code)}`) }
    runLog(code: string, run: string): Promise<ResponseModel> { return new HttpUtils().get(`${DEFAULT_PATH}/log/${encodeURIComponent(code)}/${encodeURIComponent(run)}`) }

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
