import { BaseService } from '@/services/base.ts'
import { HttpUtils } from '@/utils/http.ts'
import { ResponseModel } from '@/model/response.ts'

const DEFAULT_PATH = '/api/v1/upload'

class UploadService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    upload(configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().upload(`${ DEFAULT_PATH }`, configure)
    }
}

export default new UploadService()
