import { BaseService } from '@/services/base'

const DEFAULT_PATH = '/api/v1/report'

export class ReportService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }
}

export default new ReportService()