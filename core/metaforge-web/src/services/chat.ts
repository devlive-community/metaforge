import { BaseService } from '@/services/base'
import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/chat'

class ChatService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    getMessages(id: number): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ id }/messages`)
    }
}

export default new ChatService()
