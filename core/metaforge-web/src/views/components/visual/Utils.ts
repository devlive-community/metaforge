import { Type } from '@/views/components/visual/Type.ts'
import { ChartField, ChartFieldGroup } from '@/views/components/visual/Configuration.ts'

/**
 * Generates a table header based on the given data array.
 *
 * @param {never[]} data - the input data array
 * @return {object[]} an array of objects representing the table header
 */
const createdTableHeader = (data: never[]): object[] => {
    return data.map(item => {
        return {
            'field': item,
            'title': item,
            'width': 'auto'
        }
    })
}

const createdConfigure = (type: Type, i18n: any): Array<ChartFieldGroup> => {
    const fieldGroups: Array<ChartFieldGroup> = new Array<ChartFieldGroup>()
    const categoryField: ChartField = { label: i18n.t('dataset.common.visualConfigureCategoryField'), field: 'xAxis' }
    const valueField: ChartField = { label: i18n.t('dataset.common.visualConfigureValueField'), field: 'yAxis' }
    const seriesField: ChartField = { label: i18n.t('dataset.common.visualConfigureSeriesField'), field: 'series' }
    const showLegend: ChartField = { label: i18n.t('dataset.common.visualConfigureShowLegend'), field: 'showLegend', type: 'SWITCH' }
    const outerRadius: ChartField = { label: i18n.t('dataset.common.visualConfigureOuterRadius'), field: 'outerRadius', type: 'SLIDER', value: 0.8, min: 0.1, max: 1, step: 0.1 }
    const innerRadius: ChartField = { label: i18n.t('dataset.common.visualConfigureInnerRadius'), field: 'innerRadius', type: 'SLIDER', value: 0.5, min: 0.1, max: 1, step: 0.1 }
    const startAngle: ChartField = { label: i18n.t('dataset.common.visualConfigureStartAngle'), field: 'startAngle', type: 'SLIDER', value: -180, min: -360, max: 360, step: 1 }
    const endAngle: ChartField = { label: i18n.t('dataset.common.visualConfigureEndAngle'), field: 'endAngle', type: 'SLIDER', value: 0, min: -360, max: 360, step: 1 }
    const dataBreakpoint: ChartField = {
        label: i18n.t('dataset.common.visualConfigureDataBreakpoint'),
        field: 'dataBreakpoint',
        values: [
            { label: i18n.t('dataset.common.visualConfigureDataBreakpointBreak'), value: 'break' },
            { label: i18n.t('dataset.common.visualConfigureDataBreakpointContinuous'), value: 'link' },
            { label: i18n.t('dataset.common.visualConfigureDataBreakpointZero'), value: 'zero' },
            { label: i18n.t('dataset.common.visualConfigureDataBreakpointIgnore'), value: 'ignore' }
        ]
    }
    const leftField: ChartField = { label: i18n.t('dataset.common.visualConfigureCategoryLeftField'), field: 'leftField' }
    const rightField: ChartField = { label: i18n.t('dataset.common.visualConfigureCategoryRightField'), field: 'rightField' }

    const titleVisible: ChartField = { label: i18n.t('dataset.common.visualConfigureTitleGroupVisible'), field: 'titleVisible', type: 'SWITCH', value: true }
    const titleText: ChartField = {
        label: i18n.t('dataset.common.visualConfigureTitleGroupText'),
        field: 'titleText',
        type: 'TEXT',
        disabled: { field: 'titleVisible', value: false }
    }
    const titleSubText: ChartField = {
        label: i18n.t('dataset.common.visualConfigureTitleGroupSubText'),
        field: 'titleSubText',
        type: 'TEXT',
        disabled: { field: 'titleVisible', value: false }
    }
    const titlePosition: ChartField = {
        label: i18n.t('dataset.common.visualConfigureTitleGroupPosition'), field: 'titlePosition',
        values: [
            { label: i18n.t('dataset.common.visualConfigureTitleGroupPositionLeft'), value: 'left' },
            { label: i18n.t('dataset.common.visualConfigureTitleGroupPositionTop'), value: 'top' },
            { label: i18n.t('dataset.common.visualConfigureTitleGroupPositionRight'), value: 'right' },
            { label: i18n.t('dataset.common.visualConfigureTitleGroupPositionBottom'), value: 'bottom' }
        ],
        value: 'top',
        disabled: { field: 'titleVisible', value: false }
    }
    const titleAlign: ChartField = {
        label: i18n.t('dataset.common.visualConfigureTitleGroupAlign'), field: 'titleAlign',
        values: [
            { label: i18n.t('dataset.common.visualConfigureTitleGroupAlignLeft'), value: 'left' },
            { label: i18n.t('dataset.common.visualConfigureTitleGroupAlignCenter'), value: 'center' },
            { label: i18n.t('dataset.common.visualConfigureTitleGroupAlignRight'), value: 'right' }
        ],
        value: 'left',
        disabled: { field: 'titleVisible', value: false }
    }

    const labelVisible: ChartField = { label: i18n.t('dataset.common.visualConfigureLabelGroupVisible'), field: 'labelVisible', type: 'SWITCH', value: false }

    if (type === Type.LINE) {
        const fields: Array<ChartField> = [categoryField, valueField, seriesField, dataBreakpoint]
        const generalField: ChartFieldGroup = { label: i18n.t('dataset.common.visualConfigureGeneralGroup'), fields: fields }

        const titleGroup: ChartFieldGroup = {
            label: i18n.t('dataset.common.visualConfigureTitleGroup'),
            fields: [titleVisible, titleText, titleSubText, titlePosition, titleAlign]
        }
        fieldGroups.push(generalField, titleGroup)
    }
    else if (type === Type.AREA || type === Type.BAR || type === Type.RADAR || type === Type.SCATTER) {
        const fields: Array<ChartField> = [categoryField, valueField]
        const generalField: ChartFieldGroup = { label: i18n.t('dataset.common.visualConfigureGeneralGroup'), fields: fields }
        fieldGroups.push(generalField)
    }
    else if (type === Type.FUNNEL) {
        const fields: Array<ChartField> = [categoryField, valueField, showLegend]
        const generalField: ChartFieldGroup = { label: i18n.t('dataset.common.visualConfigureGeneralGroup'), fields: fields }
        fieldGroups.push(generalField)
    }
    else if (type === Type.GAUGE) {
        const fields: Array<ChartField> = [categoryField, valueField, outerRadius, innerRadius, startAngle, endAngle]
        const generalGroup: ChartFieldGroup = { label: i18n.t('dataset.common.visualConfigureGeneralGroup'), fields: fields }
        fieldGroups.push(generalGroup)
    }
    else if (type === Type.PIE) {
        const fields: Array<ChartField> = [categoryField, valueField, outerRadius]
        const generalField: ChartFieldGroup = { label: i18n.t('dataset.common.visualConfigureGeneralGroup'), fields: fields }
        fieldGroups.push(generalField)
    }
    else if (type === Type.HISTOGRAM) {
        const fields: Array<ChartField> = [leftField, rightField, valueField]
        const generalField: ChartFieldGroup = { label: i18n.t('dataset.common.visualConfigureGeneralGroup'), fields: fields }
        fieldGroups.push(generalField)
    }
    else if (type === Type.WORDCLOUD) {
        const fields: Array<ChartField> = [categoryField, valueField, seriesField]
        const generalField: ChartFieldGroup = { label: i18n.t('dataset.common.visualConfigureGeneralGroup'), fields: fields }
        fieldGroups.push(generalField)
    }
    else if (type === Type.ROSE) {
        const fields: Array<ChartField> = [outerRadius, innerRadius, categoryField, valueField, seriesField]
        const generalField: ChartFieldGroup = { label: i18n.t('dataset.common.visualConfigureGeneralGroup'), fields: fields }

        const labelGroup: ChartFieldGroup = {
            label: i18n.t('dataset.common.visualConfigureLabelGroup'),
            fields: [labelVisible]
        }
        fieldGroups.push(generalField, labelGroup)
    }
    return fieldGroups
}

export {
    createdTableHeader,
    createdConfigure
}
