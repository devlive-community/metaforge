import axios from 'axios'
import router from '@/router'
import { ResponseModel } from '@/model/response'
import { TokenUtils } from '@/utils/token'
import { AuthResponse } from '@/model/user/response/auth'

axios.defaults.headers.post['Access-Control-Allow-Origin'] = '*'

export class HttpUtils
{
    private readonly configure

    constructor()
    {
        if (process.env.NODE_ENV === 'development') {
            axios.defaults.baseURL = 'http://localhost:9096'
        }
        else {
            axios.defaults.baseURL = window.location.protocol + '//' + window.location.hostname + (window.location.port ? ':' + window.location.port : '')
        }
        const auth: AuthResponse | undefined = TokenUtils.getAuthUser()
        if (auth) {
            this.configure = {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': auth.type + ' ' + auth.token
                },
                cancelToken: undefined,
                params: undefined
            }
        }
    }

    handlerSuccessful(result: any): ResponseModel
    {
        const data = result.data
        let messageData = data.message
        if (data.message instanceof Array) {
            const messages: string[] = []
            data.message.forEach((element: { field: string; message: string }) => {
                messages.push(element.field + ': ' + element.message)
            })
            messageData = messages.join('\r\n')
        }
        const response: ResponseModel = {
            code: data.code,
            data: data.data,
            message: messageData,
            status: data.status
        }

        // If the authorization key does not match, clear the local token reload page
        if (response.code === 4003) {
            const currentPath = router.currentRoute.value.path
            router.push({
                path: '/common/403',
                query: {
                    redirect: currentPath
                },
                replace: true
            })
        }

        if (response.code === 5000) {
            this.handlerMessage(response.message)
        }
        return response
    }

    handlerFailed(error: any): ResponseModel
    {
        const response: ResponseModel = {
            code: 0,
            message: error.message,
            status: false
        }

        // Current route path, avoid repeated redirection
        const currentPath = router.currentRoute.value.path
        const isAlreadyOnErrorPage = currentPath.startsWith('/common/not_network')

        if (error.code === 'ERR_NETWORK' && !isAlreadyOnErrorPage) {
            // Record the original page path of the user
            const originalPath = router.currentRoute.value.fullPath
            router.push({
                path: '/common/not_network',
                query: {
                    redirect: originalPath
                },
                replace: true
            })
        }

        return response
    }

    get(url: string, params?: any): Promise<ResponseModel>
    {
        return new Promise((resolve) => {
            if (this.configure) {
                this.configure.params = params
            }
            axios.get(url, this.configure)
                 .then(result => {
                     resolve(this.handlerSuccessful(result))
                 }, error => {
                     this.handlerMessage(error.message)
                     resolve(this.handlerFailed(error))
                 })
        })
    }

    post(url: string, data = {}, cancelToken?: any): Promise<ResponseModel>
    {
        return new Promise((resolve) => {
            if (this.configure) {
                this.configure.cancelToken = cancelToken
            }
            axios.post(url, data, this.configure)
                 .then(result => {
                     resolve(this.handlerSuccessful(result))
                 }, error => {
                     this.handlerMessage(error.message)
                     resolve(this.handlerFailed(error))
                 })
        })
    }

    upload(url: string, data = {}, cancelToken?: any): Promise<ResponseModel>
    {
        return new Promise((resolve) => {
            if (this.configure) {
                this.configure.cancelToken = cancelToken
                this.configure.headers['Content-Type'] = 'multipart/form-data'
            }
            axios.post(url, data, this.configure)
                 .then(result => {
                     resolve(this.handlerSuccessful(result))
                 }, error => {
                     this.handlerMessage(error.message)
                     resolve(this.handlerFailed(error))
                 })
        })
    }

    put(url: string, data = {}): Promise<ResponseModel>
    {
        return new Promise((resolve) => {
            axios.put(url, data, this.configure)
                 .then(result => {
                     resolve(this.handlerSuccessful(result))
                 }, error => {
                     this.handlerMessage(error.message)
                     resolve(this.handlerFailed(error))
                 })
        })
    }

    delete(url: string, data = {}): Promise<ResponseModel>
    {
        return new Promise((resolve) => {
            axios.delete(
                url,
                {
                    data: data,
                    ...this.configure
                }
            ).then(result => {
                resolve(this.handlerSuccessful(result))
            }, error => {
                this.handlerMessage(error.message)
                resolve(this.handlerFailed(error))
            })
        })
    }

    private handlerMessage(message: string)
    {
        if (message) {
            console.debug(message)
        }
    }

    getAxios()
    {
        return axios
    }

    /**
     * Execute multiple requests concurrently
     * @param promises Array of HttpUtils request promises
     * @returns Promise that resolves with an array of ResponseModel
     */
    all(promises: Promise<ResponseModel>[]): Promise<ResponseModel[]>
    {
        return Promise.all(promises)
    }

    /**
     * Spread helper function to handle multiple responses
     * @param callback Function to handle spread response arguments
     * @returns Function that accepts response array and applies the callback
     */
    spread<T>(callback: (...args: ResponseModel[]) => T): (array: ResponseModel[]) => T
    {
        return function wrap(arr) {
            return callback.apply(null, arr)
        }
    }
}

// Export a default instance
export default new HttpUtils()
