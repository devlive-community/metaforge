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
    /** The signed-in user's permission: OWNER, VIEW, EDIT or PUBLISH. */
    permission?: DashboardPermission
    /** Owner of a screen shared with the signed-in user. */
    ownerName?: string
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

export type DashboardPermission = 'OWNER' | 'VIEW' | 'EDIT' | 'PUBLISH'
export type DashboardGrant = Exclude<DashboardPermission, 'OWNER'>
/** Whether a permission allows at least another, in the order VIEW < EDIT < PUBLISH < OWNER. */
export const permits = (permission: DashboardPermission | undefined, needed: DashboardPermission): boolean => {
    const order: DashboardPermission[] = ['VIEW', 'EDIT', 'PUBLISH', 'OWNER']
    // Screens loaded before sharing existed carry no permission and are the user's own.
    return order.indexOf(permission ?? 'OWNER') >= order.indexOf(needed)
}
export interface DashboardShareMember { type: 'USER' | 'ROLE'; id: number; name?: string; permission?: DashboardGrant; /** Client-side row key: type and id. */ key?: string }
export interface DashboardPublicLink { enabled: boolean; embed: boolean; origins: string; expiresAt?: number | null; token?: string; rotate?: boolean }
export interface DashboardShare {
    everyone: DashboardGrant | null
    members: DashboardShareMember[]
    /** Widget ids viewers see; null shows the whole screen. */
    widgets: string[] | null
    publicLink: DashboardPublicLink
}
