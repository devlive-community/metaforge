import { OrderModel } from '@/model/order'

export interface TableFilters
{
    keyword: string
    active?: boolean
    code: string
    createdFrom: string
    createdTo: string
    conditions: Record<string, { value: string; exact: boolean }>
}

export function emptyTableFilters(): TableFilters
{
    return { keyword: '', active: undefined, code: '', createdFrom: '', createdTo: '', conditions: {} }
}

export class FilterModel
    implements TableFilters
{
    keyword = ''
    active: boolean | undefined = undefined
    code = ''
    createdFrom = ''
    createdTo = ''
    conditions: TableFilters['conditions'] = {}
    page = 1
    size = 10
    orders: Array<OrderModel> | undefined
}
