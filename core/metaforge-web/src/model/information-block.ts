export const informationBlockKinds = [
  'statTiles', 'contactDirectory', 'resourceCards', 'releaseTimeline', 'faqList', 'shiftRoster', 'serviceStatus', 'riskRegister',
] as const
export type InformationBlockKind = typeof informationBlockKinds[number]
export const informationTones = ['info', 'success', 'warning', 'danger'] as const
export type InformationTone = typeof informationTones[number]
export interface InformationItem {
  id: string
  title: string
  value: string
  detail: string
  tone: InformationTone
}
export const informationItemLimit = 30

export function isInformationBlock(kind: unknown): kind is InformationBlockKind {
  return informationBlockKinds.some(value => value === kind)
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
      id, title: text(item.title, 160), value: text(item.value, 2048), detail: text(item.detail, 2000),
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
} satisfies Record<InformationBlockKind, Partial<Record<keyof Omit<InformationItem, 'id'>, string>>>
