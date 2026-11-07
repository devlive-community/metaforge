import { permits, type DashboardModel } from './dashboard'
import { readDashboardDocument } from './dashboard-layout'
import { screenDesignPath, screenViewPath } from './screen-route'

export function screenEntry(item: DashboardModel) {
  const document = readDashboardDocument(item.configure, item.name, item.description)
  const published = !!document.published, editable = permits(item.permission, 'EDIT')
  return { ...item, published, editable, own: !item.permission || item.permission === 'OWNER',
    mode: (document.published ?? document.draft).mode, editor: screenDesignPath(item.code),
    link: published || !editable ? screenViewPath(item.code || '') : screenDesignPath(item.code) }
}
export type ScreenEntry = ReturnType<typeof screenEntry>
