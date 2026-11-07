/**
 * The app used hash routes (/#/query?x=1) before history routing (/query?x=1). Old bookmarks and
 * links keep working: a hash route becomes the path it names. Plain anchors (#section) stay.
 */
export function legacyHashTarget(hash: string): string | null
{
    return hash.startsWith('#/') ? hash.slice(1) : null
}

export function upgradeLegacyHashRoute(location: Location, history: History): void
{
    const target = legacyHashTarget(location.hash)
    if (target) history.replaceState(history.state, '', target)
}
