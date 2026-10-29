export interface DatasetColumn {
    id: number | null
    code?: string
    name: string
    aliasName: string
    original: string | null
    formula?: string | null
    type: string
    mode: string
    comment: string
    defaultValue: string | null
    position: number
    nullable: boolean
    length: number
    orderByKey: boolean
    partitionKey: boolean
    primaryKey: boolean
    samplingKey: boolean
    virtualColumn: boolean
    customColumn: boolean
}

/** Keep persisted identities and user properties when the same source field is refreshed. */
export function reconcileDatasetColumns(headers: string[], existing: DatasetColumn[]): DatasetColumn[] {
    const used = new Set(existing.map(column => column.name))
    const columns = headers.map((header, index) => {
        const previous = existing.find(column => !column.customColumn && column.original === header)
        if (previous) return { ...previous, position: index }
        // Default the storage column name to the source field name, sanitized
        // into a safe identifier and deduplicated against the used names.
        let candidate = header.replace(/\W+/g, '_').replace(/^_+|_+$/g, '')
        if (!candidate) candidate = `column_${index + 1}`
        while (used.has(candidate)) candidate = `${candidate}_2`
        const name = candidate
        used.add(name)
        return {
            id: null, name, aliasName: header.replace(/[()]/g, '_'), original: header,
            type: 'STRING', mode: 'DIMENSION', comment: header, defaultValue: null,
            position: index, nullable: false, length: 0, orderByKey: false,
            partitionKey: false, primaryKey: false, samplingKey: false,
            virtualColumn: false, customColumn: false
        }
    })
    const refreshed = new Map(columns.map(column => [column.original, column]))
    const retained = existing.flatMap(column => column.customColumn
        ? [column]
        : refreshed.has(column.original) ? [refreshed.get(column.original)!] : [])
    const added = columns.filter(column => !existing.some(previous => !previous.customColumn && previous.original === column.original))
    return [...retained, ...added].map((column, position) => ({ ...column, position }))
}

export function datasetColumnIssue(columns: DatasetColumn[]): string | null {
    if (!columns.length) return 'dataset.editor.noColumns'
    if (columns.some(column => !column.name?.trim() || !column.aliasName?.trim())) return 'dataset.editor.columnRequired'
    if (new Set(columns.map(column => column.name.trim())).size !== columns.length) return 'dataset.editor.duplicateName'
    if (new Set(columns.map(column => column.aliasName.trim())).size !== columns.length) return 'dataset.editor.duplicateAlias'
    if (!columns.some(column => !column.virtualColumn && (column.orderByKey || column.primaryKey))) return 'dataset.validator.specifiedColumn'
    if (columns.some(column => column.samplingKey && !column.orderByKey)) return 'dataset.tip.validatorSampling'
    if (columns.some(column => column.virtualColumn && !column.formula?.trim())) return 'dataset.editor.virtualExpressionRequired'
    return null
}

export interface DatasetQueryDraft {
    version: 1
    sourceCode: string
    query: string
    headers: string[]
}

/** Drafts contain field metadata only; query rows and connection credentials stay out of storage. */
export function parseDatasetQueryDraft(raw: string | null, sourceCode: string): DatasetQueryDraft | null {
    try {
        const draft = JSON.parse(raw || 'null')
        if (draft?.version !== 1 || draft.sourceCode !== sourceCode || typeof draft.query !== 'string'
            || !Array.isArray(draft.headers) || !draft.headers.every((header: unknown) => typeof header === 'string')) return null
        return draft
    }
    catch { return null }
}

export interface DatasetCreationEntry {
    origin: 'manual' | 'sql' | 'result'
    sourceCode?: string
    query?: string
    headers?: string[]
    rows?: Record<string, unknown>[]
    rowCount?: number
    elapsed?: number
}
