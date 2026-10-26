import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/configure/runtime'

export class RuntimeConfigureService
{
    list(category: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/list/${ category }`)
    }

    detail(category: string, name: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/detail/${ category }/${ name }`)
    }

    save(category: string, name: string, configure: Record<string, string>): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/save/${ category }/${ name }`, configure)
    }
}

export default new RuntimeConfigureService()
