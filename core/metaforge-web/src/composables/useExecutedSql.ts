import { ref } from 'vue'
import { formatTimestamp, type ExecutedSql } from '@/model/executed-sql'

/**
 * Remembers the last DDL/DML operation of a page: every statement the server ran (returned in
 * `content`), the time spent (`processor.elapsed`) and the result, for the "executed SQL" dialog.
 */
export function useExecutedSql() {
  const executed = ref<ExecutedSql | null>(null)
  const visible = ref(false)

  /** Starts recording one operation; pass every request through `run`, and call `fail` on errors. */
  function record(info: Pick<ExecutedSql, 'operation' | 'table' | 'target'>) {
    const entry: ExecutedSql = { ...info, executedAt: formatTimestamp(new Date()), success: true, elapsed: 0, statements: [], message: '', result: null }
    const publish = () => { executed.value = { ...entry, statements: [...entry.statements] } }
    return {
      async run<T extends { status?: boolean; data?: any; message?: string }>(request: Promise<T>): Promise<T> {
        const started = performance.now()
        const response = await request
        const data = response?.data
        if (typeof data?.content === 'string' && data.content.trim()) entry.statements.push(data.content)
        const elapsed = Number(data?.processor?.elapsed)
        entry.elapsed += Number.isFinite(elapsed) && elapsed > 0 ? elapsed : Math.round(performance.now() - started)
        if (Array.isArray(data?.headers) && data.headers.length) entry.result = { headers: data.headers, rows: data.columns || [] }
        if (data?.message) entry.message = String(data.message)
        if (!response?.status || data?.isSuccessful === false) entry.success = false
        publish()
        return response
      },
      fail(error: unknown) {
        entry.success = false
        entry.message = error instanceof Error ? error.message : String(error)
        publish()
      },
    }
  }

  return { executed, visible, record }
}
