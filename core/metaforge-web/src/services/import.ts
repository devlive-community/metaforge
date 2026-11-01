import { HttpUtils } from '@/utils/http'
import { ResponseModel } from '@/model/response'

const DEFAULT_PATH = '/api/v1/import'

/** File import into a source's database: analyse, preview, run as a job and follow it. */
class ImportService
{
    parsers(): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/parsers`)
    }

    analyze(code: string, database: string, file: File, parser?: string): Promise<ResponseModel>
    {
        const form = new FormData()
        form.append('file', file)
        if (parser) form.append('parser', parser)
        return new HttpUtils().upload(`${ DEFAULT_PATH }/${ code }/${ database }/analyze`, form)
    }

    preview(code: string, database: string, plan: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/preview`, plan)
    }

    execute(code: string, database: string, plan: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/execute`, plan)
    }

    job(id: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/jobs/${ id }`)
    }
}

export default new ImportService()
