export interface GridConfigure
{
    headers: [] | undefined
    columns: [] | undefined
    types?: [] | undefined
    height: number
    width: number
    showSeriesNumber: boolean
    elapsed?: number
    context?: string
    sourceId?: number
    query?: string
    code?: string
}
