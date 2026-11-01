package org.devlive.metaforge.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.util.ArrayList;
import java.util.List;

public class TDengineService
        implements PluginService
{
    private static final List<String> COLUMN_HEADERS = List.of("type_name", "object_name", "object_data_type", "object_nullable",
            "object_default_value", "object_comment", "object_position", "object_definition", "object_length");

    @Override
    public String validator()
    {
        // TDengine names its version function server_version()
        return "SELECT server_version() AS version";
    }

    @Override
    public String driver()
    {
        return "com.taosdata.jdbc.TSDBDriver";
    }

    @Override
    public String connectType()
    {
        return "TAOS-RS";
    }

    /** The REST driver answers column metadata with no result set, so columns and tags come from DESCRIBE. */
    @Override
    public Response getColumns(RuntimeConfigure configure, String database, String table)
    {
        String name = "`" + database.replace("`", "``") + "`.`" + table.replace("`", "``") + "`";
        Response response = this.execute(configure, "DESCRIBE " + name);
        if (!Boolean.TRUE.equals(response.getIsSuccessful()) || response.getColumns().stream().anyMatch(row -> !(row instanceof JsonNode))) {
            return response;
        }
        ObjectMapper json = new ObjectMapper();
        List<Object> rows = new ArrayList<>();
        for (Object row : response.getColumns()) {
            JsonNode source = (JsonNode) row;
            ObjectNode column = json.createObjectNode();
            column.put("type_name", "column");
            column.put("object_name", source.path("field").asText());
            column.put("object_data_type", source.path("type").asText());
            column.put("object_nullable", rows.isEmpty() ? "NO" : "YES");
            column.put("object_default_value", "");
            column.put("object_comment", "");
            column.put("object_position", rows.size() + 1);
            column.put("object_definition", source.path("note").asText());
            column.put("object_length", source.path("length").asInt());
            rows.add(column);
        }
        response.setHeaders(COLUMN_HEADERS);
        response.setColumns(rows);
        return response;
    }
}
