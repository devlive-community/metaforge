import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/public/screen'

/** Anonymous reads of a data screen by its public link token. */
class PublicScreenService
{
    view(token: string)
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ encodeURIComponent(token) }`)
    }

    refresh(token: string, report: string)
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ encodeURIComponent(token) }/reports/${ encodeURIComponent(report) }/refresh`)
    }
}

export default new PublicScreenService()
