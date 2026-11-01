package org.devlive.metaforge.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.util.ArrayList;
import java.util.List;

/**
 * A table's columns as every plugin lists them through {@link PluginService#getColumns}, read by
 * the standard result names (object_name, object_data_type, ...). Services that need columns use
 * this instead of a source's own catalog tables, so they work for every source with column:read.
 */
public final class SourceColumns
{
    private SourceColumns()
    {}

    @Getter
    @AllArgsConstructor
    public static final class Column
    {
        private final String name;
        private final String type;
        private final boolean nullable;
        private final String defaultValue;
        private final String comment;
        private final boolean autoIncrement;
    }

    /** The table's columns in order; empty when the table does not exist or has none. */
    public static List<Column> of(PluginService service, RuntimeConfigure configure, String database, String table)
    {
        Response response = service.getColumns(configure, database, table);
        List<Column> columns = new ArrayList<>();
        if (response == null || !Boolean.TRUE.equals(response.getIsSuccessful()) || response.getColumns() == null) {
            return columns;
        }
        List<String> headers = response.getHeaders() == null ? List.of() : response.getHeaders();
        for (Object row : response.getColumns()) {
            String kind = value(row, headers, "type_name");
            String name = value(row, headers, "object_name");
            if (name == null || (kind != null && !"column".equalsIgnoreCase(kind))) {
                continue;
            }
            String type = value(row, headers, "object_data_type");
            columns.add(new Column(name, type == null ? "" : type, "YES".equalsIgnoreCase(value(row, headers, "object_nullable")),
                    value(row, headers, "object_default_value"), text(value(row, headers, "object_comment")),
                    "YES".equalsIgnoreCase(value(row, headers, "object_autoincrement"))));
        }
        return columns;
    }

    /** One field of a row by its result name; rows are lists in header order or JSON objects. */
    private static String value(Object row, List<String> headers, String name)
    {
        if (row instanceof JsonNode) {
            JsonNode node = ((JsonNode) row).get(name);
            if (node == null && ((JsonNode) row).isArray()) {
                int index = headers.indexOf(name);
                node = index < 0 ? null : ((JsonNode) row).get(index);
            }
            return node == null || node.isNull() ? null : node.asText();
        }
        if (row instanceof List) {
            int index = headers.indexOf(name);
            List<?> values = (List<?>) row;
            return index < 0 || index >= values.size() || values.get(index) == null ? null : String.valueOf(values.get(index));
        }
        return null;
    }

    private static String text(String value)
    {
        return value == null ? "" : value;
    }
}
