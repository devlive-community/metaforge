/** A shared report served on a real path: /share/report/{token} or /embed/report/{token}. */
export function sharedReportPath(pathname: string): { token: string; embed: boolean } | null
{
    const match = /^\/(share|embed)\/report\/([A-Za-z0-9_-]{32})\/?$/.exec(pathname)
    return match ? { token: match[2], embed: match[1] === 'embed' } : null
}

/** A data screen behind its public link: /share/screen/{token} or /embed/screen/{token}. */
export function sharedScreenPath(pathname: string): { token: string; embed: boolean } | null
{
    const match = /^\/(share|embed)\/screen\/([A-Za-z0-9_-]{32})\/?$/.exec(pathname)
    return match ? { token: match[2], embed: match[1] === 'embed' } : null
}

/** Public link addresses of a data screen for the owner to hand out. */
export const screenShareUrl = (origin: string, token: string) => `${ origin }/share/screen/${ token }`
export function screenEmbedCode(origin: string, token: string, title: string): string
{
    const escaped = title.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;')
    return `<iframe src="${ origin }/embed/screen/${ token }" title="${ escaped }" width="100%" height="600" style="border:0" loading="lazy"></iframe>`
}
