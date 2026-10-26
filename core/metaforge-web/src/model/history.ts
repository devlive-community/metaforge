export interface HistoryModel
{
    id?: number
    state?: string
    content?: string
    message?: string
    createTime?: string
    endTime?: string
    elapsed?: number
    count?: number
    mode?: string
    plugin?: any
    code?: string
}