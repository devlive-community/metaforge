import type { ResponseModel } from './response'
export interface NativeEntry extends Record<string, unknown> {
  id: string; name: string; type: string; size: number; ttl?: number | null; modifiedAt?: number | null; attributes: Record<string, string>
}
export interface NativeRequest { namespace?: string; prefix?: string; cursor?: string; key?: string; limit?: number; preview?: boolean }
export interface NativePage { entries: NativeEntry[]; hasMore: boolean; nextCursor?: string | null; namespace?: string; prefix?: string }
export interface NativeDetail { entry: NativeEntry; content?: string | null; encoding?: string | null; truncated: boolean; rows: Record<string, string>[]; nextCursor?: string | null; hasMore: boolean }
export function nativePage(response: ResponseModel): NativePage {
  const data = response.data
  if (!response.status) throw new Error(String(response.message || 'Native browsing failed'))
  if (!data || !Array.isArray(data.entries) || data.entries.some((entry: any) => typeof entry.id !== 'string' || typeof entry.name !== 'string' || typeof entry.type !== 'string')
      || (data.hasMore && (typeof data.nextCursor !== 'string' || !data.nextCursor))) throw new Error('Invalid native browser response')
  return data
}
export function nativeDetail(response: ResponseModel): NativeDetail {
  if (!response.status) throw new Error(String(response.message || 'Native details failed'))
  const data = response.data
  if (!data?.entry || typeof data.entry.id !== 'string' || !Array.isArray(data.rows)
      || (data.hasMore && (typeof data.nextCursor !== 'string' || !data.nextCursor))) throw new Error('Invalid native detail response')
  return data
}
export function parentObjectPrefix(prefix: string): string {
  const current = prefix.endsWith('/') ? prefix.slice(0, -1) : prefix
  const slash = current.lastIndexOf('/')
  return slash < 0 ? '' : current.slice(0, slash + 1)
}
export function safeDownloadUrl(value: unknown): string {
  if (typeof value !== 'string') throw new Error('Invalid download link')
  const url = new URL(value)
  if (!['https:', 'http:'].includes(url.protocol)) throw new Error('Invalid download link')
  return url.href
}
