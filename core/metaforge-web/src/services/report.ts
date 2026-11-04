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

    refreshChart(code: string)
    {
        return new HttpUtils().get(`${DEFAULT_PATH}/${encodeURIComponent(code)}/chart-refresh`)
    }
}

export default new ReportService()