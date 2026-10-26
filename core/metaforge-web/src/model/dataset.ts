export interface DatasetModel {
    id: number
    name: string | null | undefined
    description: string
    query: string
    syncMode: string
    expression: string
    state: Array<string>
    message: string
    tableName: string
    code: string
    scheduler: string
    executor: string
    totalRows: number
    totalSize: string
    lifeCycle: string
    lifeCycleColumn: string
    lifeCycleType: string
}
