import GridOptions from '@/views/components/grid/GridOptions'

// MfTable 列格式（bodyCell 契约）；object_nullable 走 #bodyCell
const createHeaders = (i18n: any) => {
    return [
        { title: i18n.t('common.name'), dataIndex: 'object_name', key: 'object_name' },
        { title: i18n.t('common.type'), dataIndex: 'object_data_type', key: 'object_data_type' },
        { title: i18n.t('common.position'), dataIndex: 'object_position', key: 'object_position' },
        { title: i18n.t('source.common.isNullable'), dataIndex: 'object_nullable', key: 'object_nullable' },
        { title: i18n.t('source.common.defaultValue'), dataIndex: 'object_default_value', key: 'object_default_value' },
        { title: i18n.t('source.common.comment'), dataIndex: 'object_comment', key: 'object_comment' },
        { title: i18n.t('source.common.extra'), dataIndex: 'object_definition', key: 'object_definition' }
    ]
}

/**
 * Create column definitions based on headers and types.
 *
 * @param {any[]} headers - The array of headers.
 * @param {any[]} types - The array of types.
 * @return {any[]} The array of column definitions.
 */
const createColumnDefs = (headers: any[], types: any[]): any[] => {
    const columnDefs = [] as any[]
    headers.forEach((header, index) => {
        const columnDef = {
            headerName: header,
            field: header,
            headerTooltip: header + ' [' + types[index] + ']',
            unSortIcon: true,
            cellRenderer: (params: { value: string }) => {
                return '<div style="white-space: pre-wrap;">' + params.value + '</div>'
            },
            cellEditorPopup: true,
            cellEditor: 'agLargeTextCellEditor',
            cellEditorParams: { maxLength: 9999999999999, rows: 10 },
            checked: true
        }
        columnDefs.push(columnDef)
    })
    return columnDefs
}

/**
 * Creates data editor options.
 *
 * @param {any} i18n - The i18n object.
 * @return {Object} The grid options.
 */
const createDataEditorOptions = (i18n: any): object => {
    const gridOptions = {
        animateRows: true,
        localeText: GridOptions.createLocale(i18n),
        // Fixed issues: https://github.com/EdurtIO/metaforge/issues/219
        suppressFieldDotNotation: true,
        // Turn on multi-column sorting
        alwaysMultiSort: true,
        defaultColDef: {
            sortable: true,
            resizable: true,
            wrapText: true,
            editable: true,
            // Prevents the front-end from reordering data
            comparator: () => 0
        }
    }
    return gridOptions
}

export {
    createHeaders,
    createColumnDefs,
    createDataEditorOptions
}
