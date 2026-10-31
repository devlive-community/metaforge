import { ref } from 'vue'
import { logosByKey, storedMetadataUrl } from '@/model/store-metadata'

let cache: Promise<Record<string, string>> | null = null

/**
 * Plugin logos by plugin key, read from the same catalogue the store shows. Loaded once per page;
 * when the catalogue cannot be reached the map stays empty and callers fall back to an icon.
 */
export function usePluginLogos() {
  const logos = ref<Record<string, string>>({})
  cache ||= fetch(storedMetadataUrl())
      .then(response => response.ok ? response.json() : null)
      .then(logosByKey)
      .catch(() => { cache = null; return {} })
  cache.then(value => { logos.value = value })
  return { logos }
}
