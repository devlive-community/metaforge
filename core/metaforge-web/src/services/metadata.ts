import { BaseService } from '@/services/base'
import { ResponseModel } from '@/model/response'
import HttpUtils from '@/utils/http'

const DEFAULT_PATH = '/api/v1/metadata'

class MetadataService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    getEngines(code: string): Promise<ResponseModel>
    {
        return HttpUtils.get(`${ DEFAULT_PATH }/${ code }/engines`)
    }

    getDataTypes(code: string): Promise<ResponseModel>
    {
        return HttpUtils.get(`${ DEFAULT_PATH }/${ code }/data-types`)
    }

    getDatabaseBySource(code: string): Promise<ResponseModel>
    {
        return HttpUtils.get(`${ DEFAULT_PATH }/${ code }/databases`)
    }

    getTablesByDatabase(code: string, database: string): Promise<ResponseModel>
    {
        return HttpUtils.get(`${ DEFAULT_PATH }/${ code }/${ database }/tables`)
    }

    getColumnsByTable(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return HttpUtils.get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/columns`)
    }

    getDatabase(code: string, database: string): Promise<ResponseModel>
    {
        return HttpUtils.get(`${ DEFAULT_PATH }/${ code }/${ database }`)
    }

    getTable(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return HttpUtils.get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }`)
    }

    getTableStatement(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return HttpUtils.get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/statement`)
    }

    updateAutoIncrement(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return HttpUtils.put(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/auto-increment`, configure)
    }

    createTable(code: string, database: string, configure: any): Promise<ResponseModel>
    {
        return HttpUtils.post(`${ DEFAULT_PATH }/${ code }/${ database }/create-table`, configure)
    }

    dropTable(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return HttpUtils.delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/drop-table`, configure)
    }

    truncateTable(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return HttpUtils.delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/truncate-table`, configure)
    }

    queryTable(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return HttpUtils.post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/query-table`, configure)
    }

    deleteData(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return HttpUtils.delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/delete-data`, configure)
    }

    updateData(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return HttpUtils.put(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/update-data`, configure)
    }

    exportData(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return HttpUtils.post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/export-data`, configure)
    }

    createColumn(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return HttpUtils.post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/create-column`, configure)
    }

    dropColumn(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return HttpUtils.delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/drop-column`, configure)
    }

    getColumn(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return HttpUtils.post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/get-column`, configure)
    }

    changeColumn(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return HttpUtils.put(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/change-column`, configure)
    }

    insertData(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return HttpUtils.post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/insert-data`, configure)
    }
}

export default new MetadataService()