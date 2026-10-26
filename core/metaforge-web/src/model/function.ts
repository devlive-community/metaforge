export interface FunctionModel
{
    id?: number
    name?: string | undefined
    content?: string
    description?: string
    plugin?: string | string[] | undefined
    example?: string
    type?: string
    createTime?: string
    updateTime?: string
}

export interface FunctionImportModel
{
    mode: FunctionMode
    content: string | undefined,
    plugin?: string | string[] | undefined
    type?: string | undefined
}

export enum FunctionMode
{
    txt = 'txt',
    url = 'url'
}
