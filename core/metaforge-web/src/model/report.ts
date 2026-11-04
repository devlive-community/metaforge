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
    shareToken?: string
    shareEnabled?: boolean
    shareEmbed?: boolean
    shareOrigins?: string
    /** Epoch milliseconds; absent when the link never expires. */
    shareExpiresAt?: number | null
    shareShowTitle?: boolean
    shareShowDescription?: boolean
    shareAllowData?: boolean
    shareAllowDownload?: boolean
}