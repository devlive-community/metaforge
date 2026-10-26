import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'
import { FilterModel } from '@/model/filter'

export abstract class BaseService
{
    private readonly baseUrl: string

    protected constructor(baseUrl: string)
    {
        this.baseUrl = baseUrl
    }

    /**
     * Filter the corresponding data according to the filter
     * <p>
     *   If the filter is empty, all data will be returned.
     *   Supported filters:
     *    <ul>
     *      <li>paging</li>
     *      <li>order</li>
     *    </ul>
     * </p>
     * @param {Filter} filter
     * @param filter
     */
    getAll(filter: FilterModel): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ this.baseUrl }/list`, filter)
    }

    /**
     * Filter the corresponding data according to the id
     * @param id
     */
    getById(id: number): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ this.baseUrl }/${ id }`)
    }

    getByCode(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ this.baseUrl }/info/${ code }`)
    }

    /**
     * Report an error or update data, if id > 0 in the data, it is updated, otherwise it is saved
     * @param configure
     */
    saveOrUpdate(configure: any): Promise<ResponseModel>
    {
        if (configure['id'] > 0 || configure['code']) {
            return new HttpUtils().put(this.baseUrl, configure)
        }
        else {
            return new HttpUtils().post(this.baseUrl, configure)
        }
    }

    deleteByCode(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ this.baseUrl }/${ code }`)
    }
}
