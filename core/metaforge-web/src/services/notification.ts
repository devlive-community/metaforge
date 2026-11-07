import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'
import { NotificationQuery } from '@/model/notification'

const DEFAULT_PATH = '/api/v1/notification'

/** The signed-in user's own notifications; the server scopes every call to that user. */
class NotificationService
{
    list(query: NotificationQuery): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/list`, query)
    }

    summary(): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/summary`)
    }

    mark(code: string, read: boolean): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/${ code }/read`, { read })
    }

    markAllRead(category?: string): Promise<ResponseModel>
    {
        const query = category ? `?category=${ encodeURIComponent(category) }` : ''
        return new HttpUtils().put(`${ DEFAULT_PATH }/read-all${ query }`)
    }

    remove(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/${ code }`)
    }

    removeRead(): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/read`)
    }
}

export default new NotificationService()
