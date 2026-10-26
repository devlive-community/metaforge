export interface RoleModel
{
    id: number | undefined
    name: string | undefined
    description: string | undefined
    code?: string
    createTime?: string
    updateTime?: string
}

export class RoleRequest
{
    public static of(): RoleModel
    {
        return {
            id: undefined,
            name: undefined,
            description: undefined
        }
    }
}
