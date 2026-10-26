import { BaseService } from '@/services/base'
import { UserQuestionModel } from '@/model/user'
import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/message'

class MessageService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    aiReply(configure: UserQuestionModel): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${DEFAULT_PATH}/ai/reply`, configure)
    }
}

export default new MessageService()