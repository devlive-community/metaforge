import { BaseService } from '@/services/base'
import { ResponseModel } from '@/model/response'
import type { NativeRequest } from '@/model/source-native'
import { HttpUtils } from '@/utils/http'

const DEFAULT_PATH = '/api/v1/metadata'

class MetadataService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }

    nativeNamespaces(code: string, request: NativeRequest): Promise<ResponseModel>
    {
        return new HttpUtils().post(`/api/v1/source-management/${ encodeURIComponent(code) }/namespaces`, request)
    }

    nativeBrowse(code: string, request: NativeRequest): Promise<ResponseModel>
    {
        return new HttpUtils().post(`/api/v1/source-management/${ encodeURIComponent(code) }/browse`, request)
    }

    nativeDetail(code: string, request: NativeRequest): Promise<ResponseModel>
    {
        return new HttpUtils().post(`/api/v1/source-management/${ encodeURIComponent(code) }/detail`, request)
    }

    nativeDownload(code: string, request: NativeRequest): Promise<ResponseModel>
    {
        return new HttpUtils().post(`/api/v1/source-management/${ encodeURIComponent(code) }/download`, request)
    }

    getCapabilities(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`/api/v1/source-management/${ encodeURIComponent(code) }/capabilities`)
    }

    getEngines(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/engines`)
    }

    getDataTypes(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/data-types`)
    }

    getDatabaseBySource(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/databases`)
    }

    /** Base tables of the database with engine, rows, data and index bytes and times. */
    getDatabaseTables(code: string, database: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/database-tables`)
    }

    /** Changes the database's default character set and collation; `preview` returns the statement only. */
    alterDatabaseOptions(code: string, database: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/alter-database-options`, configure)
    }

    getTablesByDatabase(code: string, database: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/tables`)
    }

    getColumnsByTable(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/columns`)
    }

    getDatabase(code: string, database: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }`)
    }

    getTable(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }`)
    }

    getTableStatement(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/statement`)
    }

    /** One kind of DDL built from the table definition: create, structure, indexes, constraints, alter, alter-table or sample-data. */
    getTableDdl(code: string, database: string, table: string, kind: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/ddl`, { kind })
    }

    /** Generates the DDL like getTableDdl and records it in the table's DDL history. */
    generateTableDdl(code: string, database: string, table: string, kind: string): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/ddl?kind=${ encodeURIComponent(kind) }`)
    }

    /** DDL generated for the table, newest first. */
    getTableDdlHistory(code: string, database: string, table: string, page: number, size: number): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/ddl-history`, { page, size })
    }

    updateAutoIncrement(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/auto-increment`, configure)
    }

    createTable(code: string, database: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/create-table`, configure)
    }

    dropTable(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/drop-table`, configure)
    }

    truncateTable(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/truncate-table`, configure)
    }

    queryTable(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/query-table`, configure)
    }

    /** Indexes, foreign keys, triggers, views and generated columns that use the given columns. */
    getColumnDependencies(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/column-dependencies`, configure)
    }

    /** Foreign keys of other tables, views, routines and triggers that depend on the table. */
    getTableDependencies(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/table-dependencies`)
    }

    deleteData(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/delete-data`, configure)
    }

    updateData(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/update-data`, configure)
    }

    exportData(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/export-data`, configure)
    }

    createColumn(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/create-column`, configure)
    }

    dropColumn(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/drop-column`, configure)
    }

    getIndexes(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/indexes`)
    }

    getForeignKeys(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/foreign-keys`)
    }

    /** One row per index column, with order, prefix, cardinality, type, comment, key length and usage. */
    getIndexDetails(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/index-details`)
    }

    /** Options and statistics of one table: engine, row format, charset, collation, sizes, counts and comment. */
    getTableProperties(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/table-properties`)
    }

    /** Every character set with its collations. */
    getCharacterSets(code: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/character-sets`)
    }

    /** Changes the given table options in one ALTER TABLE. */
    alterTableOptions(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/alter-table-options`, configure)
    }

    /** Triggers of the table with timing, event, body, definer, creation time, SQL mode and charset. */
    getTriggerDetails(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/trigger-details`)
    }

    createTrigger(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/create-trigger`, configure)
    }

    /** Changes or rebuilds one trigger: dropped and created again, restored when the new one fails. */
    alterTrigger(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/alter-trigger`, configure)
    }

    dropTrigger(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/drop-trigger`, configure)
    }

    /** One row per foreign key column of the table, with its reference, rules and whether the referenced table exists. */
    getForeignKeyDetails(code: string, database: string, table: string): Promise<ResponseModel>
    {
        return new HttpUtils().get(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/foreign-key-details`)
    }

    createForeignKey(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/create-foreign-key`, configure)
    }

    /** Changes or rebuilds foreign keys: each one is dropped, then added with the new definition. */
    alterForeignKey(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/alter-foreign-key`, configure)
    }

    dropForeignKey(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/drop-foreign-key`, configure)
    }

    /** Changes or rebuilds indexes: each one is dropped and added again in one statement. */
    alterIndex(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/alter-index`, configure)
    }

    createIndex(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/create-index`, configure)
    }

    dropIndex(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().delete(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/drop-index`, configure)
    }

    previewCreateTable(code: string, configure: any, kind: string): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/preview-create-table?kind=${ kind }`, configure)
    }

    previewCreateTableBatch(code: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/preview-create-table-batch`, configure)
    }

    getColumn(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/get-column`, configure)
    }

    changeColumn(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().put(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/change-column`, configure)
    }

    insertData(code: string, database: string, table: string, configure: any): Promise<ResponseModel>
    {
        return new HttpUtils().post(`${ DEFAULT_PATH }/${ code }/${ database }/${ table }/insert-data`, configure)
    }
}

export default new MetadataService()