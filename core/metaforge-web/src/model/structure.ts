export interface StructureModel
{
    title?: null | string
    value?: null | string
    catalog?: null
    database?: null | any
    databaseId?: string
    table?: null
    tableId?: string
    applyId?: null | number
    type?: null
    dataType?: null
    extra?: null
    engine?: null
    level?: null | StructureEnum
    loading?: false
    render?: any
    isKey?: null
    defaultValue?: null
    comment?: null
    origin?: any
    selected?: boolean
    contextmenu?: true
    code?: string
    isLeaf?: boolean
    children?: StructureModel[]
}

export enum StructureEnum
{
    TYPE,
    CATALOG,
    DATABASE,
    TABLE,
    COLUMN
}
