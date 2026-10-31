import { computed, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSourceCapabilitiesStore } from '@/stores/source-capabilities'
export function useSourceCapabilities(source: Ref<string>) {
  const store = useSourceCapabilitiesStore()
  const { t } = useI18n()
  const profile = computed(() => store.entries[source.value]?.profile || null)
  const capabilities = computed(() => new Set(profile.value?.capabilities || []))
  const can = (capability: string) => capabilities.value.has(capability)
  const capabilityLoading = computed(() => !!source.value && (store.entries[source.value]?.loading ?? true))
  const capabilityError = computed(() => store.entries[source.value]?.contractError ? t('source.manage.capabilityContractError') : store.entries[source.value]?.error || '')
  const ensureCapabilities = (force = false) => store.load(source.value, force)
  watch(source, code => { store.load(code) }, { immediate: true })
  return { profile, can, capabilityLoading, capabilityError, ensureCapabilities }
}
