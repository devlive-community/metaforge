import { apiBaseUrl, HttpUtils } from '@/utils/http'

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

    /** Column names and rows, when the owner offers the data on the public page. */
    table(token: string)
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ encodeURIComponent(token) }/table`)
    }

    /** The CSV download address; the server answers 404 unless the owner allows downloads. */
    downloadUrl(token: string): string
    {
        return `${ apiBaseUrl() }${ DEFAULT_PATH }/${ encodeURIComponent(token) }/download`
    }
}

export default new PublicReportService()
