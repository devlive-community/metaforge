import { BaseService } from '@/services/base'
import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'
import type { RoleModel } from '@/model/role'
import { roleSaveTarget } from '@/utils/role-code'

const DEFAULT_PATH = '/api/v1/role'

class RoleService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    checkCode(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${DEFAULT_PATH}/check-code`, { code })
    }

    saveOrUpdate(configure: RoleModel, originalCode?: string): Promise<ResponseModel>
    {
        const target = roleSaveTarget(configure, originalCode)
        const http = new HttpUtils()
        return target.method === 'post'
            ? http.post(DEFAULT_PATH, configure)
            : http.put(`${DEFAULT_PATH}${target.suffix}`, configure)
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
