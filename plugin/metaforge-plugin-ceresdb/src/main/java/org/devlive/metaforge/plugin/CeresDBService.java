package org.devlive.metaforge.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.util.ArrayList;
import java.util.List;

/**
 * CeresDB (HoraeDB) through the PostgreSQL JDBC driver on its PostgreSQL port. The server speaks
 * only the simple query protocol and has one schema, public, which is the plugin's database.
 */
public class CeresDBService
        implements PluginService
{
    private static final List<String> TABLE_HEADERS = List.of("type_name", "object_name", "object_comment");
    private static final List<String> COLUMN_HEADERS = List.of("type_name", "object_name", "object_data_type", "object_nullable",
            "object_default_value", "object_comment", "object_position", "object_definition");

    @Override
    public String driver()
    {
        return "org.postgresql.Driver";
    }

    @Override
    public String connectType()
    {
        return "postgresql";
    }

    /** CeresDB has no version() function. */
    @Override
    public String validator()
    {
        return "SELECT 1 AS version";
    }

    @Override
    public String url(RuntimeConfigure configure)
    {
        String url = PluginService.super.url(configure);
        if (configure.getUrl().isPresent()) {
            return url;
        }
        if (configure.getDatabase().isEmpty()) {
            url = url.replaceFirst("^(jdbc:postgresql://[^/?]+)(\\?|$)", "$1/public$2");
        }
        // The extended query protocol closes the connection.
        return url.contains("preferQueryMode=") ? url : url + (url.contains("?") ? "&" : "?") + "preferQueryMode=simple";
    }

    @Override
    public Response getDatabases(RuntimeConfigure configure)
    {
        return this.execute(configure, "SELECT 'public' AS object_name, 'DATABASE' AS object_type, '' AS object_comment");
    }

    /** Tables come from SHOW TABLES: the system tables table only answers SELECT *. */
    @Override
    public Response getTables(RuntimeConfigure configure, String database)
    {
        return remap(this.execute(configure, "SHOW TABLES"), TABLE_HEADERS, (mapper, source, position) -> {
            ObjectNode table = mapper.createObjectNode();
            table.put("type_name", "table");
            table.put("object_name", source.path("Tables").asText());
            table.put("object_comment", "");
            return table;
        });
    }

    /** Columns come from DESCRIBE, whose headers can not be renamed in SQL. */
    @Override
    public Response getColumns(RuntimeConfigure configure, String database, String table)
    {
        return remap(this.execute(configure, "DESCRIBE \"" + table.replace("\"", "\"\"") + "\""), COLUMN_HEADERS, (mapper, source, position) -> {
            ObjectNode column = mapper.createObjectNode();
            column.put("type_name", "column");
            column.put("object_name", source.path("name").asText());
            column.put("object_data_type", source.path("type").asText());
            column.put("object_nullable", source.path("is_nullable").asBoolean() ? "YES" : "NO");
            column.put("object_default_value", "");
            column.put("object_comment", "");
            column.put("object_position", position);
            column.put("object_definition", source.path("is_tag").asBoolean() ? "TAG" : source.path("is_primary").asBoolean() ? "PRIMARY KEY" : "");
            return column;
        });
    }

    private interface RowMapper
    {
        ObjectNode map(ObjectMapper mapper, JsonNode source, int position);
    }

    /** Gives a statement's JSON rows the headers MetaForge reads; other formats are returned as they are. */
    private static Response remap(Response response, List<String> headers, RowMapper mapper)
    {
        if (!Boolean.TRUE.equals(response.getIsSuccessful()) || response.getColumns().stream().anyMatch(row -> !(row instanceof JsonNode))) {
            return response;
        }
        ObjectMapper json = new ObjectMapper();
        List<Object> rows = new ArrayList<>();
        for (Object row : response.getColumns()) {
            rows.add(mapper.map(json, (JsonNode) row, rows.size() + 1));
        }
        response.setHeaders(headers);
        response.setColumns(rows);
        return response;
    }
}
