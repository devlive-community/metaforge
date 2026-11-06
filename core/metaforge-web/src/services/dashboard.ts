import { BaseService } from '@/services/base'
import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/dashboard'

export class DashboardService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    /** Opens one of the user's published data screens as their home page. */
    setHomePage(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/home/${ encodeURIComponent(code) }`)
    }

    /** Restores the default home page. */
    clearHomePage(): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/home`)
    }
}

export default new DashboardService()
