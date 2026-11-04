import { HttpUtils } from '@/utils/http'
import { BaseService } from '@/services/base'

const DEFAULT_PATH = '/api/v1/report'

export class ReportService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    /** Owner only: public link, iframe embedding, allowed sites and link rotation. */
    share(code: string, configure: {
        enabled: boolean, embed: boolean, origins: string, rotate: boolean,
        expiresAt: number | null, showTitle: boolean, showDescription: boolean
    })
    {
        return new HttpUtils().put(`${DEFAULT_PATH}/${encodeURIComponent(code)}/share`, configure)
    }

    refreshChart(code: string)
    {
        return new HttpUtils().get(`${DEFAULT_PATH}/${encodeURIComponent(code)}/chart-refresh`)
    }
}

export default new ReportService()