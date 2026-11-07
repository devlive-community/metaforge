import { ref } from 'vue'
import PluginService from '@/services/plugin'
import { logosByKey } from '@/model/store-metadata'

let cache: Promise<Record<string, string>> | null = null

/**
 * Plugin logos by plugin key, read from the same catalogue the store shows. Loaded once per page;
 * when the catalogue cannot be reached the map stays empty and callers fall back to an icon.
 */
export function usePluginLogos() {
  const logos = ref<Record<string, string>>({})
  cache ||= PluginService.getCatalogue()
      .then(response => response.status && response.data ? response.data : null)
      .then(logosByKey)
      .catch(() => { cache = null; return {} })
  cache.then(value => { logos.value = value })
  return { logos }
}
