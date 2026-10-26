import { UserModel } from '@/model/user.ts'
import { ReportModel } from '@/model/report.ts'
import { AvatarModel } from '@/model/avatar.ts'

export interface DashboardModel
{
    id?: number
    name?: string
    configure?: string
    version?: string
    description?: string
    createTime?: string
    updateTime?: string
    user?: UserModel
    reports?: ReportModel[]
    code?: string
    avatar?: AvatarModel
}

export class DashboardRequest
{
    public static of(): DashboardModel
    {
        return {
            id: undefined,
            name: undefined,
            configure: undefined,
            version: undefined,
            description: undefined,
            reports: new Array<ReportModel>()
        }
    }
}
