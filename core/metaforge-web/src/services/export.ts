import { HttpUtils } from '@/utils/http'
import { ResponseModel } from '@/model/response'

const DEFAULT_PATH = '/api/v1/export'

/** Export of a database's tables: formats, samples, the plan check, the job and its file. */
class ExportService
{
    formats(): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/formats`)
    }

    sample(code: string, database: string, plan: any, table: string, rows = 3): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ encodeURIComponent(code) }/${ encodeURIComponent(database) }/sample?table=${ encodeURIComponent(table) }&rows=${ rows }`, plan)
    }

    check(code: string, database: string, plan: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ encodeURIComponent(code) }/${ encodeURIComponent(database) }/check`, plan)
    }

    execute(code: string, database: string, plan: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ encodeURIComponent(code) }/${ encodeURIComponent(database) }/execute`, plan)
    }

    job(id: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/jobs/${ id }`)
    }

    file(id: string)
    {
        return new HttpUtils().download(`${ DEFAULT_PATH }/jobs/${ id }/file`)
    }
}

export default new ExportService()
