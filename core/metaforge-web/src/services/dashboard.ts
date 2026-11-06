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

    /** Screens other users share with the signed-in user. */
    shared(): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/shared`)
    }

    getShare(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ encodeURIComponent(code) }/share`)
    }

    saveShare(code: string, share: unknown): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/${ encodeURIComponent(code) }/share`, share)
    }

    /** Users and roles to share with, matching a keyword. */
    shareCandidates(keyword: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/share/candidates`, { keyword })
    }

    /** A dynamic chart of a screen, for anyone the screen is shared with; it runs as the owner. */
    refreshChart(code: string, report: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ encodeURIComponent(code) }/reports/${ encodeURIComponent(report) }/refresh`)
    }

    /** Restores the default home page. */
    clearHomePage(): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/home`)
    }
}

export default new DashboardService()
