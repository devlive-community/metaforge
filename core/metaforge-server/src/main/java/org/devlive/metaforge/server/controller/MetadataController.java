package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.SourceDdlHistoryEntity;
import org.devlive.metaforge.service.service.MetadataService;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.Response;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController()
@RequestMapping(value = "/api/v1/metadata")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class MetadataController
{
    private final MetadataService service;

    public MetadataController(MetadataService service)
    {
        this.service = service;
    }

    @GetMapping(value = "{code}/capabilities")
    public CommonResponse<ManagementCapabilities> capabilities(@PathVariable String code)
    {
        return this.service.getCapabilities(code);
    }

    @GetMapping(value = "{code}/engines")
    public CommonResponse<Response> fetchEngines(@PathVariable String code)
    {
        return this.service.getEngines(code);
    }

    @GetMapping(value = "{code}/data-types")
    public CommonResponse<Response> fetchDatatypes(@PathVariable String code)
    {
        return this.service.getDataTypes(code);
    }

    @GetMapping(value = "{code}/databases")
    public CommonResponse<Response> fetchDatabases(@PathVariable String code)
    {
        return this.service.getDatabases(code);
    }

    @GetMapping(value = "{code}/{database}/tables")
    public CommonResponse<Response> fetchTables(
            @PathVariable String code,
            @PathVariable String database
    )
    {
        return this.service.getTables(code, database);
    }

    @GetMapping(value = "{code}/{database}/{table}/columns")
    public CommonResponse<Response> fetchColumns(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table
    )
    {
        return this.service.getColumns(code, database, table);
    }

    @GetMapping(value = "{code}/{database}")
    public CommonResponse<Response> fetchDatabase(
            @PathVariable String code,
            @PathVariable String database
    )
    {
        return this.service.getDatabase(code, database);
    }

    /** Base tables of the database with engine, rows, sizes and times, for the database overview. */
    @GetMapping(value = "{code}/{database}/database-tables")
    public CommonResponse<Response> fetchDatabaseTables(@PathVariable String code, @PathVariable String database)
    {
        return this.service.getDatabaseTables(code, database);
    }

    /** Changes the database's default character set and collation; preview returns the statement only. */
    @PostMapping(value = "{code}/{database}/alter-database-options")
    public CommonResponse<Response> alterDatabaseOptions(@PathVariable String code, @PathVariable String database, @RequestBody TableDefinition configure)
    {
        return this.service.alterDatabaseOptions(code, database, configure);
    }

    @GetMapping(value = "{code}/{database}/{table}")
    public CommonResponse<Response> fetchTable(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table
    )
    {
        return this.service.getTable(code, database, table);
    }

    @GetMapping(value = "{code}/{database}/{table}/statement")
    public CommonResponse<Response> fetchTableStatement(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table
    )
    {
        return this.service.getTableStatement(code, database, table);
    }

    /** One kind of DDL for the table: create, structure, indexes, constraints, alter, alter-table or sample-data. */
    @GetMapping(value = "{code}/{database}/{table}/ddl")
    public CommonResponse<Response> fetchTableDdl(@PathVariable String code, @PathVariable String database, @PathVariable String table,
            @RequestParam(value = "kind", defaultValue = "create") String kind)
    {
        return this.service.getTableDdl(code, database, table, kind);
    }

    /** Generates one kind of DDL on request from the DDL tab and records it in the table's DDL history. */
    @PostMapping(value = "{code}/{database}/{table}/ddl")
    public CommonResponse<Response> generateTableDdl(@PathVariable String code, @PathVariable String database, @PathVariable String table,
            @RequestParam(value = "kind", defaultValue = "create") String kind)
    {
        return this.service.generateTableDdl(code, database, table, kind);
    }

    /** DDL generated for the table, newest first. */
    @GetMapping(value = "{code}/{database}/{table}/ddl-history")
    public CommonResponse<PageEntity<SourceDdlHistoryEntity>> fetchTableDdlHistory(@PathVariable String code, @PathVariable String database,
            @PathVariable String table, @RequestParam(value = "page", defaultValue = "1") int page, @RequestParam(value = "size", defaultValue = "10") int size)
    {
        return this.service.getTableDdlHistory(code, database, table, page, size);
    }

    @PutMapping(value = "{code}/{database}/{table}/auto-increment")
    public CommonResponse<Response> updateAutoIncrement(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.updateAutoIncrement(code, configure, database, table);
    }

    @PostMapping(value = "{code}/{database}/create-table")
    public CommonResponse<Response> createTable(
            @PathVariable String code,
            @PathVariable String database,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.createTable(code, database, configure);
    }

    @DeleteMapping(value = "{code}/{database}/{table}/drop-table")
    public CommonResponse<Response> dropTable(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.dropTable(code, database, table, configure);
    }

    @DeleteMapping(value = "{code}/{database}/{table}/truncate-table")
    public CommonResponse<Response> truncateTable(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.truncateTable(code, database, table, configure);
    }

    @PostMapping(value = "{code}/{database}/{table}/query-table")
    public CommonResponse<Response> queryTable(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.queryTable(code, database, table, configure);
    }

    @DeleteMapping(value = "{code}/{database}/{table}/delete-data")
    public CommonResponse<Response> deleteData(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.deleteData(code, database, table, configure);
    }

    @PostMapping(value = "{code}/{database}/{table}/export-data")
    public CommonResponse<Response> exportData(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.exportData(code, database, table, configure);
    }

    @PostMapping(value = "{code}/{database}/{table}/create-column")
    public CommonResponse<Response> createColumn(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.createColumn(code, database, table, configure);
    }

    /** Builds the CREATE TABLE statement for an unsaved definition; nothing is executed or recorded. */
    @PostMapping(value = "{code}/preview-create-table")
    public CommonResponse<Response> previewCreateTable(@PathVariable String code,
            @RequestParam(value = "kind", defaultValue = "full") String kind,
            @RequestBody TableDefinition configure)
    {
        return this.service.previewCreateTable(code, configure, kind);
    }

    /** All SQL tabs for one unsaved definition, without executing or recording DDL. */
    @PostMapping(value = "{code}/preview-create-table-batch")
    public CommonResponse<Map<String, String>> previewCreateTableBatch(@PathVariable String code, @RequestBody TableDefinition configure)
    {
        return this.service.previewCreateTableBatch(code, configure);
    }

    @GetMapping(value = "{code}/{database}/{table}/indexes")
    public CommonResponse<Response> fetchIndexes(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table
    )
    {
        return this.service.getIndexes(code, database, table);
    }

    @GetMapping(value = "{code}/{database}/{table}/foreign-keys")
    public CommonResponse<Response> fetchForeignKeys(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table
    )
    {
        return this.service.getForeignKeys(code, database, table);
    }

    @PostMapping(value = "{code}/{database}/{table}/create-index")
    public CommonResponse<Response> createIndex(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.createIndex(code, database, table, configure);
    }

    @DeleteMapping(value = "{code}/{database}/{table}/drop-index")
    public CommonResponse<Response> dropIndex(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.dropIndex(code, database, table, configure);
    }

    @DeleteMapping(value = "{code}/{database}/{table}/drop-column")
    public CommonResponse<Response> dropColumn(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.dropColumn(code, database, table, configure);
    }

    @GetMapping(value = "{code}/{database}/{table}/table-properties")
    public CommonResponse<Response> fetchTableProperties(@PathVariable String code, @PathVariable String database, @PathVariable String table)
    {
        return this.service.getTableProperties(code, database, table);
    }

    @GetMapping(value = "{code}/character-sets")
    public CommonResponse<Response> fetchCharacterSets(@PathVariable String code)
    {
        return this.service.getCharacterSets(code);
    }

    @PostMapping(value = "{code}/{database}/{table}/alter-table-options")
    public CommonResponse<Response> alterTableOptions(@PathVariable String code, @PathVariable String database, @PathVariable String table,
            @RequestBody TableDefinition configure)
    {
        return this.service.alterTableOptions(code, database, table, configure);
    }

    @GetMapping(value = "{code}/{database}/{table}/trigger-details")
    public CommonResponse<Response> fetchTriggerDetails(@PathVariable String code, @PathVariable String database, @PathVariable String table)
    {
        return this.service.getTriggerDetails(code, database, table);
    }

    @PostMapping(value = "{code}/{database}/{table}/create-trigger")
    public CommonResponse<Response> createTrigger(@PathVariable String code, @PathVariable String database, @PathVariable String table,
            @RequestBody TableDefinition configure)
    {
        return this.service.createTrigger(code, database, table, configure);
    }

    @PostMapping(value = "{code}/{database}/{table}/alter-trigger")
    public CommonResponse<Response> alterTrigger(@PathVariable String code, @PathVariable String database, @PathVariable String table,
            @RequestBody TableDefinition configure)
    {
        return this.service.alterTrigger(code, database, table, configure);
    }

    @DeleteMapping(value = "{code}/{database}/{table}/drop-trigger")
    public CommonResponse<Response> dropTrigger(@PathVariable String code, @PathVariable String database, @PathVariable String table,
            @RequestBody TableDefinition configure)
    {
        return this.service.dropTrigger(code, database, table, configure);
    }

    @GetMapping(value = "{code}/{database}/{table}/foreign-key-details")
    public CommonResponse<Response> fetchForeignKeyDetails(@PathVariable String code, @PathVariable String database, @PathVariable String table)
    {
        return this.service.getForeignKeyDetails(code, database, table);
    }

    @PostMapping(value = "{code}/{database}/{table}/create-foreign-key")
    public CommonResponse<Response> createForeignKey(@PathVariable String code, @PathVariable String database, @PathVariable String table,
            @RequestBody TableDefinition configure)
    {
        return this.service.createForeignKey(code, database, table, configure);
    }

    @PostMapping(value = "{code}/{database}/{table}/alter-foreign-key")
    public CommonResponse<Response> alterForeignKey(@PathVariable String code, @PathVariable String database, @PathVariable String table,
            @RequestBody TableDefinition configure)
    {
        return this.service.alterForeignKey(code, database, table, configure);
    }

    @DeleteMapping(value = "{code}/{database}/{table}/drop-foreign-key")
    public CommonResponse<Response> dropForeignKey(@PathVariable String code, @PathVariable String database, @PathVariable String table,
            @RequestBody TableDefinition configure)
    {
        return this.service.dropForeignKey(code, database, table, configure);
    }

    @GetMapping(value = "{code}/{database}/{table}/index-details")
    public CommonResponse<Response> fetchIndexDetails(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table
    )
    {
        return this.service.getIndexDetails(code, database, table);
    }

    @PostMapping(value = "{code}/{database}/{table}/alter-index")
    public CommonResponse<Response> alterIndex(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.alterIndex(code, database, table, configure);
    }

    @PostMapping(value = "{code}/{database}/{table}/column-dependencies")
    public CommonResponse<Response> fetchColumnDependencies(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.getColumnDependencies(code, database, table, configure);
    }

    /** Foreign keys, views, routines and triggers that depend on the table, checked before dropping it. */
    @GetMapping(value = "{code}/{database}/{table}/table-dependencies")
    public CommonResponse<Response> fetchTableDependencies(@PathVariable String code, @PathVariable String database, @PathVariable String table)
    {
        return this.service.getTableDependencies(code, database, table);
    }

    @PostMapping(value = "{code}/{database}/{table}/get-column")
    public CommonResponse<Response> getColumn(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.getColumn(code, database, table, configure);
    }

    @PutMapping(value = "{code}/{database}/{table}/change-column")
    public CommonResponse<Response> changeColumn(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.changeColumn(code, database, table, configure);
    }

    @PostMapping(value = "{code}/{database}/{table}/insert-data")
    public CommonResponse<Response> insertData(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.insertData(code, database, table, configure);
    }

    @PutMapping(value = "{code}/{database}/{table}/update-data")
    public CommonResponse<Response> updateData(
            @PathVariable String code,
            @PathVariable String database,
            @PathVariable String table,
            @RequestBody TableDefinition configure
    )
    {
        return this.service.updateData(code, database, table, configure);
    }

    @GetMapping(value = "{code}/suggests")
    public CommonResponse<Response> getSuggests(
            @PathVariable String code,
            @RequestParam String keyword
    )
    {
        return this.service.getSuggests(code, keyword);
    }
}
