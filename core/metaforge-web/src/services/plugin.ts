import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/plugin'

class PluginService
{
    getPlugins(hasConfigure?: boolean): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }`, { hasConfigure })
    }

    /** With a version the server downloads through the statistics service and verifies sha256/size. */
    install(configure: { name: string, url: string, version?: string, sha256?: string, size?: number }): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/install`, configure)
    }

    uninstall(name: string): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/uninstall/${ encodeURIComponent(name) }`)
    }

    /** Confirmed download counts by plugin key; unknown plugins are absent. */
    getDownloads(keys: string[]): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/downloads`, { keys: keys.join(',') })
    }

    getReferences(): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/references`)
    }

    changeState(name: string, enabled: boolean): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ encodeURIComponent(name) }/state`, { enabled })
    }
}

export default new PluginService()
