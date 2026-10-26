import { BaseService } from '@/services/base'
import { FilterModel } from '@/model/filter'
import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/schedule'

class ScheduleService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    /**
     * Get the schedule history.
     *
     * @param {FilterModel} filter - the filter for the schedule history
     * @param {number} id - the ID of the schedule
     * @return {Promise<ResponseModel>} the response model promise
     */
    getScheduleHistory(filter: FilterModel, id: number): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${DEFAULT_PATH}/${id}/history`, filter)
    }
}

export default new ScheduleService()
