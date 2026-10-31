package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.SourceDdlHistoryEntity;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.ManagementDetail;
import org.devlive.metaforge.spi.model.ManagementDownload;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.devlive.metaforge.spi.model.ManagementRequest;
import org.devlive.metaforge.spi.model.Response;

import java.util.Map;

public interface MetadataService
{
    CommonResponse<ManagementCapabilities> getCapabilities(String code);

    CommonResponse<ManagementPage> managementNamespaces(String code, ManagementRequest request);

    CommonResponse<ManagementPage> managementList(String code, ManagementRequest request);

    CommonResponse<ManagementDetail> managementDetail(String code, ManagementRequest request);

    CommonResponse<ManagementDownload> managementDownload(String code, ManagementRequest request);

    CommonResponse<Response> getEngines(String code);

    CommonResponse<Response> getDataTypes(String code);

    CommonResponse<Response> getDatabases(String code);

    CommonResponse<Response> getTables(String code, String database);

    CommonResponse<Response> getColumns(String code, String database, String table);

    CommonResponse<Response> getDatabase(String code, String database);

    CommonResponse<Response> getDatabaseTables(String code, String database);

    CommonResponse<Response> alterDatabaseOptions(String code, String database, TableDefinition configure);

    CommonResponse<Response> getTable(String code, String database, String table);

    CommonResponse<Response> getTableStatement(String code, String database, String table);

    CommonResponse<Response> getTableDdl(String code, String database, String table, String kind);

    /** Generates the DDL like {@link #getTableDdl} and keeps it in the table's DDL history. */
    CommonResponse<Response> generateTableDdl(String code, String database, String table, String kind);

    CommonResponse<PageEntity<SourceDdlHistoryEntity>> getTableDdlHistory(String code, String database, String table, int page, int size);

    CommonResponse<Response> updateAutoIncrement(String code, TableDefinition configure, String database, String table);

    CommonResponse<Response> createTable(String code, String database, TableDefinition configure);

    CommonResponse<Response> dropTable(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> truncateTable(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> queryTable(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> deleteData(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> exportData(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> createColumn(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> createIndex(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> previewCreateTable(String code, TableDefinition configure, String kind);

    CommonResponse<Map<String, String>> previewCreateTableBatch(String code, TableDefinition configure);

    CommonResponse<Response> dropIndex(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> getIndexes(String code, String database, String table);

    CommonResponse<Response> getForeignKeys(String code, String database, String table);

    CommonResponse<Response> dropColumn(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> getColumnDependencies(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> getTableDependencies(String code, String database, String table);

    CommonResponse<Response> getIndexDetails(String code, String database, String table);

    CommonResponse<Response> alterIndex(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> getForeignKeyDetails(String code, String database, String table);

    CommonResponse<Response> createForeignKey(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> alterForeignKey(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> dropForeignKey(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> getTriggerDetails(String code, String database, String table);

    CommonResponse<Response> createTrigger(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> alterTrigger(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> dropTrigger(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> getTableProperties(String code, String database, String table);

    CommonResponse<Response> getCharacterSets(String code);

    CommonResponse<Response> alterTableOptions(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> getColumn(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> changeColumn(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> insertData(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> updateData(String code, String database, String table, TableDefinition configure);

    CommonResponse<Response> getSuggests(String code, String keyword);
}
