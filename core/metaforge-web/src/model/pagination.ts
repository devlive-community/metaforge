import { toNumber } from 'lodash'

export interface PaginationModel
    extends PaginationResponseModel
{
    pageSize: number
    currentPage: number
    totalRecords?: number
    startIndex?: number
    endIndex?: number
    totalPages?: number
    hasPreviousPage?: boolean
    hasNextPage?: boolean
}

export interface PaginationResponseModel
{
    size?: number
    total?: number
    page?: number
    current?: number
}

export enum PaginationEnum
{
    PREVIOUS,
    NEXT,
    FIRST,
    LAST,
    JUMP
}

export class PaginationRequest
{
    /**
     * Creates a PaginationModel from a PaginationResponseModel.
     *
     * @param {PaginationResponseModel} data - the pagination response data
     * @return {PaginationModel} the created pagination model
     */
    public static of(data: PaginationResponseModel): PaginationModel
    {
        return {
            pageSize: toNumber(data.size),
            total: data.total,
            currentPage: toNumber(data.page)
        }
    }
}
