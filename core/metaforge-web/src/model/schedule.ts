export interface ScheduleModel
{
    id: number
    name: string
    description: string
    expression: string
    active?: boolean
    isSystem?: boolean
    type?: string
    createTime?: string
    updateTime?: string
}