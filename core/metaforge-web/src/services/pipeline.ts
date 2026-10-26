import { BaseService } from '@/services/base'
import { ResponseModel } from '@/model/response.ts'
import { HttpUtils } from '@/utils/http.ts'
import { PipelineModel } from '@/model/pipeline.ts'

const DEFAULT_PATH = '/api/v1/pipeline'

class PipelineService
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

    submit(configure: PipelineModel): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/submit`, configure)
    }
}

export default new PipelineService()