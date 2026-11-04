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
