import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/public/report'

/** Anonymous reads of a shared report by its share token. */
class PublicReportService
{
    view(token: string)
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ encodeURIComponent(token) }`)
    }

    refresh(token: string)
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ encodeURIComponent(token) }/refresh`)
    }

    data(token: string)
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ encodeURIComponent(token) }/data`)
    }
}

export default new PublicReportService()
