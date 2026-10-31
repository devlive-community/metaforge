/** Plugin catalogue the store reads: one entry per plugin with its key, type, label and logo. */
export const DEFAULT_METADATA_URL = 'https://cdn.north.devlive.org/applications/metaforge/metadata/metadata.json'

export const isValidUrl = (urlString: string): boolean => {
  try {
    new URL(urlString)
    return true
  }
  catch {
    return false
  }
}

/** The catalogue address chosen in the store, or the default one. */
export function storedMetadataUrl(): string {
  let saved: string | null = null
  try { saved = localStorage.getItem('metadataUrl') } catch { /* Storage may be unavailable. */ }
  return saved && isValidUrl(saved) ? saved : DEFAULT_METADATA_URL
}

/** Logos by plugin key from the catalogue's entries. */
export function logosByKey(metadata: any): Record<string, string> {
  const logos: Record<string, string> = {}
  for (const item of metadata?.children || []) {
    if (item?.key && item?.logo) logos[String(item.key)] = String(item.logo)
  }
  return logos
}
