import { BaseService } from '@/services/base'

const DEFAULT_PATH = '/api/v1/notification'

class NotificationService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }
}

export default new NotificationService()
