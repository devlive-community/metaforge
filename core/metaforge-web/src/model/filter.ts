import { OrderModel } from '@/model/order'

export class FilterModel
{
    page = 1
    size = 10
    orders: Array<OrderModel> | undefined
}
