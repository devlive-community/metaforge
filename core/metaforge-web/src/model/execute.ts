export interface ExecuteModel
{
    name: string
    label?: string
    content: string
    env?: object
    format?: string
    limit?: number
    mode?: string
    /** Table scope of a table's workbench: the server only runs statements on this table, in this database. */
    database?: string
    table?: string
}
