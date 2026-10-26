import {BaseService} from '@/services/base'
import {HttpUtils} from "@/utils/http.ts";

const DEFAULT_PATH = '/api/v1/configure'

class ConfigurationService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    getExecutor()
    {
        return new HttpUtils().get(`${DEFAULT_PATH}/executor`)
    }
}

export default new ConfigurationService()
