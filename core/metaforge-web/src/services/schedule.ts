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

    /**
     * Trigger the scheduled task manually.
     *
     * @param {number} id - the ID of the schedule
     * @return {Promise<ResponseModel>} the response model promise
     */
    trigger(id: number): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${DEFAULT_PATH}/${id}/trigger`, {})
    }

    /**
     * Toggle the built-in flag of a scheduled task.
     */
    changeSystem(code: string, system: boolean): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${DEFAULT_PATH}/${code}/system?system=${system}`)
    }

    /**
     * Get the upcoming execution times of the schedule.
     *
     * @param {number} id - the ID of the schedule
     * @return {Promise<ResponseModel>} the response model promise
     */
    getUpcomingTimes(id: number): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${DEFAULT_PATH}/${id}/upcoming`, {})
    }
}

export default new ScheduleService()
