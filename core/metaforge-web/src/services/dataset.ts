import { ResponseModel } from '@/model/response'
import { BaseService } from '@/services/base'
import { HttpUtils } from '@/utils/http'
import { FilterModel } from '@/model/filter'

const DEFAULT_PATH = '/api/v1/dataset'

export class DatasetService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    /**
     * Adhoc function for executing custom code.
     *
     * @param {string} code - custom code to execute
     * @param {any} configure - configuration object
     * @return {Promise<ResponseModel>} response from the execution
     */
    adhoc(code: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/adhoc/${ code }`, configure)
    }

    /**
     * Retrieves the columns for a given code.
     *
     * @param {string} code - The code to retrieve columns for.
     * @return {Promise<ResponseModel>} A promise that resolves to the response model containing the columns.
     */
    getColumnsByCode(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/columns/${ code }`)
    }

    /**
     * Rebuilds the specified item identified by the given ID.
     *
     * @param {string} code - the ID of the item to be rebuilt
     * @return {Promise<ResponseModel>} a Promise that resolves with the response from the rebuild request
     */
    rebuild(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/rebuild/${ code }`)
    }

    /**
     * Retrieves the history for a given code using the provided pagination model.
     *
     * @param {string} code - The code for which to retrieve the history.
     * @param {FilterModel} configure - The file model to use for the request.
     * @return {Promise<ResponseModel>} A promise that resolves with the response model containing the history data.
     */
    getHistory(code: string, configure: FilterModel): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/history/${ code }`, configure)
    }

    /**
     * Read the executor's task log of a given sync history record.
     *
     * @param {number} id - the dataset history id
     * @return {Promise<ResponseModel>} response containing the log lines (List<String>)
     */
    getHistoryLog(id: number): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/history/log/${ id }`)
    }

    /**
     * Stop a running sync task identified by the given history record id.
     *
     * @param {number} id - the dataset history id (must be RUNNING)
     * @return {Promise<ResponseModel>} success when the cancel flag has been set
     */
    stopHistory(id: number): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/history/stop/${ id }`)
    }

    /**
     * Sync data with the server using the provided id.
     * Optional `overrides` is a map of tunable executor fields the user wants to change for this run only.
     *
     * @param {string} code - The id of the data to sync
     * @param {Record<string, string>} [overrides] - per-invocation overrides for tunable fields
     * @return {Promise<ResponseModel>} A promise that resolves with the response from the server
     */
    syncData(code: string, overrides?: Record<string, string>): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/syncData/${ code }`, overrides || {})
    }

    /**
     * Fetch the tunable executor fields for this dataset (with current effective values prefilled).
     * Used by the sync dialog to render an editable form.
     */
    getSyncFields(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/syncFields/${ code }`)
    }

    /**
     * Clears the data associated with the given code.
     *
     * @param {string} code - the code for which data needs to be cleared
     * @return {Promise<ResponseModel>} a Promise that resolves with the response from the server
     */
    clearData(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/clearData/${ code }`)
    }
}

export default new DatasetService()
