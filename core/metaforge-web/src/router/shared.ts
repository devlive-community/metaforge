/** A shared report served on a real path: /share/report/{token} or /embed/report/{token}. */
export function sharedReportPath(pathname: string): { token: string; embed: boolean } | null
{
    const match = /^\/(share|embed)\/report\/([A-Za-z0-9_-]{32})\/?$/.exec(pathname)
    return match ? { token: match[2], embed: match[1] === 'embed' } : null
}
