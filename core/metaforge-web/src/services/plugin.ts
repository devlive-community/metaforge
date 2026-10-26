import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/plugin'

class PluginService
{
    getPlugins(hasConfigure?: boolean): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }`, { hasConfigure })
    }

    install(configure: { name: string, url: string }): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/install`, configure)
    }

    uninstall(name: string): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/uninstall/${ name }`)
    }
}

export default new PluginService()
