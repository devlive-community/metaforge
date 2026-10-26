import { ResponseModel } from '@/model/response'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/captcha'

class CaptchaService
{
    /**
     * Retrieves a captcha for the given timestamp.
     *
     * @param {number} timestamp - the timestamp for the captcha request
     * @return {Promise<ResponseModel>} a promise that resolves to the response model
     */
    getCaptcha(timestamp: number): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${DEFAULT_PATH}?timestamp=${timestamp}`);
    }
}

export default new CaptchaService()
