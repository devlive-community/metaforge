import useClipboard from 'vue-clipboard3'
import { join } from 'lodash'

class GridOptions
{
    /**
     * Create a default internationalization configuration
     * @param i18n I18n context
     * @private
     */
    createLocale(i18n: any)
    {
        return {
            page: i18n.t('grid.page'),
            more: i18n.t('grid.more'),
            to: i18n.t('grid.to'),
            of: i18n.t('grid.of'),
            next: i18n.t('grid.next'),
            last: i18n.t('grid.last'),
            first: i18n.t('grid.first'),
            previous: i18n.t('grid.previous'),
            loadingOoo: i18n.t('grid.loadingOoo'),
            selectAll: i18n.t('grid.selectAll'),
            searchOoo: i18n.t('grid.searchOoo'),
            blanks: i18n.t('grid.blanks'),
            filterOoo: i18n.t('grid.filterOoo'),
            applyFilter: i18n.t('grid.applyFilter'),
            equals: i18n.t('grid.equals'),
            notEqual: i18n.t('grid.notEqual'),
            lessThan: i18n.t('grid.lessThan'),
            greaterThan: i18n.t('grid.greaterThan'),
            lessThanOrEqual: i18n.t('grid.lessThanOrEqual'),
            greaterThanOrEqual: i18n.t('grid.greaterThanOrEqual'),
            inRange: i18n.t('grid.inRange'),
            contains: i18n.t('grid.contains'),
            notContains: i18n.t('grid.notContains'),
            startsWith: i18n.t('grid.startsWith'),
            endsWith: i18n.t('grid.endsWith'),
            group: i18n.t('grid.group'),
            columns: i18n.t('grid.columns'),
            rowGroupColumns: i18n.t('grid.rowGroupColumns'),
            rowGroupColumnsEmptyMessage: i18n.t('grid.rowGroupColumnsEmptyMessage'),
            valueColumns: i18n.t('grid.valueColumns'),
            pivotMode: i18n.t('grid.pivotMode'),
            groups: i18n.t('grid.groups'),
            values: i18n.t('grid.values'),
            pivots: i18n.t('grid.values'),
            valueColumnsEmptyMessage: i18n.t('grid.valueColumnsEmptyMessage'),
            pivotColumnsEmptyMessage: i18n.t('grid.pivotColumnsEmptyMessage'),
            noRowsToShow: i18n.t('grid.noRowsToShow'),
            pinColumn: i18n.t('grid.pinColumn'),
            valueAggregation: i18n.t('grid.valueAggregation'),
            autosizeThiscolumn: i18n.t('grid.autosizeThiscolumn'),
            autosizeAllColumns: i18n.t('grid.autosizeAllColumns'),
            groupBy: i18n.t('grid.groupBy'),
            ungroupBy: i18n.t('grid.ungroupBy'),
            resetColumns: i18n.t('grid.resetColumns'),
            expandAll: i18n.t('grid.expandAll'),
            collapseAll: i18n.t('grid.collapseAll'),
            toolPanel: i18n.t('grid.toolPanel'),
            export: i18n.t('grid.export'),
            csvExport: i18n.t('grid.csvExport'),
            excelExport: i18n.t('grid.excelExport'),
            pinLeft: i18n.t('grid.pinLeft'),
            pinRight: i18n.t('grid.pinRight'),
            noPin: i18n.t('grid.noPin'),
            sum: i18n.t('grid.sum'),
            min: i18n.t('grid.min'),
            max: i18n.t('grid.max'),
            none: i18n.t('grid.none'),
            count: i18n.t('grid.count'),
            average: i18n.t('grid.average'),
            copy: i18n.t('grid.copy'),
            copyWithHeaders: i18n.t('grid.copyWithHeaders'),
            ctrlC: i18n.t('grid.ctrlC'),
            paste: i18n.t('grid.paste'),
            ctrlV: i18n.t('grid.ctrlV')
        };
    }

    createDefaultOptions(i18n: any)
    {
        const gridOptions = {
            enableSorting: true,
            enableFilter: true,
            enableColResize: true,
            showToolPanel: false,
            contractColumnSelection: true,
            toolPanelSuppressRowGroups: true,
            rowDragManaged: true,
            animateRows: true,
            allowContextMenuWithControlKey: true,
            // Support copy multiple selection
            rowSelection: 'multiple',
            onSelectionChanged: (instance: any) => {
                let index = 0;
                const separator = '\t'
                const selectedRows = instance.api.getSelectedRows()
                const copyRows = [] as string[]
                selectedRows.forEach((row: any) => {
                    if (index === 0) {
                        copyRows.push(join(Object.keys(row), separator))
                        index += 1;
                    }
                    copyRows.push(join(Object.values(row), separator))
                });
                useClipboard()
                    .toClipboard(join(copyRows, '\n'))
                    .then(() => console.log('Copy'));
            },
            // Fixed issues: https://github.com/EdurtIO/metaforge/issues/219
            suppressFieldDotNotation: true,
            localeText: this.createLocale(i18n),
            defaultColDef: {
                editable: true,
                sortable: true,
                filter: true,
                resizable: true,
                wrapText: true
            }
        }
        return gridOptions;
    }

    /**
     * Build data editor configuration information
     * @param i18n I18n context
     */
    createDataEditorOptions(i18n: any)
    {
        const gridOptions = {
            animateRows: true,
            localeText: this.createLocale(i18n),
            // Fixed issues: https://github.com/EdurtIO/metaforge/issues/219
            suppressFieldDotNotation: true,
            defaultColDef: {
                sortable: true,
                resizable: true,
                wrapText: true
            }
        }
        return gridOptions;
    }
}

export default new GridOptions()
