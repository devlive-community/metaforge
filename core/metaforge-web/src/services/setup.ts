import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/setup'

class SetupService
{
    getStatus(): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${DEFAULT_PATH}/status`)
    }

    getEnvironment(): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${DEFAULT_PATH}/environment`)
    }

    testConnection(configure?: Record<string, unknown>): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${DEFAULT_PATH}/test`, configure || {})
    }

    install(configure: Record<string, unknown>): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${DEFAULT_PATH}/install`, configure)
    }
}

export default new SetupService()
