import { inject, provide, ref, type Ref } from 'vue'

const KEY = 'metadataTreeVersion'

/**
 * The source workspace's metadata tree reloads only when its structure changes (a table or a
 * column created, changed or dropped), never because another table was opened.
 */
export function provideMetadataTreeVersion(): Ref<number>
{
    const version = ref(0)
    provide(KEY, version)
    return version
}

export function useMetadataTreeVersion(): Ref<number> | null
{
    return inject<Ref<number> | null>(KEY, null)
}

/** Call after a successful structural change; outside the workspace it does nothing. */
export function useMetadataTreeChange(): () => void
{
    const version = useMetadataTreeVersion()
    return () => { if (version) version.value++ }
}
