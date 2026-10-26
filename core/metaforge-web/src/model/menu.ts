export interface MenuModel
{
    id?: number | string
    name?: string
    code?: string
    description?: string
    url?: string
    group?: string
    sorted?: number
    type?: string
    parent?: number | string
    active?: boolean
    i18nKey?: string
    icon?: string
    redirect?: number | string
    new?: boolean
    createTime?: string
    updateTime?: string
}

export class MenuRequest
{
    public static of(): MenuModel
    {
        return {
            name: undefined,
            code: undefined,
            description: undefined,
            url: undefined,
            group: undefined,
            sorted: 0,
            type: undefined,
            parent: undefined,
            active: true,
            i18nKey: undefined,
            icon: undefined,
            redirect: undefined,
            new: false
        }
    }
}
