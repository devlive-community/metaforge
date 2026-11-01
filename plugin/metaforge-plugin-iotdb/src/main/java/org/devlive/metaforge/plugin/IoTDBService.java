package org.devlive.metaforge.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * IoTDB's tree model: databases such as root.sg, devices under them as tables, named by their
 * full path, and the devices' measurements as columns.
 */
public class IoTDBService
        implements PluginService
{
    private static final List<String> DATABASE_HEADERS = List.of("object_name", "object_type", "object_comment");
    private static final List<String> TABLE_HEADERS = List.of("type_name", "object_name", "object_comment");
    private static final List<String> COLUMN_HEADERS = List.of("type_name", "object_name", "object_data_type", "object_nullable",
            "object_default_value", "object_comment", "object_position", "object_definition");
    // A path of dot-separated nodes, optionally back-quoted
    private static final Pattern PATH = Pattern.compile("[A-Za-z0-9_.`\\-]+");

    @Override
    public String validator()
    {
        // IoTDB has no version() function; SHOW VERSION starts with the version
        return "SHOW VERSION";
    }

    @Override
    public String driver()
    {
        return "org.apache.iotdb.jdbc.IoTDBDriver";
    }

    @Override
    public String connectType()
    {
        return "iotdb";
    }

    /** The driver's schema metadata fails once no database is left, so databases come from SHOW DATABASES. */
    @Override
    public Response getDatabases(RuntimeConfigure configure)
    {
        return remap(this.execute(configure, "SHOW DATABASES"), DATABASE_HEADERS, (mapper, source, position) -> {
            ObjectNode database = mapper.createObjectNode();
            database.put("object_name", source.path("Database").asText());
            database.put("object_type", "DATABASE");
            database.put("object_comment", "");
            return database;
        });
    }

    @Override
    public Response getTables(RuntimeConfigure configure, String database)
    {
        return remap(this.execute(configure, "SHOW DEVICES " + path(database) + ".**"), TABLE_HEADERS, (mapper, source, position) -> {
            ObjectNode table = mapper.createObjectNode();
            table.put("type_name", "table");
            table.put("object_name", source.path("Device").asText());
            table.put("object_comment", "");
            return table;
        });
    }

    /** Measurements of a device; the table is the device's full path, or a path relative to the database. */
    @Override
    public Response getColumns(RuntimeConfigure configure, String database, String table)
    {
        String device = table.startsWith("root.") ? table : database + "." + table;
        return remap(this.execute(configure, "SHOW TIMESERIES " + path(device) + ".*"), COLUMN_HEADERS, (mapper, source, position) -> {
            String timeseries = source.path("Timeseries").asText();
            ObjectNode column = mapper.createObjectNode();
            column.put("type_name", "column");
            column.put("object_name", timeseries.substring(timeseries.lastIndexOf('.') + 1));
            column.put("object_data_type", source.path("DataType").asText());
            column.put("object_nullable", "YES");
            column.put("object_default_value", "");
            column.put("object_comment", "");
            column.put("object_position", position);
            column.put("object_definition", source.path("Encoding").asText() + " " + source.path("Compression").asText());
            return column;
        });
    }

    private static String path(String path)
    {
        if (path == null || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid IoTDB path: " + path);
        }
        return path;
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
