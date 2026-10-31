import { inject, onScopeDispose, type Ref } from 'vue'

/** Loading flags of the panes currently shown in the source workspace; see useMetadataWorkspace. */
export type WorkspaceLoaders = Set<Readonly<Ref<boolean>>>

/**
 * Registers a pane's loading flag with the source workspace, so the header's refresh button
 * shows a loading state until the pane's own data is back. Outside the workspace it does nothing.
 */
export function useWorkspaceLoading(loading: Readonly<Ref<boolean>>) {
  const loaders = inject<WorkspaceLoaders | null>('metadataPaneLoaders', null)
  if (!loaders) return
  loaders.add(loading)
  onScopeDispose(() => { loaders.delete(loading) })
}
