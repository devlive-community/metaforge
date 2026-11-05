/** A setting a notify channel asks every user for, as the server declares it. */
export interface NotifyField
{
    name: string
    label: string
    type: 'TEXT' | 'SECRET' | 'URL'
    required: boolean
    description?: string
    placeholder?: string
}

export interface NotifyChannel
{
    type: string
    builtin: boolean
    fields: NotifyField[]
}

export interface NotifyCategory
{
    key: string
    scenes: string[]
}

export interface NotifyCatalog
{
    channels: NotifyChannel[]
    categories: NotifyCategory[]
}

/** One channel of the signed-in user. Secret values are never returned: `configured` lists the ones that are set. */
export interface NotifyChannelSetting
{
    type: string
    enabled: boolean
    services: string[]
    configure: Record<string, string>
    configured?: string[]
}

export const INTERNAL_CHANNEL = 'Internal'

/** Subscribes or unsubscribes one enabled channel to the given scenes. */
export const setScenes = (setting: NotifyChannelSetting, scenes: string[], checked: boolean): void => {
    const services = new Set(setting.services)
    scenes.forEach(scene => checked ? services.add(scene) : services.delete(scene))
    setting.services = Array.from(services)
}

/** How many of the scenes the channel receives: none, some or all. */
export const coverage = (setting: NotifyChannelSetting, scenes: string[]): 'none' | 'some' | 'all' => {
    const count = scenes.filter(scene => setting.services.includes(scene)).length
    return count === 0 ? 'none' : count === scenes.length ? 'all' : 'some'
}

/** A comparable snapshot of the settings, to tell whether anything changed since the last save. */
export const snapshot = (settings: NotifyChannelSetting[]): string => JSON.stringify(settings.map(setting => ({
    type: setting.type,
    enabled: setting.enabled,
    services: [...setting.services].sort(),
    configure: setting.configure
})))
