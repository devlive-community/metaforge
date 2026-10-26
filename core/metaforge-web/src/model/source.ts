export interface SourceModel
{
    id?: number
    name?: string
    description?: string
    type?: string
    protocol?: string
    host?: string
    port?: number
    username?: string
    password?: string
    catalog?: string
    database?: string
    ssl?: boolean
    publish?: boolean
    configure?: string
    usedConfig?: boolean
    version?: string
    available?: boolean
    message?: string
    createTime?: string
    updateTime?: string
    configures?: Map<string, string>
    schema?: any
    code?: string
}

export class SourceRequest
{
    public static of(): SourceModel
    {
        return {
            id: undefined,
            name: undefined,
            description: undefined,
            type: undefined,
            protocol: undefined,
            host: undefined,
            port: undefined,
            username: undefined,
            password: undefined,
            catalog: undefined,
            database: undefined,
            ssl: false,
            publish: false,
            configure: undefined,
            usedConfig: false,
            version: undefined,
            available: false,
            configures: new Map<string, string>()
        }
    }
}
