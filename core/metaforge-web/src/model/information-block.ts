export const informationBlockKinds = [
  'statTiles', 'contactDirectory', 'resourceCards', 'releaseTimeline', 'faqList', 'shiftRoster', 'serviceStatus', 'riskRegister',
  'linkButtonBar', 'imageBadges', 'decisionLog', 'maintenanceSchedule', 'comparisonCards', 'milestoneTracker',
  'noticeBoard', 'capacityGrid', 'escalationPaths', 'ratingCards',
  'glossaryIndex', 'statusBoard', 'incidentTimeline', 'teamHandoff', 'dependencyRegister',
  'acceptanceChecklist', 'policyCards', 'assetInventory', 'environmentCards', 'copySnippets',
] as const
export type InformationBlockKind = typeof informationBlockKinds[number]
export const informationTones = ['info', 'success', 'warning', 'danger'] as const
export type InformationTone = typeof informationTones[number]
export interface InformationItem {
  id: string
  title: string
  value: string
  secondary?: string
  detail: string
  tone: InformationTone
}
export const informationItemLimit = 30

export function isInformationBlock(kind: unknown): kind is InformationBlockKind {
  return informationBlockKinds.some(value => value === kind)
}

export function isTabularInformationBlock(kind: unknown) {
  return kind === 'riskRegister' || kind === 'shiftRoster' || kind === 'decisionLog' || kind === 'maintenanceSchedule'
    || kind === 'teamHandoff' || kind === 'dependencyRegister' || kind === 'assetInventory'
}

export function readInformationItems(value: unknown): InformationItem[] {
  if (!Array.isArray(value)) return []
  const ids = new Set<string>()
  return value.slice(0, informationItemLimit).flatMap((item, index) => {
    if (!item || typeof item !== 'object') return []
    const text = (value: unknown, max: number) => typeof value === 'string' ? value.slice(0, max) : ''
    let id = text(item.id, 100) || 'item-' + index
    while (ids.has(id)) id += '-'
    ids.add(id)
    return [{
      id, title: text(item.title, 160), value: text(item.value, 2048), secondary: text(item.secondary, 250) || undefined, detail: text(item.detail, 2000),
      tone: informationTones.find(tone => tone === item.tone) ?? 'info',
    }]
  })
}

export const informationFields = {
  statTiles: { title: 'metric', value: 'value', detail: 'context' },
  contactDirectory: { title: 'person', value: 'email', detail: 'role' },
  resourceCards: { title: 'resource', value: 'url', detail: 'description' },
  releaseTimeline: { title: 'release', value: 'dateLabel', detail: 'changes' },
  faqList: { title: 'question', detail: 'answer' },
  shiftRoster: { title: 'person', value: 'shift', detail: 'station' },
  serviceStatus: { title: 'service', detail: 'description', tone: 'status' },
  riskRegister: { title: 'risk', value: 'owner', tone: 'severity', detail: 'mitigation' },
  linkButtonBar: { title: 'linkLabel', value: 'url' },
  imageBadges: { title: 'badgeName', value: 'url', detail: 'description' },
  decisionLog: { title: 'decision', value: 'owner', tone: 'decisionStatus', detail: 'rationale' },
  maintenanceSchedule: { title: 'service', value: 'windowStart', secondary: 'windowEnd', detail: 'description' },
  comparisonCards: { title: 'metric', value: 'referenceValue', secondary: 'comparisonValue', detail: 'context' },
  milestoneTracker: { title: 'milestone', value: 'dateLabel', tone: 'milestoneStatus', detail: 'description' },
  noticeBoard: { title: 'notice', tone: 'noticeTone', detail: 'description' },
  capacityGrid: { title: 'resource', value: 'capacityUsed', secondary: 'capacityLimit', detail: 'context' },
  escalationPaths: { title: 'person', value: 'email', detail: 'role' },
  ratingCards: { title: 'ratingItem', value: 'ratingValue', detail: 'description' },
  glossaryIndex: { title: 'term', value: 'termAlias', detail: 'definition' },
  statusBoard: { title: 'workItem', value: 'owner', tone: 'workStatus', detail: 'description' },
  incidentTimeline: { title: 'incident', value: 'dateLabel', tone: 'severity', detail: 'incidentUpdate' },
  teamHandoff: { title: 'handoffItem', value: 'owner', secondary: 'recipient', tone: 'handoffStatus', detail: 'nextAction' },
  dependencyRegister: { title: 'dependency', value: 'dependent', secondary: 'owner', tone: 'dependencyStatus', detail: 'impact' },
  acceptanceChecklist: { title: 'criterion', value: 'reviewer', tone: 'checkResult', detail: 'evidence' },
  policyCards: { title: 'policyName', value: 'url', secondary: 'owner', detail: 'description' },
  assetInventory: { title: 'assetName', value: 'assetId', secondary: 'assetLocation', tone: 'assetStatus', detail: 'description' },
  environmentCards: { title: 'environment', value: 'url', secondary: 'deployedVersion', detail: 'description' },
  copySnippets: { title: 'snippetName', value: 'snippetLanguage', detail: 'snippetText' },
} satisfies Record<InformationBlockKind, Partial<Record<keyof Omit<InformationItem, 'id'>, string>>>

export function informationResourceUrl(value: string) {
  try {
    const url = new URL(value)
    if (url.protocol === 'https:' || url.protocol === 'http:') return url.href
  } catch { /* Leave the resource inactive until the address is valid. */ }
  return ''
}

export function informationMailUrl(value: string) {
  const address = value.trim()
  return /^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$/i.test(address)
    ? 'mailto:' + encodeURIComponent(address).replace('%40', '@') : ''
}
