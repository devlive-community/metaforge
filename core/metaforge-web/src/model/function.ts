export interface FunctionModel
{
    id?: number
    code?: string
    name?: string | undefined
    content?: string
    description?: string
    plugin?: string | string[] | undefined
    example?: string
    type?: string
    category?: string
    syntax?: string
    examples?: string
    parameters?: string
    returnType?: string
    notes?: string
    version?: string
    attributes?: string
    createTime?: string
    updateTime?: string
}

export enum FunctionType
{
    KEYWORD = 'KEYWORD',
    OPERATOR = 'OPERATOR',
    FUNCTION = 'FUNCTION'
}

export type FunctionKnowledgeKey =
    | 'name'
    | 'type'
    | 'category'
    | 'description'
    | 'syntax'
    | 'examples'
    | 'parameters'
    | 'return_type'
    | 'notes'
    | 'version'

/** Parsed source document: column headers plus raw rows. */
export interface FunctionSourceDocument
{
    columns: string[]
    rows: string[][]
    /** True when the parser only found bare lines (single implicit column). */
    implicitColumn?: boolean
}

export type FunctionImportDuplicate = 'skip' | 'overwrite'

export interface FunctionImportRequest
{
    plugin: string[]
    type: string
    entries: Record<string, string>[]
    duplicate: FunctionImportDuplicate
    continueOnError?: boolean
}

/** System fields a plugin/type can map to; the wizard renders these dynamically. */
export interface FunctionMappingField
{
    key: FunctionKnowledgeKey
    required?: boolean
}
