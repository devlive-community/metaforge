export interface RouterItem
{
    url: string
    i18nKey: string
    children: Array<RouterItem>
    view: string
}