import type { ResponseModel } from './response'

export type QueryResultState = 'loading' | 'initial' | 'error' | 'empty' | 'ready'

/** Presentation states follow the server response; a failed query is never an empty result. */
export function queryResultState(response: ResponseModel | null | undefined, running: boolean): QueryResultState {
    if (running) return 'loading'
    if (!response) return 'initial'
    if (!response.status) return 'error'
    return Array.isArray(response.data?.columns) && response.data.columns.length > 0 ? 'ready' : 'empty'
}
