import { BaseModel } from '@/model/base.ts'

export interface MessageModel
    extends BaseModel
{
    content?: string
    model?: string
    type?: string
    promptTokens?: number
    completionTokens?: number
    totalTokens?: number
}