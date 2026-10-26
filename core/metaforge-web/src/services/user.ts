import { UserRequest } from '@/model/user/request/user'
import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'
import { BaseService } from '@/services/base'
import { UserChatModel, UserEditor, UsernameModel, UserPasswordModel, UserRoleModel } from '@/model/user'
import { FilterModel } from '@/model/filter'

const DEFAULT_PATH_AUTH = '/api/auth'
const DEFAULT_PATH_USER = '/api/v1/user'
const DEFAULT_PATH_SOURCE = '/api/v1/source'
const DEFAULT_PATH_QUERY = '/api/v1/audit/plugin'

class UserService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH_USER)
    }

    /**
     * Sign in with the given user request configuration.
     *
     * @param {UserRequest} configure - the user request configuration
     * @return {Promise<ResponseModel>} a promise containing the response model
     */
    signin(configure: UserRequest): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH_AUTH }/signin`, configure)
    }

    /**
     * Signup user with the given configuration.
     *
     * @param {UserRequest} configure - the user configuration
     * @return {Promise<ResponseModel>} the response model
     */
    signup(configure: UserRequest): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH_AUTH }/signup`, configure)
    }

    /**
     * Get menus from the server.
     *
     * @return {Promise<ResponseModel>} The response model containing the menus.
     */
    getMenus(): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH_USER }/menus`)
    }

    /**
     * Get information through an asynchronous operation.
     *
     * @return {Promise<ResponseModel>} the response model from the operation
     */
    getInfo(): Promise<ResponseModel>
    {
        return new HttpUtils().get(DEFAULT_PATH_USER)
    }

    /**
     * Get the count of sources from the server.
     *
     * @return {Promise<ResponseModel>} the count of sources
     */
    getSourceCount(): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH_SOURCE }/admin/count`)
    }

    /**
     * Retrieves the count of queries from the server.
     *
     * @return {Promise<ResponseModel>} the response model promise
     */
    getQueryCount(): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH_QUERY }/admin/count`)
    }

    /**
     * Assigns a user role based on the provided configuration.
     *
     * @param {UserRoleModel} configure - the user role configuration
     * @return {Promise<ResponseModel>} a promise that resolves with the response from the server
     */
    assignRole(configure: UserRoleModel): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH_USER }/allocationRole`, configure)
    }

    getUserContribution()
    {
        return new HttpUtils().get(`${ DEFAULT_PATH_QUERY }/admin/contribution`)
    }

    getUserContributionRadar()
    {
        return new HttpUtils().get(`${ DEFAULT_PATH_QUERY }/admin/radar`)
    }

    getLogs(filter: FilterModel): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH_USER }/log`, filter)
    }

    changeEditor(configure: UserEditor): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH_USER }/changeEditorConfigure`, configure)
    }

    testChat(configure: UserChatModel): Promise<ResponseModel>
    {
      return new HttpUtils().post(`${ DEFAULT_PATH_USER }/testChat`, configure)
    }

    changeChart(configure: UserChatModel): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH_USER }/changeThirdConfigure`, configure)
    }

    checkUsername(username: string): Promise<ResponseModel>
    {
      return new HttpUtils().get(`${ DEFAULT_PATH_USER }/checkUsername?username=${ encodeURIComponent(username) }`)
    }

    changeUsername(configure: UsernameModel): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH_USER }/changeUsername`, configure)
    }

    changePassword(configure: UserPasswordModel): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH_USER }/changePassword`, configure)
    }

    uploadAvatar(configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().upload(`${ DEFAULT_PATH_USER }/uploadAvatar`, configure)
    }

    changeNotify(configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH_USER }/change-notify`, configure)
    }
}

export default new UserService()