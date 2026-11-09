/**
 * Page paths of data screens. Every link goes through these helpers so the paths live in one place:
 *   /screen                list
 *   /screen/design         design a new screen
 *   /screen/:code/design   design an existing screen
 *   /screen/:code          view the published screen
 */
export const SCREEN_LIST_PATH = '/screen'
export const SCREEN_NEW_PATH = '/screen/design'
const segment = (code: string) => encodeURIComponent(code)
export const screenViewPath = (code: string) => `/screen/${ segment(code) }`
export const screenDesignPath = (code?: string | null) => code ? `/screen/${ segment(code) }/design` : SCREEN_NEW_PATH

/** Where a former /dashboard address now lives, keeping its query and hash; undefined when it is not one. */
export function legacyScreenPath(path: string): string | undefined {
    const match = path.match(/^\/dashboard(\/[^?#]*)?([?#].*)?$/i)
    if (!match) return undefined
    const rest = match[1] || ''
    const suffix = match[2] || ''
    const editor = rest.match(/^\/info(?:\/([^/]+))?\/?$/i)
    if (editor) return (editor[1] ? screenDesignPath(decodeURIComponent(editor[1])) : SCREEN_NEW_PATH) + suffix
    const preview = rest.match(/^\/preview\/([^/]+)\/?$/i)
    if (preview) return screenViewPath(decodeURIComponent(preview[1])) + suffix
    return SCREEN_LIST_PATH + suffix
}
