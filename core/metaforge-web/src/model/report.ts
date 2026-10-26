import { BaseModel } from '@/model/base'
import { DatasetModel } from '@/model/dataset'
import { SourceModel } from '@/model/source.ts'

export interface ReportModel
    extends BaseModel
{
    configure?: string
    realtime?: boolean
    type?: string
    query?: string
    description?: string
    dataset?: DatasetModel
    source?: SourceModel
}