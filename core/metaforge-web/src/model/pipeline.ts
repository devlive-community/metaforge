import { BaseModel } from '@/model/base.ts'
import { SourceModel } from '@/model/source.ts'

export interface PipelineModel
    extends BaseModel
{
    content?: string
    state?: string
    message?: any
    work?: string
    startTime?: string
    endTime?: string
    elapsed?: number
    executor?: string
    from?: SourceModel
    fromConfigures?: Map<string, string>
    to?: SourceModel
    toConfigures?: Map<string, string>
    flowConfigure?: any
}