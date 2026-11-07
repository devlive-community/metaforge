/** Logos by plugin key from the catalogue's entries. The catalogue itself comes from the server. */
export function logosByKey(metadata: any): Record<string, string> {
  const logos: Record<string, string> = {}
  for (const item of metadata?.children || []) {
    if (item?.key && item?.logo) logos[String(item.key)] = String(item.logo)
  }
  return logos
}
