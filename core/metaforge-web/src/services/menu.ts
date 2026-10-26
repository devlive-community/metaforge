import { BaseService } from '@/services/base'

const DEFAULT_PATH = '/api/v1/menu'

class MenuService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }
}

export default new MenuService()
