import { BaseService } from '@/services/base'

const DEFAULT_PATH = '/api/v1/dashboard'

export class DashboardService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }
}

export default new DashboardService()
