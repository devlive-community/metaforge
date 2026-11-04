package org.devlive.metaforge.service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.entity.ReportEntity;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.enums.ReportType;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.column.JdbcColumn;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
final class QueryChartRefresh
{
    private static final int MAX_ROWS = 10000;
    private static final int TIMEOUT_SECONDS = 15;

    private final ObjectMapper mapper;
    private final PluginManager plugins;

    QueryChartRefresh(ObjectMapper mapper, PluginManager plugins)
    {
        this.mapper = mapper;
        this.plugins = plugins;
    }

    JsonNode chart(ReportEntity report)
    {
        if (report.getType() != ReportType.QUERY || !report.isRealtime() || report.getConfigure() == null) {
            return null;
        }
        try {
            JsonNode configuration = mapper.readTree(report.getConfigure());
            if (configuration == null) {
                return null;
            }
            JsonNode chart = configuration.path("queryChart");
            return chart.isMissingNode() ? null : chart;
        }
        catch (JsonProcessingException e) {
            return null;
        }
    }

    String validationError(ReportEntity report, SourceEntity source, Long userId, JsonNode chart)
    {
        if (!source.isActive() || !Objects.equals(source.getUserId(), userId)) {
            return "Dynamic refresh requires an active data source that you own.";
        }
        if (source.isUsedConfig() || !Set.of("mysql", "postgresql").contains(engine(source))) {
            return "Dynamic refresh requires a standard MySQL or PostgreSQL connection.";
        }
        if (chart.path("version").asInt() != 1 || fields(chart).isEmpty()) {
            return "Invalid dynamic chart fields.";
        }
        if (!ReadOnlyChartSql.accepts(report.getQuery())) {
            return "Dynamic refresh requires one read-only SELECT with supported SQL functions.";
        }
        return null;
    }

    List<Map<String, Object>> execute(ReportEntity report, SourceEntity source, JsonNode chart)
            throws SQLException
    {
        var plugin = plugins.getPlugin(source.getType()).orElseThrow(() -> new SQLException("Source plugin unavailable"));
        var service = plugin.getService(PluginService.class);
        try {
            service.connect(source.toConfigure(plugins, plugin));
            var connected = PluginService.local.get();
            if (connected == null || !(connected.getConnection() instanceof Connection)) {
                throw new SQLException("Source connection unavailable");
            }
            Connection connection = (Connection) connected.getConnection();
            boolean mysql = "mysql".equals(engine(source));
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(TIMEOUT_SECONDS);
                statement.execute(mysql ? "START TRANSACTION READ ONLY" : "SET TRANSACTION READ ONLY");
                statement.setMaxRows(Math.max(1, Math.min(MAX_ROWS, chart.path("options").path("maxRows").asInt(MAX_ROWS))));
                try (ResultSet result = statement.executeQuery(report.getQuery())) {
                    return readRows(result, fields(chart));
                }
            }
            finally {
                connection.rollback();
            }
        }
        finally {
            service.destroy();
        }
    }

    private static Set<String> fields(JsonNode chart)
    {
        JsonNode nodes = chart.path("options").path("fields");
        Set<String> fields = new LinkedHashSet<>();
        if (nodes.isArray() && nodes.size() <= 1024) {
            for (JsonNode field : nodes) {
                if (!field.isTextual() || field.textValue().isBlank() || !fields.add(field.textValue())) {
                    return Set.of();
                }
            }
        }
        return fields;
    }

    private static String engine(SourceEntity source)
    {
        return Objects.toString(source.getType(), "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static List<Map<String, Object>> readRows(ResultSet result, Set<String> fields)
            throws SQLException
    {
        var metadata = result.getMetaData();
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int column = 1; column <= metadata.getColumnCount(); column++) {
            String name = metadata.getColumnLabel(column);
            if (fields.contains(name) && columns.putIfAbsent(name, column) != null) {
                throw new SQLException("Duplicate chart field");
            }
        }
        if (!columns.keySet().containsAll(fields)) {
            throw new SQLException("Missing chart field");
        }

        JdbcColumn reader = new JdbcColumn(result);
        List<Map<String, Object>> rows = new ArrayList<>();
        while (result.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String field : fields) {
                int column = columns.get(field);
                Object value = reader.mappingColumnData(metadata.getColumnTypeName(column), column);
                if (!(value == null || value instanceof CharSequence || value instanceof Number
                        || value instanceof Boolean || value instanceof java.util.Date || value instanceof byte[])) {
                    value = result.getString(column);
                }
                row.put(field, result.wasNull() ? null : value);
            }
            rows.add(row);
        }
        return rows;
    }
}
