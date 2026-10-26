package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.service.MetadataService;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
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
