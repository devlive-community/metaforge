import type { ReportModel } from '@/model/report'

/** Sites allowed to embed a report: scheme and host, optional *. subdomain wildcard and port. */
export const SHARE_ORIGIN = /^https?:\/\/(\*\.)?[a-z0-9-]+(\.[a-z0-9-]+)*(:\d{1,5})?$/i

/** Dataset reports and query charts can be shared; earlier query reports ran their SQL in the browser. */
export function isShareableReport(report: Pick<ReportModel, 'type' | 'configure'>): boolean
{
    if (report.type === 'DATASET') return true
    try {
        const configure = report.configure ? JSON.parse(report.configure) : null
        return !!configure?.queryChart && typeof configure.queryChart === 'object'
    }
    catch {
        return false
    }
}

export function reportShareUrl(origin: string, token: string): string
{
    return `${ origin }/share/report/${ token }`
}

export function reportEmbedCode(origin: string, token: string, title: string): string
{
    const escaped = title.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;')
    return `<iframe src="${ origin }/embed/report/${ token }" title="${ escaped }" width="100%" height="480" style="border:0" loading="lazy"></iframe>`
}

/** How long a share link stays valid: forever, a number of days from now, or until the end of a chosen day. */
export type ShareExpiry = 'never' | '1d' | '7d' | '30d' | 'date'

const DAY = 24 * 60 * 60 * 1000

/** Epoch milliseconds the link expires at, or null when it never expires. A date ends at 23:59:59 local time. */
export function shareExpiresAt(choice: ShareExpiry, date: string, now = Date.now()): number | null
{
    if (choice === 'never') return null
    if (choice === 'date') {
        const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date)
        return match ? new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]), 23, 59, 59, 999).getTime() : null
    }
    return now + Number.parseInt(choice, 10) * DAY
}

/** The local YYYY-MM-DD of a timestamp, for the date picker. */
export function localDate(time: number): string
{
    const value = new Date(time)
    const pad = (part: number) => String(part).padStart(2, '0')
    return `${ value.getFullYear() }-${ pad(value.getMonth() + 1) }-${ pad(value.getDate()) }`
}

export function isShareExpired(expiresAt: number | null | undefined, now = Date.now()): boolean
{
    return typeof expiresAt === 'number' && expiresAt <= now
}

/** YYYY-MM-DD HH:mm:ss in local time, the date style used across the app. */
export function formatShareTime(time: number): string
{
    const value = new Date(time)
    const pad = (part: number) => String(part).padStart(2, '0')
    return `${ localDate(time) } ${ pad(value.getHours()) }:${ pad(value.getMinutes()) }:${ pad(value.getSeconds()) }`
}
