import { BaseModel } from '@/model/base.ts'
import { UserModel } from '@/model/user.ts'

export interface ChatModel
    extends BaseModel
{
    avatar?: string
    description?: string
    user?: UserModel
}

export class ChatRequest
{
    public static of(): ChatModel
    {
        return {
            avatar: undefined,
            description: undefined
        }
    }
}
