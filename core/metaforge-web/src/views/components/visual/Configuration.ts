import { Type } from '@/views/components/visual/Type'
import { ResponseModel } from '@/model/response'

export class Configuration
{
    title?: null
    headers: never[] = []
    columns: never[] = []
    type: Type = Type.TABLE
    message?: null
    chartConfigure?: IChart
}

export class ConfigurationRequest
{
    public static of(response: ResponseModel): Configuration
    {
        const configuration = new Configuration()
        configuration.headers = response.data.headers
        configuration.columns = response.data.columns
        return configuration
    }
}

export interface IChart
{
    xAxis?: string
    x2Axis?: string
    yAxis?: string
    series?: string
    outerRadius?: number[]
    innerRadius?: number[]
    invalidType?: string
    showLegend?: boolean
    startAngle?: number[]
    endAngle?: number[]
    titleVisible?: boolean
    titleText?: string
    titleSubText?: string
    titlePosition?: string
    titleAlign?: string
    labelVisible?: boolean
}

export interface ChartFieldGroup
{
    label?: string
    fields?: ChartField[]
}

export interface ChartField
{
    label?: string
    field?: string
    type?: string
    values?: any[]
    value?: any
    min?: number
    max?: number
    step?: number
    disabled?: ChartFieldItem
}

export interface ChartFieldItem
{
    field?: string
    value?: any
}

