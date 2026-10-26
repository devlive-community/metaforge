import { BaseService } from '@/services/base'
import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/role'

class RoleService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    getAllMenuById(id: number): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${DEFAULT_PATH}/${id}/menus`)
    }

    saveMenu(id: number, menus: any[]): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${DEFAULT_PATH}/${id}/menus`, menus)
    }
}

export default new RoleService()
