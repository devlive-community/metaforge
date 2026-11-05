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

/** Subscribes or unsubscribes a channel's scene list to the given scenes. */
export const setScenes = (target: { services: string[] }, scenes: string[], checked: boolean): void => {
    const services = new Set(target.services)
    scenes.forEach(scene => checked ? services.add(scene) : services.delete(scene))
    target.services = Array.from(services)
}

/** How many of the scenes the channel receives: none, some or all. */
export const coverage = (target: { services: string[] }, scenes: string[]): 'none' | 'some' | 'all' => {
    const count = scenes.filter(scene => target.services.includes(scene)).length
    return count === 0 ? 'none' : count === scenes.length ? 'all' : 'some'
}

/** Whether two scene lists hold the same scenes, in any order. */
export const sameScenes = (left: string[], right: string[]): boolean =>
    left.length === right.length && left.every(scene => right.includes(scene))
