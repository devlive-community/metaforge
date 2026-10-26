import { ResponseModel } from '@/model/response'
import { BaseService } from '@/services/base'
import { HttpUtils } from '@/utils/http'
import { SourceModel } from '@/model/source.ts'
import { isEmpty } from 'lodash'
import { FilterModel } from '@/model/filter.ts'

const DEFAULT_PATH_V2 = '/api/v2/source'

class SourceService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH_V2)
    }

    getPlugins(): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH_V2 }/plugins`)
    }

    testConnection(configure: any)
    {
        return new HttpUtils().post(`${ DEFAULT_PATH_V2 }/test`, configure)
    }

    getById(id: number): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH_V2 }/${ id }`)
    }

    saveOrUpdate(configure: SourceModel): Promise<ResponseModel>
    {
        configure.protocol = isEmpty(configure.protocol) ? 'HTTP' : configure.protocol
        if (configure['id'] > 0 || configure['code']) {
            return new HttpUtils().put(DEFAULT_PATH_V2, JSON.stringify(configure))
        }
        else {
            return new HttpUtils().post(DEFAULT_PATH_V2, JSON.stringify(configure))
        }
    }

    syncMetadata(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH_V2 }/syncMetadata/${ code }`)
    }

    getHistory(code: string, configure: FilterModel): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH_V2 }/getHistory/${ code }`, configure)
    }
}

export default new SourceService()
