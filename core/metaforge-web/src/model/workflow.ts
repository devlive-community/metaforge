import { BaseModel } from '@/model/base.ts'

export interface WorkflowModel
    extends BaseModel
{
    content?: string
    state?: string
    message?: any
    work?: string
    startTime?: string
    endTime?: string
    elapsed?: number
    executor?: string
    configure?: any
}