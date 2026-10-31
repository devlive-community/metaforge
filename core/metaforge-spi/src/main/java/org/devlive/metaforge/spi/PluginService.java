package org.devlive.metaforge.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.plugin.Service;
import org.devlive.metaforge.spi.adapter.Adapter;
import org.devlive.metaforge.spi.adapter.BatchWriter;
import org.devlive.metaforge.spi.adapter.HttpAdapter;
import org.devlive.metaforge.spi.adapter.JdbcAdapter;
import org.devlive.metaforge.spi.adapter.JdbcStreamAdapter;
import org.devlive.metaforge.spi.adapter.NativeAdapter;
import org.devlive.metaforge.spi.adapter.RowCallback;
import org.devlive.metaforge.spi.connection.Connection;
import org.devlive.metaforge.spi.connection.JdbcConnection;
import org.devlive.metaforge.spi.connection.JdbcUrlGuard;
import org.devlive.metaforge.spi.generator.DataType;
import org.devlive.metaforge.spi.generator.Filter;
import org.devlive.metaforge.spi.generator.Index;
import org.devlive.metaforge.spi.generator.OrderBy;
import org.devlive.metaforge.spi.generator.column.CreateColumn;
import org.devlive.metaforge.spi.generator.column.SelectColumn;
import org.devlive.metaforge.spi.generator.definition.BaseDefinition;
import org.devlive.metaforge.spi.generator.definition.ColumnDefinition;
import org.devlive.metaforge.spi.generator.definition.ForeignKeyDefinition;
import org.devlive.metaforge.spi.generator.definition.IndexDefinition;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.generator.definition.TableOptionsDefinition;
import org.devlive.metaforge.spi.generator.definition.TriggerDefinition;
import org.devlive.metaforge.spi.generator.table.AbstractTable;
import org.devlive.metaforge.spi.generator.table.AlterTable;
import org.devlive.metaforge.spi.generator.table.CreateTable;
import org.devlive.metaforge.spi.generator.table.DeleteTable;
import org.devlive.metaforge.spi.generator.table.DropTable;
import org.devlive.metaforge.spi.generator.table.InsertTable;
import org.devlive.metaforge.spi.generator.table.SelectTable;
import org.devlive.metaforge.spi.generator.table.TruncateTable;
import org.devlive.metaforge.spi.generator.table.UpdateTable;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.ManagementDetail;
import org.devlive.metaforge.spi.model.ManagementDownload;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.devlive.metaforge.spi.model.ManagementRequest;
import org.devlive.metaforge.spi.model.Pagination;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.devlive.metaforge.spi.sql.TableDdl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface PluginService
        extends Service
{
    ThreadLocal<Connection> local = new ThreadLocal<>();
    Logger log = LoggerFactory.getLogger(PluginService.class);

    default ManagementCapabilities managementCapabilities()
    {
        return ManagementCapabilities.forJdbc(connectType());
    }

    default ManagementPage managementNamespaces(RuntimeConfigure configure, ManagementRequest request)
    {
        throw new UnsupportedOperationException("Namespace browsing is not supported");
    }

    default ManagementPage managementList(RuntimeConfigure configure, ManagementRequest request)
    {
        throw new UnsupportedOperationException("Native browsing is not supported");
    }

    default ManagementDetail managementDetail(RuntimeConfigure configure, ManagementRequest request)
    {
        throw new UnsupportedOperationException("Native details are not supported");
    }

    default ManagementDownload managementDownload(RuntimeConfigure configure, ManagementRequest request)
    {
        throw new UnsupportedOperationException("Native downloads are not supported");
    }

    default String validator()
    {
        return "SELECT version() AS version";
    }

    default PluginType type()
    {
        return PluginType.JDBC;
    }

    default String name()
    {
        return this.getClass().getSimpleName().replace("Plugin", "");
    }

    default String description()
    {
        return String.format("Integrate %s data sources", this.name());
    }

    default String driver()
    {
        return "org.devlive.metaforge.JdbcDriver";
    }

    default String connectType()
    {
        return "metaforge";
    }

    /**
     * 是否支持元数据管理
     *
     * @return 是否支持
     */
    default Boolean isSupportMeta()
    {
        return false;
    }

    default void connect(RuntimeConfigure configure)
    {
        Response response = new Response();
        try {
            configure.setDriver(this.driver());
            configure.setType(this.connectType());
            configure.setUrl(Optional.of(url(configure)));
            local.set(new JdbcConnection(configure, response));
        }
        catch (Exception ex) {
            response.setIsConnected(Boolean.FALSE);
            response.setMessage(ex.getMessage());
            log.error("Error connecting :", ex);
        }
    }

    /**
     * 构建驱动路径
     * Build the driver path
     *
     * @param configure 配置信息 | Configuration information
     * @return 驱动路径 | Driver path
     */
    default String url(RuntimeConfigure configure)
    {
        if (configure.getUrl().isPresent()) {
            return configure.getUrl().get();
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append("jdbc:");
        buffer.append(configure.getType());
        buffer.append("://");
        buffer.append(JdbcUrlGuard.validateHost(configure.getHost()));
        buffer.append(":");
        buffer.append(configure.getPort());
        if (configure.getDatabase().isPresent()) {
            JdbcUrlGuard.validateDatabase(configure.getDatabase().get());
            buffer.append("/");
            buffer.append(configure.getDatabase().get());
        }
        if (configure.getSsl().isPresent()) {
            buffer.append(String.format("?ssl=%s", configure.getSsl().get()));
        }
        if (configure.getEnv().isPresent()) {
            Map<String, Object> env = configure.getEnv().get();
            List<String> flatEnv = env.entrySet()
                    .stream()
                    .map(value -> {
                        JdbcUrlGuard.validateParameter(value.getKey(), String.valueOf(value.getValue()));
                        return String.format("%s=%s", value.getKey(), value.getValue());
                    })
                    .collect(Collectors.toList());
            if (configure.getSsl().isEmpty()) {
                buffer.append("?");
            }
            else {
                if (configure.getIsAppendChar()) {
                    buffer.append("&");
                }
            }
            buffer.append(String.join("&", flatEnv));
        }
        return buffer.toString();
    }

    @Deprecated
    default Response execute(String content)
    {
        Connection connection = local.get();
        Response response = new Response();
        response.setContent(content);
        if (connection != null) {
            log.info("Execute [ {} ] plugin started", this.name());
            Adapter adapter;
            if (type().equals(PluginType.JDBC)) {
                adapter = new JdbcAdapter(connection);
            }
            else if (type().equals(PluginType.NATIVE)) {
                adapter = new NativeAdapter(connection);
            }
            else {
                adapter = new HttpAdapter(connection);
            }
            response = adapter.handlerExecute(content);
            log.info("Execute [ {} ] plugin end", this.name());
        }
        destroy();
        return response;
    }

    default Response execute(RuntimeConfigure configure, String content)
    {
        this.connect(configure);
        return this.execute(content);
    }

    /**
     * 当前插件是否支持流式读写
     * Whether the plugin supports streaming read / batched write.
     * Default: only JDBC plugins. Override to opt-in or opt-out.
     */
    default boolean supportsStreaming()
    {
        return type().equals(PluginType.JDBC);
    }

    /**
     * 流式执行查询，按行回调，不在 JVM 内物化结果集。
     * Stream rows out of the source one at a time. The implementation must NOT materialize
     * the full result set in heap.
     */
    default void executeStream(RuntimeConfigure configure, String content, int fetchSize, RowCallback callback)
    {
        if (type().equals(PluginType.JDBC)) {
            JdbcStreamAdapter.executeStream(this, configure, content, fetchSize, callback);
        }
        else {
            throw new UnsupportedOperationException("Streaming read is not supported for plugin " + this.name());
        }
    }

    /**
     * 打开批量写入会话，调用方按行 add 后由会话内部按 batchSize flush。
     * Open a batch-write session. Caller pushes rows; the writer flushes every batchSize rows
     * and on close(). Must be closed by the caller.
     */
    default BatchWriter openBatchWriter(
            RuntimeConfigure configure,
            String database,
            String table,
            java.util.List<String> columns,
            int batchSize)
    {
        if (type().equals(PluginType.JDBC)) {
            return JdbcStreamAdapter.openBatchWriter(this, configure, database, table, columns, batchSize);
        }
        else {
            throw new UnsupportedOperationException("Batch write is not supported for plugin " + this.name());
        }
    }

    /**
     * 获取数据库支持的引擎
     * Get database supported engines
     *
     * @return 引擎列表 | Engine list
     */
    default Response getEngines()
    {
        return Response.builder()
                .columns(Lists.newArrayList(
                        "InnoDB",
                        "MyISAM"
                ))
                .isConnected(true)
                .isSuccessful(true)
                .build();
    }

    /**
     * 获取数据库支持数据类型
     * Get database supported data types
     *
     * @return 数据类型列表 | Data type list
     */
    default Response getDataTypes()
    {
        return Response.builder()
                .columns(Arrays.asList(DataType.values()))
                .isConnected(true)
                .isSuccessful(true)
                .build();
    }

    /**
     * 获取数据库列表
     * Get database list
     *
     * @param configure 配置信息 | Configuration information
     * @return 数据库列表 | Database list
     */
    default Response getDatabases(RuntimeConfigure configure)
    {
        String sql = "SELECT\n" +
                "    SCHEMA_NAME AS object_name,\n" +
                "    'DATABASE' AS object_type,\n" +
                "    '' AS object_comment\n" +
                "FROM information_schema.SCHEMATA;";
        Response response = this.execute(configure, sql);
        // Hive、达梦等数据源不提供 information_schema，执行失败时回退到 JDBC 元数据
        if (Boolean.TRUE.equals(response.getIsConnected()) && Boolean.FALSE.equals(response.getIsSuccessful())) {
            return metadataDatabases(configure);
        }
        return response;
    }

    /**
     * 通过 JDBC DatabaseMetaData 获取数据库列表
     * Load databases via JDBC DatabaseMetaData, for sources without information_schema
     *
     * @param configure 配置信息 | Configuration information
     * @return 数据库列表 | Database list
     */
    default Response metadataDatabases(RuntimeConfigure configure)
    {
        Response response = new Response();
        response.setHeaders(Lists.newArrayList("object_name", "object_type", "object_comment"));
        response.setTypes(Lists.newArrayList("String", "String", "String"));
        List<Object> rows = Lists.newArrayList();
        response.setColumns(rows);
        try {
            DatabaseMetaData metaData = openJdbcConnection(configure).getMetaData();
            try (ResultSet resultSet = metaData.getSchemas()) {
                while (resultSet.next()) {
                    rows.add(Lists.newArrayList(resultSet.getString("TABLE_SCHEM"), "DATABASE", ""));
                }
            }
            // 个别驱动只暴露 catalog 而没有 schema（如部分 MySQL 系）
            if (rows.isEmpty()) {
                try (ResultSet resultSet = metaData.getCatalogs()) {
                    while (resultSet.next()) {
                        rows.add(Lists.newArrayList(resultSet.getString("TABLE_CAT"), "DATABASE", ""));
                    }
                }
            }
            response.setIsConnected(true);
            response.setIsSuccessful(true);
        }
        catch (Exception ex) {
            log.error("Failed to load databases via JDBC metadata", ex);
            response.setIsConnected(Boolean.FALSE);
            response.setIsSuccessful(Boolean.FALSE);
            response.setMessage(ex.getMessage());
        }
        finally {
            destroy();
        }
        return response;
    }

    /**
     * 打开 JDBC 连接并返回原生连接对象，仅供元数据回退使用
     * Open a JDBC connection and expose the native connection for metadata fallbacks
     *
     * @param configure 配置信息 | Configuration information
     * @return 原生 JDBC 连接 | Native JDBC connection
     */
    private java.sql.Connection openJdbcConnection(RuntimeConfigure configure)
    {
        this.connect(configure);
        Connection connection = local.get();
        if (connection == null || !(connection.getConnection() instanceof java.sql.Connection)) {
            throw new IllegalStateException("Connection is not available or not a JDBC connection");
        }
        return (java.sql.Connection) connection.getConnection();
    }

    /**
     * 根据数据库获取数据表结构
     * Get table structure by database
     *
     * @param configure 配置信息 | Configuration information
     * @param database 数据库 | Database
     * @return 数据表结构 | Table structure
     */
    default Response getTables(RuntimeConfigure configure, String database)
    {
        String sql = "SELECT\n" +
                "    CASE\n" +
                "        WHEN type = 'BASE TABLE' THEN 'table'\n" +
                "        WHEN type = 'VIEW' THEN 'view'\n" +
                "        WHEN type = 'FUNCTION' THEN 'function'\n" +
                "        WHEN type = 'PROCEDURE' THEN 'procedure'\n" +
                "    END AS type_name,\n" +
                "    object_name,\n" +
                "    object_comment\n" +
                "FROM (\n" +
                "    -- 表\n" +
                "    SELECT\n" +
                "        'BASE TABLE' as type,\n" +
                "        TABLE_NAME as object_name,\n" +
                "        TABLE_COMMENT as object_comment\n" +
                "    FROM information_schema.TABLES\n" +
                "    WHERE TABLE_SCHEMA = '{0}'\n" +
                "        AND TABLE_TYPE = 'BASE TABLE'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 视图\n" +
                "    SELECT\n" +
                "        'VIEW' as type,\n" +
                "        TABLE_NAME as object_name,\n" +
                "        TABLE_COMMENT as object_comment\n" +
                "    FROM information_schema.TABLES\n" +
                "    WHERE TABLE_SCHEMA = '{0}'\n" +
                "        AND TABLE_TYPE = 'VIEW'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 函数\n" +
                "    SELECT\n" +
                "        'FUNCTION' as type,\n" +
                "        ROUTINE_NAME as object_name,\n" +
                "        ROUTINE_COMMENT as object_comment\n" +
                "    FROM information_schema.ROUTINES\n" +
                "    WHERE ROUTINE_SCHEMA = '{0}'\n" +
                "        AND ROUTINE_TYPE = 'FUNCTION'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 存储过程（查询）\n" +
                "    SELECT\n" +
                "        'PROCEDURE' as type,\n" +
                "        ROUTINE_NAME as object_name,\n" +
                "        ROUTINE_COMMENT as object_comment\n" +
                "    FROM information_schema.ROUTINES\n" +
                "    WHERE ROUTINE_SCHEMA = '{0}'\n" +
                "        AND ROUTINE_TYPE = 'PROCEDURE'\n" +
                ") AS combined_objects\n" +
                "ORDER BY\n" +
                "    FIELD(type, 'BASE TABLE', 'VIEW', 'FUNCTION', 'PROCEDURE'),\n" +
                "    object_name;";

        Response response = this.execute(configure, sql.replace("{0}", database));
        // 达梦等数据源不提供 information_schema，执行失败时回退到 JDBC 元数据
        if (Boolean.TRUE.equals(response.getIsConnected()) && Boolean.FALSE.equals(response.getIsSuccessful())) {
            return metadataTables(configure, database);
        }
        return response;
    }

    /**
     * 通过 JDBC DatabaseMetaData 获取数据表和视图列表
     * Load tables and views via JDBC DatabaseMetaData, for sources without information_schema
     *
     * @param configure 配置信息 | Configuration information
     * @param database 数据库 | Database
     * @return 数据表列表 | Table list
     */
    default Response metadataTables(RuntimeConfigure configure, String database)
    {
        Response response = new Response();
        response.setHeaders(Lists.newArrayList("type_name", "object_name", "object_comment"));
        response.setTypes(Lists.newArrayList("String", "String", "String"));
        List<Object> rows = Lists.newArrayList();
        response.setColumns(rows);
        try {
            DatabaseMetaData metaData = openJdbcConnection(configure).getMetaData();
            try (ResultSet resultSet = metaData.getTables(null, database, "%", new String[] {"TABLE", "VIEW"})) {
                while (resultSet.next()) {
                    String typeName = "VIEW".equals(resultSet.getString("TABLE_TYPE")) ? "view" : "table";
                    String comment = resultSet.getString("REMARKS");
                    rows.add(Lists.newArrayList(typeName, resultSet.getString("TABLE_NAME"), comment == null ? "" : comment));
                }
            }
            response.setIsConnected(true);
            response.setIsSuccessful(true);
        }
        catch (Exception ex) {
            log.error("Failed to load tables via JDBC metadata", ex);
            response.setIsConnected(Boolean.FALSE);
            response.setIsSuccessful(Boolean.FALSE);
            response.setMessage(ex.getMessage());
        }
        finally {
            destroy();
        }
        return response;
    }

    /**
     * 根据数据库，数据表获取列结构
     * Get column structure by database, table
     *
     * @param configure 配置信息 | Configuration information
     * @param database 数据库 | Database
     * @param table 数据表 | Table
     * @return 数据列结构 | Column structure
     */
    default Response getColumns(RuntimeConfigure configure, String database, String table)
    {
        String sql = "SELECT detail.*,\n" +
                "    COALESCE(meta.CHARACTER_MAXIMUM_LENGTH, meta.NUMERIC_PRECISION, 0) AS object_length,\n" +
                "    CASE WHEN meta.EXTRA LIKE '%auto_increment%' THEN 'YES' ELSE 'NO' END AS object_autoincrement,\n" +
                "    meta.CHARACTER_SET_NAME AS object_charset,\n" +
                "    meta.COLLATION_NAME AS object_collation\n" +
                "FROM (\n" +
                "    -- 列信息\n" +
                "    SELECT\n" +
                "        'column' as type_name,\n" +
                "        COLUMN_NAME as object_name,\n" +
                "        COLUMN_TYPE as object_data_type,\n" +
                "        IS_NULLABLE as object_nullable,\n" +
                "        COLUMN_DEFAULT as object_default_value,\n" +
                "        COLUMN_COMMENT as object_comment,\n" +
                "        ORDINAL_POSITION as object_position,\n" +
                "        '' as object_definition\n" +
                "    FROM\n" +
                "        information_schema.COLUMNS\n" +
                "    WHERE\n" +
                "        TABLE_SCHEMA = '{0}' -- 数据库名参数\n" +
                "        AND TABLE_NAME = '{1}' -- 表名参数\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 主键信息\n" +
                "    SELECT\n" +
                "        'primary' as type_name,\n" +
                "        COLUMN_NAME as object_name,\n" +
                "        '' as object_data_type,\n" +
                "        '' as object_nullable,\n" +
                "        '' as object_default_value,\n" +
                "        '' as object_comment,\n" +
                "        0 as object_position,\n" +
                "        CONCAT('PRIMARY KEY on (',\n" +
                "            GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION),\n" +
                "            ')'\n" +
                "        ) as object_definition\n" +
                "    FROM\n" +
                "        information_schema.KEY_COLUMN_USAGE\n" +
                "    WHERE\n" +
                "        TABLE_SCHEMA = '{0}'\n" +
                "        AND TABLE_NAME = '{1}'\n" +
                "        AND CONSTRAINT_NAME = 'PRIMARY'\n" +
                "    GROUP BY\n" +
                "        TABLE_SCHEMA, TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 索引信息\n" +
                "    SELECT DISTINCT\n" +
                "        'index' as type_name,\n" +
                "        COLUMN_NAME as object_name,\n" +
                "        '' as object_data_type,\n" +
                "        '' as object_nullable,\n" +
                "        '' as object_default_value,\n" +
                "        '' as object_comment,\n" +
                "        0 as object_position,\n" +
                "        CONCAT(\n" +
                "            CASE NON_UNIQUE\n" +
                "                WHEN 1 THEN 'Non-unique'\n" +
                "                ELSE 'Unique'\n" +
                "            END,\n" +
                "            ' index on (',\n" +
                "            GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX),\n" +
                "            ')'\n" +
                "        ) as object_definition\n" +
                "    FROM\n" +
                "        information_schema.STATISTICS\n" +
                "    WHERE\n" +
                "        TABLE_SCHEMA = '{0}'\n" +
                "        AND TABLE_NAME = '{1}'\n" +
                "    GROUP BY\n" +
                "        TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, NON_UNIQUE\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 触发器信息\n" +
                "    SELECT\n" +
                "        'trigger' as type_name,\n" +
                "        TRIGGER_NAME as object_name,\n" +
                "        '' as object_data_type,\n" +
                "        '' as object_nullable,\n" +
                "        '' as object_default_value,\n" +
                "        '' as object_comment,\n" +
                "        0 as object_position,\n" +
                "        CONCAT(\n" +
                "            'TRIGGER ',\n" +
                "            ACTION_TIMING, ' ',\n" +
                "            EVENT_MANIPULATION\n" +
                "        ) as object_definition\n" +
                "    FROM\n" +
                "        information_schema.TRIGGERS\n" +
                "    WHERE\n" +
                "        EVENT_OBJECT_SCHEMA = '{0}'\n" +
                "        AND EVENT_OBJECT_TABLE = '{1}'\n" +
                ") detail\n" +
                "LEFT JOIN information_schema.COLUMNS meta\n" +
                "    ON detail.type_name = 'column' AND meta.TABLE_SCHEMA = '{0}'\n" +
                "    AND meta.TABLE_NAME = '{1}' AND meta.COLUMN_NAME = detail.object_name\n" +
                "ORDER BY\n" +
                "    FIELD(type_name, 'column', 'primary', 'index', 'trigger'),\n" +
                "    object_position,\n" +
                "    object_name;";

        Response response = this.execute(
                configure,
                sql.replace("{0}", database)
                        .replace("{1}", table)
        );
        // 达梦等数据源不提供 information_schema，执行失败时回退到 JDBC 元数据
        if (Boolean.TRUE.equals(response.getIsConnected()) && Boolean.FALSE.equals(response.getIsSuccessful())) {
            return metadataColumns(configure, database, table);
        }
        return response;
    }

    /**
     * 通过 JDBC DatabaseMetaData 获取列和主键信息
     * Load columns and primary keys via JDBC DatabaseMetaData, for sources without information_schema
     *
     * @param configure 配置信息 | Configuration information
     * @param database 数据库 | Database
     * @param table 数据表 | Table
     * @return 数据列结构 | Column structure
     */
    default Response metadataColumns(RuntimeConfigure configure, String database, String table)
    {
        Response response = new Response();
        response.setHeaders(Lists.newArrayList(
                "type_name",
                "object_name",
                "object_data_type",
                "object_nullable",
                "object_default_value",
                "object_comment",
                "object_position",
                "object_definition",
                "object_length",
                "object_autoincrement",
                "object_scale"));
        response.setTypes(Lists.newArrayList(
                "String", "String", "String", "String", "String", "String", "Integer", "String", "Integer", "String", "Integer"));
        List<Object> rows = Lists.newArrayList();
        response.setColumns(rows);
        try {
            DatabaseMetaData metaData = openJdbcConnection(configure).getMetaData();
            try (ResultSet resultSet = metaData.getColumns(null, database, table, "%")) {
                while (resultSet.next()) {
                    String defaultValue = resultSet.getString("COLUMN_DEF");
                    String comment = resultSet.getString("REMARKS");
                    int columnSize;
                    try {
                        columnSize = resultSet.getInt("COLUMN_SIZE");
                    }
                    catch (SQLException ignored) {
                        columnSize = 0;
                    }
                    int decimalDigits;
                    try {
                        decimalDigits = resultSet.getInt("DECIMAL_DIGITS");
                    }
                    catch (SQLException ignored) {
                        decimalDigits = 0;
                    }
                    rows.add(Lists.newArrayList(
                            "column",
                            resultSet.getString("COLUMN_NAME"),
                            resultSet.getString("TYPE_NAME"),
                            resultSet.getInt("NULLABLE") == java.sql.ResultSetMetaData.columnNullable ? "YES" : "NO",
                            defaultValue,
                            comment == null ? "" : comment,
                            resultSet.getInt("ORDINAL_POSITION"),
                            "",
                            columnSize,
                            resultSet.getString("IS_AUTOINCREMENT"),
                            decimalDigits));
                }
            }
            try (ResultSet resultSet = metaData.getPrimaryKeys(null, database, table)) {
                List<String> primaryColumns = Lists.newArrayList();
                while (resultSet.next()) {
                    primaryColumns.add(resultSet.getString("COLUMN_NAME"));
                }
                for (String primaryColumn : primaryColumns) {
                    rows.add(Lists.newArrayList(
                            "primary",
                            primaryColumn,
                            "", "", "", "",
                            0,
                            "PRIMARY KEY on (" + String.join(", ", primaryColumns) + ")",
                            0, "NO", 0));
                }
            }
            response.setIsConnected(true);
            response.setIsSuccessful(true);
        }
        catch (Exception ex) {
            log.error("Failed to load columns via JDBC metadata", ex);
            response.setIsConnected(Boolean.FALSE);
            response.setIsSuccessful(Boolean.FALSE);
            response.setMessage(ex.getMessage());
        }
        finally {
            destroy();
        }
        return response;
    }

    /**
     * 数据表索引结构 | Table index structure
     *
     * @param configure 配置信息 | Configuration information
     * @param database 数据库 | Database
     * @param table 数据表 | Table
     * @return 索引结构 | Index structure
     */
    default Response metadataIndexes(RuntimeConfigure configure, String database, String table)
    {
        Response response = new Response();
        response.setHeaders(Lists.newArrayList(
                "type_name",
                "object_name",
                "object_column",
                "object_unique",
                "object_definition"));
        response.setTypes(Lists.newArrayList(
                "String", "String", "String", "Boolean", "String"));
        List<Object> rows = Lists.newArrayList();
        try (ResultSet resultSet = openJdbcConnection(configure).getMetaData().getIndexInfo(null, database, table, false, false)) {
            while (resultSet.next()) {
                String indexName = resultSet.getString("INDEX_NAME");
                if (indexName == null) {
                    continue;
                }
                rows.add(Lists.newArrayList(
                        "index",
                        indexName,
                        resultSet.getString("COLUMN_NAME"),
                        !resultSet.getBoolean("NON_UNIQUE"),
                        ""));
            }
        }
        catch (Exception ignored) {
            // drivers without index metadata simply report an empty list
        }
        response.setColumns(rows);
        return response;
    }

    /**
     * 数据表外键结构 | Table foreign key structure
     *
     * @param configure 配置信息 | Configuration information
     * @param database 数据库 | Database
     * @param table 数据表 | Table
     * @return 外键结构 | Foreign key structure
     */
    default Response metadataForeignKeys(RuntimeConfigure configure, String database, String table)
    {
        Response response = new Response();
        response.setHeaders(Lists.newArrayList(
                "type_name",
                "object_name",
                "object_column",
                "object_reference_table",
                "object_reference_column"));
        response.setTypes(Lists.newArrayList(
                "String", "String", "String", "String", "String"));
        List<Object> rows = Lists.newArrayList();
        try (ResultSet resultSet = openJdbcConnection(configure).getMetaData().getImportedKeys(null, database, table)) {
            while (resultSet.next()) {
                rows.add(Lists.newArrayList(
                        "foreignkey",
                        resultSet.getString("FK_NAME"),
                        resultSet.getString("FKCOLUMN_NAME"),
                        resultSet.getString("PKTABLE_NAME"),
                        resultSet.getString("PKCOLUMN_NAME")));
            }
        }
        catch (Exception ignored) {
            // drivers without foreign key metadata simply report an empty list
        }
        response.setColumns(rows);
        return response;
    }

    /**
     * 创建索引 | Create index
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response createIndex(RuntimeConfigure configure, TableDefinition definition)
    {
        return this.executeStatements(definition.getIndexes().stream()
                .map(index -> this.createIndexStatement(definition.getDatabase(), definition.getName(), index))
                .collect(Collectors.toList()), configure, definition);
    }

    /**
     * 修改或重建索引 | Change or rebuild an index: drops the original and adds the new definition in
     * one ALTER, so the table never goes without it in between.
     */
    default Response alterIndex(RuntimeConfigure configure, TableDefinition definition)
    {
        return this.executeStatements(definition.getIndexes().stream()
                .map(index -> this.alterIndexStatement(definition.getDatabase(), definition.getName(), index))
                .collect(Collectors.toList()), configure, definition);
    }

    /**
     * 删除索引 | Drop index
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response dropIndex(RuntimeConfigure configure, TableDefinition definition)
    {
        String table = sqlIdentifier(definition.getDatabase()) + "." + sqlIdentifier(definition.getName());
        return this.executeStatements(definition.getIndexes().stream()
                .map(index -> isPrimaryIndex(index.getName())
                        ? "ALTER TABLE " + table + " DROP PRIMARY KEY"
                        : "DROP INDEX " + sqlIdentifier(index.getName()) + " ON " + table)
                .collect(Collectors.toList()), configure, definition);
    }

    default boolean isPrimaryIndex(String name)
    {
        return "PRIMARY".equalsIgnoreCase(name);
    }

    /** FULLTEXT and SPATIAL are index kinds of their own; BTREE and HASH are access methods. */
    default String indexKind(IndexDefinition index)
    {
        String type = index.getType() == null ? "" : index.getType().trim().toUpperCase(java.util.Locale.ROOT);
        return "FULLTEXT".equals(type) || "SPATIAL".equals(type) ? type : "";
    }

    default String indexMethod(IndexDefinition index)
    {
        String type = index.getType() == null ? "" : index.getType().trim().toUpperCase(java.util.Locale.ROOT);
        return "BTREE".equals(type) || "HASH".equals(type) ? " USING " + type : "";
    }

    /** `col`(length) ASC, ... ; FULLTEXT and SPATIAL columns take no direction. */
    default String indexColumns(IndexDefinition index)
    {
        boolean ordered = indexKind(index).isEmpty();
        List<String> parts = Lists.newArrayList();
        for (int position = 0; position < index.getColumns().size(); position++) {
            StringBuilder part = new StringBuilder(sqlIdentifier(index.getColumns().get(position)));
            Integer length = position < index.getLengths().size() ? index.getLengths().get(position) : null;
            if (length != null && length > 0) {
                part.append("(").append(length).append(")");
            }
            String order = position < index.getOrders().size() ? index.getOrders().get(position) : null;
            if (ordered && order != null && ("ASC".equalsIgnoreCase(order) || "DESC".equalsIgnoreCase(order))) {
                part.append(" ").append(order.toUpperCase(java.util.Locale.ROOT));
            }
            parts.add(part.toString());
        }
        return "(" + String.join(", ", parts) + ")";
    }

    default String indexComment(IndexDefinition index)
    {
        return index.getComment() == null || index.getComment().isEmpty() ? "" : " COMMENT " + sqlLiteral(index.getComment());
    }

    default String indexAlgorithm(IndexDefinition index, String separator)
    {
        String algorithm = index.getAlgorithm() == null ? "" : index.getAlgorithm().trim().toUpperCase(java.util.Locale.ROOT);
        return "INPLACE".equals(algorithm) || "COPY".equals(algorithm) ? separator + "ALGORITHM = " + algorithm : "";
    }

    /** The ADD clause of an index inside ALTER TABLE. */
    default String addIndexClause(IndexDefinition index)
    {
        if (isPrimaryIndex(index.getName())) {
            return "ADD PRIMARY KEY" + indexMethod(index) + " " + indexColumns(index) + indexComment(index);
        }
        String kind = indexKind(index);
        String prefix = !kind.isEmpty() ? kind + " " : index.isUnique() ? "UNIQUE " : "";
        return "ADD " + prefix + "INDEX " + sqlIdentifier(index.getName()) + (kind.isEmpty() ? indexMethod(index) : "") + " " + indexColumns(index) + indexComment(index);
    }

    default String createIndexStatement(String database, String table, IndexDefinition index)
    {
        String target = sqlIdentifier(database) + "." + sqlIdentifier(table);
        if (isPrimaryIndex(index.getName())) {
            return "ALTER TABLE " + target + "\n    " + addIndexClause(index) + indexAlgorithm(index, ",\n    ");
        }
        String kind = indexKind(index);
        String prefix = !kind.isEmpty() ? kind + " " : index.isUnique() ? "UNIQUE " : "";
        return "CREATE " + prefix + "INDEX " + sqlIdentifier(index.getName()) + (kind.isEmpty() ? indexMethod(index) : "")
                + "\n    ON " + target + " " + indexColumns(index) + indexComment(index) + indexAlgorithm(index, "\n    ");
    }

    default String alterIndexStatement(String database, String table, IndexDefinition index)
    {
        String original = index.getOriginalName() == null || index.getOriginalName().isEmpty() ? index.getName() : index.getOriginalName();
        String drop = isPrimaryIndex(original) ? "DROP PRIMARY KEY" : "DROP INDEX " + sqlIdentifier(original);
        return "ALTER TABLE " + sqlIdentifier(database) + "." + sqlIdentifier(table)
                + "\n    " + drop + ",\n    " + addIndexClause(index) + indexAlgorithm(index, ",\n    ");
    }

    /**
     * 索引明细 | One row per index column with what the index page shows: kind, uniqueness, order,
     * prefix, cardinality, comment, an estimated key length (as EXPLAIN reports key_len) and, when
     * performance_schema is readable, how often the index was used.
     * Rows: index_name, non_unique, seq_in_index, column_name, collation, cardinality, sub_part,
     * index_type, index_comment, nullable, key_length, usage_count, usage_rows.
     */
    default Response getIndexDetails(RuntimeConfigure configure, String database, String table)
    {
        String schema = sqlLiteral(database);
        String name = sqlLiteral(table);
        // Bytes per character for the column's character set, from its declared length.
        String charBytes = "IFNULL(c.CHARACTER_OCTET_LENGTH / NULLIF(c.CHARACTER_MAXIMUM_LENGTH, 0), 1)";
        String keyLength = "CASE\n"
                + "        WHEN c.DATA_TYPE IN ('tinyint') THEN 1 WHEN c.DATA_TYPE = 'smallint' THEN 2 WHEN c.DATA_TYPE = 'mediumint' THEN 3\n"
                + "        WHEN c.DATA_TYPE IN ('int', 'integer', 'float', 'timestamp') THEN 4 WHEN c.DATA_TYPE IN ('bigint', 'double') THEN 8\n"
                + "        WHEN c.DATA_TYPE = 'date' THEN 3 WHEN c.DATA_TYPE = 'datetime' THEN 5 WHEN c.DATA_TYPE = 'time' THEN 3 WHEN c.DATA_TYPE = 'year' THEN 1\n"
                + "        WHEN c.DATA_TYPE = 'decimal' THEN CEIL(c.NUMERIC_PRECISION / 2)\n"
                + "        WHEN c.DATA_TYPE IN ('char', 'varchar', 'binary', 'varbinary', 'tinytext', 'text', 'mediumtext', 'longtext', 'tinyblob', 'blob', 'mediumblob', 'longblob')\n"
                + "            THEN IFNULL(s.SUB_PART * " + charBytes + ", c.CHARACTER_OCTET_LENGTH) + IF(c.DATA_TYPE IN ('char', 'binary'), 0, 2)\n"
                + "        ELSE NULL END + IF(c.IS_NULLABLE = 'YES', 1, 0)";
        String sql = "SELECT s.INDEX_NAME AS index_name, s.NON_UNIQUE AS non_unique, s.SEQ_IN_INDEX AS seq_in_index, s.COLUMN_NAME AS column_name,\n"
                + "    s.COLLATION AS collation, s.CARDINALITY AS cardinality, s.SUB_PART AS sub_part, s.INDEX_TYPE AS index_type,\n"
                + "    s.INDEX_COMMENT AS index_comment, s.NULLABLE AS nullable,\n"
                // Key length only applies to BTREE-style indexes; FULLTEXT and SPATIAL report none.
                + "    IF(s.INDEX_TYPE IN ('FULLTEXT', 'SPATIAL'), NULL, CAST(ROUND(" + keyLength + ") AS UNSIGNED)) AS key_length\n"
                + "FROM information_schema.STATISTICS s\n"
                + "LEFT JOIN information_schema.COLUMNS c\n"
                + "    ON c.TABLE_SCHEMA = s.TABLE_SCHEMA AND c.TABLE_NAME = s.TABLE_NAME AND c.COLUMN_NAME = s.COLUMN_NAME\n"
                + "WHERE s.TABLE_SCHEMA = " + schema + " AND s.TABLE_NAME = " + name + "\n"
                + "ORDER BY s.INDEX_NAME = 'PRIMARY' DESC, s.INDEX_NAME, s.SEQ_IN_INDEX";
        Response response = this.execute(configure, sql);
        if (response == null || !Boolean.TRUE.equals(response.getIsSuccessful())) {
            return response;
        }
        // Usage is optional: performance_schema may be off or unreadable for this account.
        java.util.Map<String, ObjectNode> usage = new java.util.HashMap<>();
        Response stats = this.execute(configure, "SELECT INDEX_NAME AS index_name, COUNT_STAR AS usage_count, COUNT_READ AS usage_rows\n"
                + "FROM performance_schema.table_io_waits_summary_by_index_usage\n"
                + "WHERE OBJECT_SCHEMA = " + schema + " AND OBJECT_NAME = " + name + " AND INDEX_NAME IS NOT NULL");
        if (stats != null && Boolean.TRUE.equals(stats.getIsSuccessful()) && stats.getColumns() != null) {
            stats.getColumns().stream().filter(ObjectNode.class::isInstance).map(ObjectNode.class::cast)
                    .forEach(row -> usage.put(row.path("index_name").asText(), row));
        }
        if (response.getColumns() != null) {
            response.getColumns().stream().filter(ObjectNode.class::isInstance).map(ObjectNode.class::cast).forEach(row -> {
                ObjectNode used = usage.get(row.path("index_name").asText());
                row.set("usage_count", used == null ? row.nullNode() : used.get("usage_count"));
                row.set("usage_rows", used == null ? row.nullNode() : used.get("usage_rows"));
            });
        }
        List<String> headers = response.getHeaders() == null ? Lists.newArrayList() : Lists.newArrayList(response.getHeaders());
        headers.add("usage_count");
        headers.add("usage_rows");
        response.setHeaders(headers);
        response.setContent(sql);
        return response;
    }

    /**
     * 外键明细 | One row per foreign key column of this table: the referenced column, the update
     * and delete rules, and whether the referenced table still exists.
     * Rows: constraint_name, column_name, ordinal_position, referenced_database, referenced_table,
     * referenced_column, update_rule, delete_rule, referenced_exists.
     */
    default Response getForeignKeyDetails(RuntimeConfigure configure, String database, String table)
    {
        String sql = "SELECT k.CONSTRAINT_NAME AS constraint_name, k.COLUMN_NAME AS column_name, k.ORDINAL_POSITION AS ordinal_position,\n"
                + "    k.REFERENCED_TABLE_SCHEMA AS referenced_database, k.REFERENCED_TABLE_NAME AS referenced_table,\n"
                + "    k.REFERENCED_COLUMN_NAME AS referenced_column, r.UPDATE_RULE AS update_rule, r.DELETE_RULE AS delete_rule,\n"
                + "    IF(t.TABLE_NAME IS NULL, 0, 1) AS referenced_exists\n"
                + "FROM information_schema.KEY_COLUMN_USAGE k\n"
                + "JOIN information_schema.REFERENTIAL_CONSTRAINTS r\n"
                + "    ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME AND r.TABLE_NAME = k.TABLE_NAME\n"
                + "LEFT JOIN information_schema.TABLES t\n"
                + "    ON t.TABLE_SCHEMA = k.REFERENCED_TABLE_SCHEMA AND t.TABLE_NAME = k.REFERENCED_TABLE_NAME\n"
                + "WHERE k.TABLE_SCHEMA = " + sqlLiteral(database) + " AND k.TABLE_NAME = " + sqlLiteral(table) + " AND k.REFERENCED_TABLE_NAME IS NOT NULL\n"
                + "ORDER BY k.CONSTRAINT_NAME, k.ORDINAL_POSITION";
        Response response = this.execute(configure, sql);
        if (response != null) {
            response.setContent(sql);
        }
        return response;
    }

    default Response createForeignKey(RuntimeConfigure configure, TableDefinition definition)
    {
        return this.executeStatements(definition.getForeignKeys().stream()
                .map(key -> "ALTER TABLE " + sqlIdentifier(definition.getDatabase()) + "." + sqlIdentifier(definition.getName()) + this.addForeignKeyClause(definition, key))
                .collect(Collectors.toList()), configure, definition);
    }

    /**
     * 修改或重建外键 | Change or rebuild a foreign key: MySQL only drops and adds a foreign key in one
     * ALTER with ALGORITHM=INPLACE, which adding one with foreign key checks on does not allow, so it
     * is dropped first and added in a second statement.
     */
    default Response alterForeignKey(RuntimeConfigure configure, TableDefinition definition)
    {
        String table = sqlIdentifier(definition.getDatabase()) + "." + sqlIdentifier(definition.getName());
        List<String> statements = Lists.newArrayList();
        definition.getForeignKeys().forEach(key -> {
            String original = key.getOriginalName() == null || key.getOriginalName().isEmpty() ? key.getName() : key.getOriginalName();
            statements.add("ALTER TABLE " + table + " DROP FOREIGN KEY " + sqlIdentifier(original));
            statements.add("ALTER TABLE " + table + this.addForeignKeyClause(definition, key));
        });
        return this.executeStatements(statements, configure, definition);
    }

    default Response dropForeignKey(RuntimeConfigure configure, TableDefinition definition)
    {
        String table = sqlIdentifier(definition.getDatabase()) + "." + sqlIdentifier(definition.getName());
        return this.executeStatements(definition.getForeignKeys().stream()
                .map(key -> "ALTER TABLE " + table + " DROP FOREIGN KEY " + sqlIdentifier(key.getName()))
                .collect(Collectors.toList()), configure, definition);
    }

    /** The ADD CONSTRAINT part, laid out one clause per line as the prototype shows it. */
    default String addForeignKeyClause(TableDefinition definition, ForeignKeyDefinition key)
    {
        String referencedDatabase = key.getReferencedDatabase() == null || key.getReferencedDatabase().isEmpty() ? definition.getDatabase() : key.getReferencedDatabase();
        StringBuilder clause = new StringBuilder()
                .append("\n    ADD CONSTRAINT ").append(sqlIdentifier(key.getName()))
                .append("\n    FOREIGN KEY (").append(key.getColumns().stream().map(this::sqlIdentifier).collect(Collectors.joining(", "))).append(")")
                .append("\n    REFERENCES ").append(sqlIdentifier(referencedDatabase)).append(".").append(sqlIdentifier(key.getReferencedTable()))
                .append(" (").append(key.getReferencedColumns().stream().map(this::sqlIdentifier).collect(Collectors.joining(", "))).append(")");
        String onUpdate = foreignKeyRule(key.getOnUpdate());
        String onDelete = foreignKeyRule(key.getOnDelete());
        if (!onUpdate.isEmpty()) {
            clause.append("\n    ON UPDATE ").append(onUpdate);
        }
        if (!onDelete.isEmpty()) {
            clause.append("\n    ON DELETE ").append(onDelete);
        }
        return clause.toString();
    }

    /** Only the referential actions InnoDB enforces; anything else falls back to the default. */
    default String foreignKeyRule(String rule)
    {
        String value = rule == null ? "" : rule.trim().toUpperCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
        return java.util.Arrays.asList("RESTRICT", "CASCADE", "SET NULL", "NO ACTION").contains(value) ? value : "";
    }

    /**
     * 触发器明细 | Triggers of the table with timing, event, body, definer, creation time, SQL mode
     * and client character set.
     */
    default Response getTriggerDetails(RuntimeConfigure configure, String database, String table)
    {
        String sql = "SELECT TRIGGER_NAME AS trigger_name, ACTION_TIMING AS timing, EVENT_MANIPULATION AS event, ACTION_STATEMENT AS body,\n"
                + "    DEFINER AS definer, CREATED AS created, SQL_MODE AS sql_mode, CHARACTER_SET_CLIENT AS charset, ACTION_ORDER AS action_order\n"
                + "FROM information_schema.TRIGGERS\n"
                + "WHERE EVENT_OBJECT_SCHEMA = " + sqlLiteral(database) + " AND EVENT_OBJECT_TABLE = " + sqlLiteral(table) + "\n"
                + "ORDER BY ACTION_TIMING, EVENT_MANIPULATION, ACTION_ORDER";
        Response response = this.execute(configure, sql);
        if (response != null) {
            response.setContent(sql);
        }
        return response;
    }

    default Response createTrigger(RuntimeConfigure configure, TableDefinition definition)
    {
        return this.executeStatements(definition.getTriggers().stream()
                .map(trigger -> this.createTriggerStatement(definition.getDatabase(), definition.getName(), trigger))
                .collect(Collectors.toList()), configure, definition);
    }

    default Response dropTrigger(RuntimeConfigure configure, TableDefinition definition)
    {
        return this.executeStatements(definition.getTriggers().stream()
                .map(trigger -> "DROP TRIGGER " + sqlIdentifier(definition.getDatabase()) + "." + sqlIdentifier(trigger.getName()))
                .collect(Collectors.toList()), configure, definition);
    }

    /**
     * 修改或重建触发器 | MySQL has no ALTER TRIGGER, so the trigger is dropped and created again. The
     * original statement is read first; when the new one fails it is run again so the table keeps
     * its trigger.
     */
    default Response alterTrigger(RuntimeConfigure configure, TableDefinition definition)
    {
        TriggerDefinition trigger = definition.getTriggers().get(0);
        String original = trigger.getOriginalName() == null || trigger.getOriginalName().isEmpty() ? trigger.getName() : trigger.getOriginalName();
        String drop = "DROP TRIGGER " + sqlIdentifier(definition.getDatabase()) + "." + sqlIdentifier(original) + ";";
        String create = this.createTriggerStatement(definition.getDatabase(), definition.getName(), trigger) + ";";
        if (definition.isPreview()) {
            return this.getResponse(drop + "\n" + create, configure, definition);
        }
        Response shown = this.execute(configure, "SHOW CREATE TRIGGER " + sqlIdentifier(definition.getDatabase()) + "." + sqlIdentifier(original));
        String restore = shown != null && Boolean.TRUE.equals(shown.getIsSuccessful()) && shown.getColumns() != null && !shown.getColumns().isEmpty()
                && shown.getColumns().get(0) instanceof ObjectNode
                ? ((ObjectNode) shown.getColumns().get(0)).path("SQL Original Statement").asText(null) : null;
        Response dropped = this.getResponse(drop, configure, definition);
        if (!Boolean.TRUE.equals(dropped.getIsSuccessful())) {
            dropped.setContent(drop);
            return dropped;
        }
        Response created = this.getResponse(create, configure, definition);
        created.setContent(drop + "\n" + create);
        if (!Boolean.TRUE.equals(created.getIsSuccessful()) && restore != null) {
            Response restored = this.execute(configure, restore);
            boolean back = restored != null && Boolean.TRUE.equals(restored.getIsSuccessful());
            created.setContent(drop + "\n" + create + "\n" + restore + ";");
            created.setMessage(created.getMessage() + (back ? " (the original trigger was restored)" : " (restoring the original trigger failed: " + (restored == null ? "" : restored.getMessage()) + ")"));
        }
        return created;
    }

    default String createTriggerStatement(String database, String table, TriggerDefinition trigger)
    {
        String timing = "AFTER".equalsIgnoreCase(trigger.getTiming()) ? "AFTER" : "BEFORE";
        String event = trigger.getEvent() == null ? "" : trigger.getEvent().trim().toUpperCase(java.util.Locale.ROOT);
        if (!java.util.Arrays.asList("INSERT", "UPDATE", "DELETE").contains(event)) {
            throw new IllegalArgumentException("Trigger event must be INSERT, UPDATE or DELETE");
        }
        String body = trigger.getBody() == null ? "" : trigger.getBody().trim().replaceAll(";+\\s*$", "");
        if (body.isEmpty()) {
            throw new IllegalArgumentException("Trigger body is required");
        }
        StringBuilder sql = new StringBuilder("CREATE ");
        String definer = trigger.getDefiner() == null ? "" : trigger.getDefiner().trim();
        if (!definer.isEmpty()) {
            int at = definer.lastIndexOf('@');
            String user = (at < 0 ? definer : definer.substring(0, at)).replaceAll("^[`'\"]|[`'\"]$", "");
            String host = (at < 0 ? "%" : definer.substring(at + 1)).replaceAll("^[`'\"]|[`'\"]$", "");
            sql.append("DEFINER = ").append(sqlLiteral(user)).append("@").append(sqlLiteral(host)).append(" ");
        }
        return sql.append("TRIGGER ").append(sqlIdentifier(database)).append(".").append(sqlIdentifier(trigger.getName()))
                .append("\n").append(timing).append(" ").append(event).append(" ON ").append(sqlIdentifier(database)).append(".").append(sqlIdentifier(table))
                .append("\nFOR EACH ROW\n").append(body).toString();
    }

    /**
     * 表属性 | The table's options and statistics: type, engine, row format, character set and
     * collation, next auto-increment value, data and index bytes, rows, comment, and index and
     * foreign key counts.
     */
    default Response getTableProperties(RuntimeConfigure configure, String database, String table)
    {
        String schema = sqlLiteral(database);
        String name = sqlLiteral(table);
        String sql = "SELECT t.TABLE_NAME AS table_name, t.TABLE_SCHEMA AS table_schema, t.TABLE_TYPE AS table_type, t.ENGINE AS engine,\n"
                + "    t.ROW_FORMAT AS row_format, t.CREATE_TIME AS create_time, t.UPDATE_TIME AS update_time, c.CHARACTER_SET_NAME AS charset,\n"
                + "    t.TABLE_COLLATION AS collation, t.AUTO_INCREMENT AS auto_increment, t.DATA_LENGTH AS data_length,\n"
                + "    t.INDEX_LENGTH AS index_length, t.TABLE_ROWS AS table_rows, t.TABLE_COMMENT AS table_comment,\n"
                + "    (SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS s WHERE s.TABLE_SCHEMA = t.TABLE_SCHEMA AND s.TABLE_NAME = t.TABLE_NAME) AS index_count,\n"
                + "    (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS r WHERE r.CONSTRAINT_SCHEMA = t.TABLE_SCHEMA AND r.TABLE_NAME = t.TABLE_NAME) AS foreign_key_count\n"
                + "FROM information_schema.TABLES t\n"
                + "LEFT JOIN information_schema.COLLATIONS c ON c.COLLATION_NAME = t.TABLE_COLLATION\n"
                + "WHERE t.TABLE_SCHEMA = " + schema + " AND t.TABLE_NAME = " + name;
        Response response = this.execute(configure, sql);
        if (response != null) {
            response.setContent(sql);
        }
        return response;
    }

    /** 字符集与排序规则 | Every character set with its collations; is_default marks the default one. */
    default Response getCharacterSets(RuntimeConfigure configure)
    {
        return this.execute(configure, "SELECT CHARACTER_SET_NAME AS charset, COLLATION_NAME AS collation, IS_DEFAULT AS is_default\n"
                + "FROM information_schema.COLLATIONS WHERE CHARACTER_SET_NAME IS NOT NULL ORDER BY CHARACTER_SET_NAME, COLLATION_NAME");
    }

    /** 修改表属性 | Changes the given table options in one ALTER TABLE. */
    default Response alterTableOptions(RuntimeConfigure configure, TableDefinition definition)
    {
        return this.executeStatements(Lists.newArrayList(this.tableOptionsStatement(definition.getDatabase(), definition.getName(), definition.getOptions())),
                configure, definition);
    }

    default String tableOptionsStatement(String database, String table, TableOptionsDefinition options)
    {
        List<String> clauses = Lists.newArrayList();
        if (options != null) {
            if (options.getEngine() != null) {
                clauses.add("ENGINE = " + tableOptionWord(options.getEngine(), "engine"));
            }
            if (options.getRowFormat() != null) {
                clauses.add("ROW_FORMAT = " + tableOptionWord(options.getRowFormat(), "row format").toUpperCase(java.util.Locale.ROOT));
            }
            if (options.getAutoIncrement() != null) {
                if (options.getAutoIncrement() < 1) {
                    throw new IllegalArgumentException("The next auto-increment value must be at least 1");
                }
                clauses.add("AUTO_INCREMENT = " + options.getAutoIncrement());
            }
            if (options.getCharset() != null) {
                clauses.add("DEFAULT CHARACTER SET = " + tableOptionWord(options.getCharset(), "character set"));
            }
            if (options.getCollate() != null) {
                clauses.add("COLLATE = " + tableOptionWord(options.getCollate(), "collation"));
            }
            if (options.getComment() != null) {
                clauses.add("COMMENT = " + sqlLiteral(options.getComment()));
            }
        }
        if (clauses.isEmpty()) {
            throw new IllegalArgumentException("No table option to change");
        }
        return "ALTER TABLE " + sqlIdentifier(database) + "." + sqlIdentifier(table) + "\n    " + String.join("\n    ", clauses);
    }

    /** Engines, row formats, character sets and collations are bare words; anything else is refused. */
    default String tableOptionWord(String value, String option)
    {
        String word = value.trim();
        if (!word.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid " + option + ": " + value);
        }
        return word;
    }

    default Response getPrimaryKeys(RuntimeConfigure configure, String database, String table)
    {
        String sql = "SELECT \n" +
                "    COLUMN_NAME as object_name,\n" +
                "    CONCAT('PRIMARY KEY on (',\n" +
                "        GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION),\n" +
                "        ')'\n" +
                "    ) as object_definition\n" +
                "FROM \n" +
                "    information_schema.KEY_COLUMN_USAGE\n" +
                "WHERE \n" +
                "    TABLE_SCHEMA = '{0}'\n" +
                "    AND TABLE_NAME = '{1}'\n" +
                "    AND CONSTRAINT_NAME = 'PRIMARY'\n" +
                "GROUP BY \n" +
                "    TABLE_SCHEMA, TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME;";
        return this.execute(
                configure,
                sql.replace("{0}", database)
                        .replace("{1}", table)
        );
    }

    /**
     * 根据数据库获取数据库信息
     * Get database information by database
     *
     * @param configure 配置信息 | Configuration information
     * @param database 数据库 | Database
     * @return 数据库信息 | Database information
     */
    default Response getDatabase(RuntimeConfigure configure, String database)
    {
        String schema = sqlLiteral(database);
        // A LEFT JOIN keeps the schema row for a database without tables; counts cover base tables only.
        String sql = "SELECT\n"
                + "    s.SCHEMA_NAME AS object_name,\n"
                + "    s.DEFAULT_CHARACTER_SET_NAME AS object_charset,\n"
                + "    s.DEFAULT_COLLATION_NAME AS object_collation,\n"
                + "    t.object_create_time,\n"
                + "    t.object_update_time,\n"
                + "    ROUND(IFNULL(t.data_length, 0) / 1024 / 1024, 2) AS object_data_size,\n"
                + "    ROUND(IFNULL(t.index_length, 0) / 1024 / 1024, 2) AS object_index_size,\n"
                + "    ROUND(IFNULL(t.data_length + t.index_length, 0) / 1024 / 1024, 2) AS object_total_size,\n"
                + "    IFNULL(t.data_length, 0) AS object_data_length,\n"
                + "    IFNULL(t.index_length, 0) AS object_index_length,\n"
                + "    IFNULL(t.data_free, 0) AS object_data_free,\n"
                + "    IFNULL(t.table_count, 0) AS object_table_count,\n"
                + "    IFNULL(t.total_rows, 0) AS object_total_rows,\n"
                + "    (SELECT COUNT(*) FROM information_schema.COLUMNS c JOIN information_schema.TABLES b\n"
                + "        ON b.TABLE_SCHEMA = c.TABLE_SCHEMA AND b.TABLE_NAME = c.TABLE_NAME AND b.TABLE_TYPE = 'BASE TABLE'\n"
                + "        WHERE c.TABLE_SCHEMA = " + schema + ") AS object_column_count,\n"
                + "    (SELECT COUNT(*) FROM (SELECT DISTINCT TABLE_NAME, INDEX_NAME FROM information_schema.STATISTICS\n"
                + "        WHERE TABLE_SCHEMA = " + schema + ") i) AS object_index_count,\n"
                + "    (SELECT COUNT(*) FROM information_schema.VIEWS WHERE TABLE_SCHEMA = " + schema + ") AS object_view_count,\n"
                + "    (SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = " + schema + " AND ROUTINE_TYPE = 'PROCEDURE') AS object_procedure_count,\n"
                + "    (SELECT COUNT(*) FROM information_schema.EVENTS WHERE EVENT_SCHEMA = " + schema + ") AS object_event_count,\n"
                + "    (SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = " + schema + ") AS object_trigger_count,\n"
                + "    (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = " + schema + ") AS object_foreign_key_count\n"
                + "FROM information_schema.SCHEMATA s\n"
                + "LEFT JOIN (\n"
                + "    SELECT TABLE_SCHEMA, MIN(CREATE_TIME) AS object_create_time, MAX(UPDATE_TIME) AS object_update_time,\n"
                + "        SUM(DATA_LENGTH) AS data_length, SUM(INDEX_LENGTH) AS index_length, SUM(DATA_FREE) AS data_free,\n"
                + "        COUNT(*) AS table_count, SUM(TABLE_ROWS) AS total_rows\n"
                + "    FROM information_schema.TABLES\n"
                + "    WHERE TABLE_SCHEMA = " + schema + " AND TABLE_TYPE = 'BASE TABLE'\n"
                + "    GROUP BY TABLE_SCHEMA\n"
                + ") t ON t.TABLE_SCHEMA = s.SCHEMA_NAME\n"
                + "WHERE s.SCHEMA_NAME = " + schema;
        return this.execute(configure, sql);
    }

    /**
     * 数据库表概览 | Base tables of the database with engine, estimated rows, data and index sizes in
     * bytes, creation and update times and comment, by name.
     */
    default Response getDatabaseTables(RuntimeConfigure configure, String database)
    {
        String sql = "SELECT TABLE_NAME AS object_name, ENGINE AS object_engine, TABLE_ROWS AS object_rows,\n"
                + "    DATA_LENGTH AS object_data_length, INDEX_LENGTH AS object_index_length,\n"
                + "    CREATE_TIME AS object_create_time, UPDATE_TIME AS object_update_time, TABLE_COMMENT AS object_comment\n"
                + "FROM information_schema.TABLES\n"
                + "WHERE TABLE_SCHEMA = " + sqlLiteral(database) + " AND TABLE_TYPE = 'BASE TABLE'\n"
                + "ORDER BY TABLE_NAME";
        return this.execute(configure, sql);
    }

    /**
     * 修改数据库默认字符集与排序规则 | Changes the database's default character set and collation in
     * one ALTER DATABASE; tables keep theirs, only new tables follow the new default.
     */
    default Response alterDatabaseOptions(RuntimeConfigure configure, TableDefinition definition)
    {
        return this.executeStatements(Lists.newArrayList(this.databaseOptionsStatement(definition.getDatabase(), definition.getOptions())), configure, definition);
    }

    default String databaseOptionsStatement(String database, TableOptionsDefinition options)
    {
        List<String> clauses = Lists.newArrayList();
        if (options != null && options.getCharset() != null) {
            clauses.add("DEFAULT CHARACTER SET = " + tableOptionWord(options.getCharset(), "character set"));
        }
        if (options != null && options.getCollate() != null) {
            clauses.add("DEFAULT COLLATE = " + tableOptionWord(options.getCollate(), "collation"));
        }
        if (clauses.isEmpty()) {
            throw new IllegalArgumentException("Nothing to change");
        }
        return "ALTER DATABASE " + sqlIdentifier(database) + " " + String.join(" ", clauses);
    }

    /**
     * 根据表获取表信息
     * Get table information by table data
     *
     * @param configure 配置信息 | Configuration information
     * @param database 数据库 | Database
     * @param table 表 | Table
     * @return 表信息 | Table information
     */
    default Response getTable(RuntimeConfigure configure, String database, String table)
    {
        String sql = "WITH object_info AS (\n" +
                "    -- 获取表和视图类型\n" +
                "    SELECT\n" +
                "        TABLE_NAME as name,\n" +
                "        CASE\n" +
                "            WHEN TABLE_TYPE = 'BASE TABLE' THEN 'table'\n" +
                "            WHEN TABLE_TYPE = 'VIEW' THEN 'view'  \n" +
                "        END as type\n" +
                "    FROM information_schema.TABLES\n" +
                "    WHERE TABLE_SCHEMA = '{0}'\n" +
                "        AND TABLE_NAME = '{1}'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 获取函数和存储过程类型\n" +
                "    SELECT\n" +
                "        ROUTINE_NAME as name,\n" +
                "        LOWER(ROUTINE_TYPE) as type\n" +
                "    FROM information_schema.ROUTINES\n" +
                "    WHERE ROUTINE_SCHEMA = '{0}'\n" +
                "        AND ROUTINE_NAME = '{1}'\n" +
                ")\n" +
                "SELECT d.*\n" +
                "FROM object_info i\n" +
                "LEFT JOIN (\n" +
                "    -- 表的详细信息\n" +
                "    SELECT\n" +
                "        'table' as object_type,\n" +
                "        t.TABLE_NAME as object_name,\n" +
                "        t.ENGINE as object_engine,\n" +
                "        t.TABLE_COLLATION as object_collation,\n" +
                "        t.TABLE_COMMENT as object_comment,\n" +
                "        t.CREATE_TIME as object_create_time,\n" +
                "        t.UPDATE_TIME as object_update_time,\n" +
                "        ROUND(t.DATA_LENGTH/1024/1024, 2) as object_data_size,\n" +
                "        ROUND(t.INDEX_LENGTH/1024/1024, 2) as object_index_size,\n" +
                "        t.TABLE_ROWS as object_rows,\n" +
                "        (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = t.TABLE_SCHEMA AND TABLE_NAME = t.TABLE_NAME) as object_column_count,\n" +
                "        (SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = t.TABLE_SCHEMA AND TABLE_NAME = t.TABLE_NAME) as object_index_count,\n" +
                "        'table' as type_name,\n" +
                "        t.ROW_FORMAT as object_format,\n" +
                "        t.AVG_ROW_LENGTH as object_avg_row_length,\n" +
                "        t.AUTO_INCREMENT as object_auto_increment\n" +
                "    FROM information_schema.TABLES t\n" +
                "    WHERE t.TABLE_SCHEMA = '{0}'\n" +
                "        AND t.TABLE_NAME = '{1}'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 视图的详细信息\n" +
                "    SELECT\n" +
                "        'view' as object_type,\n" +
                "        TABLE_NAME as object_name,\n" +
                "        'view' as object_engine,\n" +
                "        '' as object_collation,\n" +
                "        '' as object_comment,\n" +
                "        NULL as object_create_time,\n" +
                "        NULL as object_update_time,\n" +
                "        0 as object_data_size,\n" +
                "        0 as object_index_size,\n" +
                "        0 as object_rows,\n" +
                "        (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = v.TABLE_SCHEMA AND TABLE_NAME = v.TABLE_NAME) as object_column_count,\n" +
                "        0 as object_index_count,\n" +
                "        'view' as type_name,\n" +
                "        NULL as object_format,\n" +
                "        0 as object_avg_row_length,\n" +
                "        NULL as object_auto_increment\n" +
                "    FROM information_schema.VIEWS v\n" +
                "    WHERE v.TABLE_SCHEMA = '{0}'\n" +
                "        AND v.TABLE_NAME = '{1}'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 函数或存储过程的详细信息\n" +
                "    SELECT\n" +
                "        LOWER(r.ROUTINE_TYPE) as object_type,\n" +
                "        r.ROUTINE_NAME as object_name,\n" +
                "        LOWER(r.ROUTINE_TYPE) as object_engine,\n" +
                "        '' as object_collation,\n" +
                "        '' as object_comment,\n" +
                "        r.CREATED as object_create_time,\n" +
                "        r.LAST_ALTERED as object_update_time,\n" +
                "        0 as object_data_size,\n" +
                "        0 as object_index_size,\n" +
                "        0 as object_rows,\n" +
                "        0 as object_column_count,\n" +
                "        0 as object_index_count,\n" +
                "        LOWER(r.ROUTINE_TYPE) as type_name,\n" +
                "        NULL as object_format,\n" +
                "        0 as object_avg_row_length,\n" +
                "        NULL as object_auto_increment\n" +
                "    FROM information_schema.ROUTINES r\n" +
                "    WHERE r.ROUTINE_SCHEMA = '{0}'\n" +
                "        AND r.ROUTINE_NAME = '{1}'\n" +
                ") d ON i.type = d.type_name;";

        return this.execute(
                configure,
                sql.replace("{0}", database)
                        .replace("{1}", table)
        );
    }

    default Response getTableStatement(RuntimeConfigure configure, TableDefinition definition)
    {
        String sql = "WITH index_list AS (\n" +
                "    SELECT\n" +
                "        INDEX_NAME,\n" +
                "        GROUP_CONCAT(\n" +
                "            CONCAT('`', COLUMN_NAME, '`',\n" +
                "                # 处理索引长度前缀\n" +
                "                CASE WHEN SUB_PART IS NOT NULL\n" +
                "                    THEN CONCAT('(', SUB_PART, ')')\n" +
                "                    ELSE ''\n" +
                "                END\n" +
                "            )\n" +
                "            ORDER BY SEQ_IN_INDEX\n" +
                "            SEPARATOR ', '\n" +
                "        ) as index_columns,\n" +
                "        NON_UNIQUE,\n" +
                "        INDEX_TYPE,  # 添加索引类型(BTREE, HASH等)\n" +
                "        INDEX_COMMENT # 添加索引注释\n" +
                "    FROM information_schema.STATISTICS\n" +
                "    WHERE TABLE_SCHEMA = '{0}'\n" +
                "    AND TABLE_NAME = '{1}'\n" +
                "    GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_TYPE, INDEX_COMMENT\n" +
                ")\n" +
                "SELECT CONCAT(\n" +
                "    'CREATE TABLE `',\n" +
                "    t.TABLE_NAME,\n" +
                "    '` (\\n',\n" +
                "    # 列定义\n" +
                "    GROUP_CONCAT(\n" +
                "        CONCAT('  `', c.COLUMN_NAME, '` ',\n" +
                "            c.COLUMN_TYPE,\n" +
                "            # 字符集\n" +
                "            CASE WHEN c.CHARACTER_SET_NAME IS NOT NULL\n" +
                "                THEN CONCAT(' CHARACTER SET ', c.CHARACTER_SET_NAME)\n" +
                "                ELSE ''\n" +
                "            END,\n" +
                "            # 排序规则\n" +
                "            CASE WHEN c.COLLATION_NAME IS NOT NULL\n" +
                "                THEN CONCAT(' COLLATE ', c.COLLATION_NAME)\n" +
                "                ELSE ''\n" +
                "            END,\n" +
                "            CASE WHEN c.IS_NULLABLE = 'NO' THEN ' NOT NULL' ELSE '' END,\n" +
                "            CASE\n" +
                "                WHEN c.COLUMN_DEFAULT IS NOT NULL THEN\n" +
                "                    CASE\n" +
                "                        # 特殊处理 CURRENT_TIMESTAMP\n" +
                "                        WHEN c.COLUMN_DEFAULT = 'CURRENT_TIMESTAMP' THEN ' DEFAULT CURRENT_TIMESTAMP'\n" +
                "                        # 处理函数和表达式默认值\n" +
                "                        WHEN c.COLUMN_DEFAULT REGEXP '^[A-Za-z]' THEN CONCAT(' DEFAULT ', c.COLUMN_DEFAULT)\n" +
                "                        ELSE CONCAT(' DEFAULT \\'', c.COLUMN_DEFAULT, '\\'')\n" +
                "                    END\n" +
                "                ELSE ''\n" +
                "            END,\n" +
                "            CASE WHEN c.EXTRA != '' THEN CONCAT(' ', c.EXTRA) ELSE '' END,\n" +
                "            CASE WHEN c.COLUMN_COMMENT != '' THEN CONCAT(' COMMENT \\'', REPLACE(c.COLUMN_COMMENT, '\\'', '\\\\\\''), '\\'') ELSE '' END\n" +
                "        )\n" +
                "        ORDER BY c.ORDINAL_POSITION\n" +
                "        SEPARATOR ',\\n'\n" +
                "    ),\n" +
                "    # 索引定义\n" +
                "    IFNULL(CONCAT(',\\n', (\n" +
                "        SELECT GROUP_CONCAT(\n" +
                "            CASE\n" +
                "                WHEN INDEX_NAME = 'PRIMARY' THEN\n" +
                "                    CONCAT('  PRIMARY KEY ', index_columns)\n" +
                "                ELSE\n" +
                "                    CONCAT(\n" +
                "                        CASE WHEN NON_UNIQUE = 0 THEN '  UNIQUE ' ELSE '  ' END,\n" +
                "                        'KEY `', INDEX_NAME, '` ',\n" +
                "                        # 添加索引类型\n" +
                "                        CASE WHEN INDEX_TYPE != 'BTREE' THEN CONCAT('USING ', INDEX_TYPE, ' ') ELSE '' END,\n" +
                "                        '(', index_columns, ')',\n" +
                "                        # 添加索引注释\n" +
                "                        CASE WHEN INDEX_COMMENT != ''\n" +
                "                            THEN CONCAT(' COMMENT \\'', REPLACE(INDEX_COMMENT, '\\'', '\\\\\\''), '\\'')\n" +
                "                            ELSE ''\n" +
                "                        END\n" +
                "                    )\n" +
                "            END\n" +
                "            SEPARATOR ',\\n'\n" +
                "        )\n" +
                "        FROM index_list\n" +
                "    )), ''),\n" +
                "    '\\n) ENGINE=', t.ENGINE,\n" +
                "    CASE WHEN t.ROW_FORMAT IS NOT NULL THEN CONCAT(' ROW_FORMAT=', t.ROW_FORMAT) ELSE '' END,\n" +
                "    ' DEFAULT CHARSET=', t.TABLE_COLLATION,\n" +
                "    CASE WHEN t.TABLE_COMMENT != ''\n" +
                "        THEN CONCAT(' COMMENT=\\'', REPLACE(t.TABLE_COMMENT, '\\'', '\\\\\\''), '\\'')\n" +
                "        ELSE ''\n" +
                "    END,\n" +
                "    ';'\n" +
                ") as create_table_sql\n" +
                "FROM\n" +
                "    information_schema.TABLES t\n" +
                "    JOIN information_schema.COLUMNS c ON c.TABLE_SCHEMA = t.TABLE_SCHEMA AND c.TABLE_NAME = t.TABLE_NAME\n" +
                "WHERE\n" +
                "    t.TABLE_SCHEMA = '{0}'\n" +
                "    AND t.TABLE_NAME = '{1}'\n" +
                "GROUP BY\n" +
                "    t.TABLE_SCHEMA,\n" +
                "    t.TABLE_NAME,\n" +
                "    t.ENGINE,\n" +
                "    t.ROW_FORMAT,\n" +
                "    t.TABLE_COLLATION,\n" +
                "    t.TABLE_COMMENT;";

        return this.getResponse(
                sql.replace("{0}", definition.getDatabase())
                        .replace("{1}", definition.getName()),
                configure,
                definition
        );
    }

    /**
     * 生成表 DDL | Builds one kind of DDL for the table from SHOW CREATE TABLE: create (as printed),
     * structure (columns and options only), indexes, constraints, alter (one MODIFY per column),
     * alter-table (one statement restating every column and option) or sample-data (INSERT
     * statements for the first rows). Nothing is changed; the content holds the generated SQL.
     */
    default Response getTableDdl(RuntimeConfigure configure, String database, String table, String kind)
    {
        String target = sqlIdentifier(database) + "." + sqlIdentifier(table);
        Response shown = this.execute(configure, "SHOW CREATE TABLE " + target);
        if (shown == null || !Boolean.TRUE.equals(shown.getIsSuccessful())) {
            return shown;
        }
        String create = shown.getColumns() == null || shown.getColumns().isEmpty() || !(shown.getColumns().get(0) instanceof ObjectNode)
                ? null : ((ObjectNode) shown.getColumns().get(0)).path("Create Table").asText(null);
        if (create == null) {
            throw new IllegalArgumentException("Only tables have a DDL statement");
        }
        TableDdl ddl = TableDdl.parse(create);
        String sql;
        switch (kind == null ? "" : kind) {
            case "create":
                sql = ddl.create();
                break;
            case "structure":
                sql = ddl.structure();
                break;
            case "indexes":
                sql = ddl.indexes(target);
                break;
            case "constraints":
                sql = ddl.constraints(target);
                break;
            case "alter":
                sql = ddl.alterColumns(target);
                break;
            case "alter-table":
                sql = ddl.alterTable(target);
                break;
            case "sample-data":
                return this.sampleInserts(configure, target, ddl.insertableColumns());
            default:
                throw new IllegalArgumentException("Unknown DDL kind: " + kind);
        }
        return Response.builder().isSuccessful(true).isConnected(true).headers(Lists.newArrayList()).columns(Lists.newArrayList())
                .types(Lists.newArrayList()).content(sql).build();
    }

    /** One multi-row INSERT for the first ten rows of the table; empty when the table has none. */
    default Response sampleInserts(RuntimeConfigure configure, String target, List<String> columns)
    {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("The table has no insertable columns");
        }
        String select = "SELECT " + columns.stream().map(this::sqlIdentifier).collect(Collectors.joining(", ")) + " FROM " + target + " LIMIT 10";
        Response rows = this.execute(configure, select);
        if (rows == null || !Boolean.TRUE.equals(rows.getIsSuccessful())) {
            return rows;
        }
        ObjectMapper mapper = new ObjectMapper();
        List<String> values = Lists.newArrayList();
        for (Object row : rows.getColumns() == null ? Lists.newArrayList() : rows.getColumns()) {
            JsonNode node = row instanceof JsonNode ? (JsonNode) row : mapper.valueToTree(row);
            values.add("(" + columns.stream().map(column -> this.sqlValue(node.get(column))).collect(Collectors.joining(", ")) + ")");
        }
        String sql = values.isEmpty() ? "" : "INSERT INTO " + target + " (" + columns.stream().map(this::sqlIdentifier).collect(Collectors.joining(", ")) + ") VALUES\n  "
                + String.join(",\n  ", values) + ";";
        return Response.builder().isSuccessful(true).isConnected(true).headers(Lists.newArrayList()).columns(Lists.newArrayList())
                .types(Lists.newArrayList()).content(sql).build();
    }

    default String sqlValue(JsonNode value)
    {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return "NULL";
        }
        if (value.isNumber()) {
            return value.asText();
        }
        if (value.isBoolean()) {
            return value.asBoolean() ? "1" : "0";
        }
        return sqlLiteral(value.isValueNode() ? value.asText() : value.toString());
    }

    default Response getSuggests(RuntimeConfigure configure, String keyword)
    {
        String sql = "(\n" +
                "    -- 数据库\n" +
                "    SELECT\n" +
                "        SCHEMA_NAME as object_name,\n" +
                "        'Database' as object_type,\n" +
                "        'common.database' as object_key\n" +
                "    FROM information_schema.SCHEMATA\n" +
                "    WHERE SCHEMA_NAME LIKE '%{0}%'\n" +
                ")\n" +
                "UNION ALL\n" +
                "(\n" +
                "    -- 表和视图\n" +
                "    SELECT\n" +
                "        TABLE_NAME as object_name,\n" +
                "        CASE TABLE_TYPE\n" +
                "            WHEN 'BASE TABLE' THEN 'Table'\n" +
                "            WHEN 'VIEW' THEN 'View'\n" +
                "            ELSE 'Table'\n" +
                "        END as object_type,\n" +
                "        CASE TABLE_TYPE\n" +
                "            WHEN 'BASE TABLE' THEN 'common.table'\n" +
                "            WHEN 'VIEW' THEN 'common.view'\n" +
                "            ELSE 'common.table'\n" +
                "        END as object_key\n" +
                "    FROM information_schema.TABLES\n" +
                "    WHERE TABLE_NAME LIKE '%{0}%'\n" +
                ")\n" +
                "UNION ALL\n" +
                "(\n" +
                "    -- 列（包含主键、索引信息）\n" +
                "    SELECT DISTINCT\n" +
                "        COLUMN_NAME as object_name,\n" +
                "        CASE\n" +
                "            WHEN COLUMN_KEY = 'PRI' THEN 'Key'\n" +
                "            WHEN COLUMN_KEY = 'UNI' THEN 'Signature'\n" +
                "            WHEN COLUMN_KEY = 'MUL' THEN 'Hash'\n" +
                "            ELSE 'Columns2'\n" +
                "        END as object_type,\n" +
                "        CASE\n" +
                "            WHEN COLUMN_KEY = 'PRI' THEN 'common.primary'\n" +
                "            WHEN COLUMN_KEY = 'UNI' THEN 'common.unique'\n" +
                "            WHEN COLUMN_KEY = 'MUL' THEN 'common.index'\n" +
                "            ELSE 'common.column'\n" +
                "        END as object_key\n" +
                "    FROM information_schema.COLUMNS\n" +
                "    WHERE COLUMN_NAME LIKE '%{0}%'\n" +
                ")\n" +
                "UNION ALL\n" +
                "(\n" +
                "    -- 触发器\n" +
                "    SELECT\n" +
                "        TRIGGER_NAME as object_name,\n" +
                "        'Pi' as object_type,\n" +
                "        'common.trigger' as object_key\n" +
                "    FROM information_schema.TRIGGERS\n" +
                "    WHERE TRIGGER_NAME LIKE '%{0}%'\n" +
                ")\n" +
                "UNION ALL\n" +
                "(\n" +
                "    -- 存储过程\n" +
                "    SELECT\n" +
                "        ROUTINE_NAME as object_name,\n" +
                "        'Microchip' as object_type,\n" +
                "        'common.procedure' as object_key\n" +
                "    FROM information_schema.ROUTINES\n" +
                "    WHERE ROUTINE_TYPE = 'PROCEDURE'\n" +
                "        AND ROUTINE_NAME LIKE '%{0}%'\n" +
                ")\n" +
                "UNION ALL\n" +
                "(\n" +
                "    -- 函数\n" +
                "    SELECT\n" +
                "        ROUTINE_NAME as object_name,\n" +
                "        'SquareFunction' as object_type,\n" +
                "        'common.function' as object_key\n" +
                "    FROM information_schema.ROUTINES\n" +
                "    WHERE ROUTINE_TYPE = 'FUNCTION'\n" +
                "        AND ROUTINE_NAME LIKE '%{0}%'\n" +
                ")\n" +
                "ORDER BY\n" +
                "    CASE object_type\n" +
                "        WHEN 'Database' THEN 1\n" +
                "        WHEN 'Table' THEN 2\n" +
                "        WHEN 'View' THEN 3\n" +
                "        WHEN 'Key' THEN 4\n" +
                "        WHEN 'Signature' THEN 5\n" +
                "        WHEN 'Hash' THEN 6\n" +
                "        WHEN 'Columns2' THEN 7\n" +
                "        WHEN 'Pi' THEN 8\n" +
                "        WHEN 'Microchip' THEN 9\n" +
                "        WHEN 'SquareFunction' THEN 10\n" +
                "        ELSE 11\n" +
                "    END,\n" +
                "    object_name;";

        return this.execute(configure, sql.replace("{0}", keyword));
    }

    /**
     * 更新表的自增值
     * Update table auto increment value
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response changeAutoIncrement(RuntimeConfigure configure, TableDefinition definition)
    {
        String sql = AlterTable.create(definition.getDatabase(), definition.getName())
                .autoIncrement(definition.getAutoIncrement())
                .build();

        return this.getResponse(sql, configure, definition);
    }

    /**
     * 新建表
     * Create table
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response createTable(RuntimeConfigure configure, TableDefinition definition)
    {
        AbstractTable tableDefinition = buildCreateTable(definition);
        return this.getResponse(tableDefinition.build(), configure, definition);
    }

    /** Builds the CREATE TABLE statement for a definition without executing or recording anything. */
    default Response previewCreateTable(RuntimeConfigure configure, TableDefinition definition, String kind)
    {
        CreateTable tableDefinition = buildCreateTable(definition);
        Response response = new Response();
        response.setIsSuccessful(Boolean.TRUE);
        response.setContent(tableDefinition.build(kind == null || kind.isBlank() ? "full" : kind));
        return response;
    }

    private CreateTable buildCreateTable(TableDefinition definition)
    {
        CreateTable tableDefinition = CreateTable.create(definition.getDatabase(), definition.getName());
        tableDefinition.comment(definition.getComment());
        tableDefinition.engine(definition.getEngine());
        tableDefinition.rowFormat(definition.getFormat());

        if (definition.getAutoIncrement() > 0) {
            tableDefinition.autoIncrement(definition.getAutoIncrement());
        }

        if (StringUtils.isNotEmpty(definition.getCharset())) {
            tableDefinition.charset(definition.getCharset());
        }

        if (StringUtils.isNotEmpty(definition.getCollate())) {
            tableDefinition.collate(definition.getCollate());
        }

        if (StringUtils.isNotEmpty(definition.getDataDirectory())) {
            tableDefinition.dataDirectory(definition.getDataDirectory());
        }

        definition.getColumns().forEach(col -> {
            if (col.isPrimaryKey()) {
                tableDefinition.addPrimaryKey(col.getName());
            }

            CreateColumn column = this.getCreateColumn(col);

            tableDefinition.addColumn(column);
        });

        definition.getIndexes()
                .forEach(index -> {
                    Index builder = Index.create(index.getName(), index.getColumns().toArray(new String[0]));
                    if (index.isUnique()) {
                        builder.unique();
                    }
                    tableDefinition.addIndex(builder);
                });

        return tableDefinition;
    }

    /**
     * 删除表
     * Delete table
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response dropTable(RuntimeConfigure configure, TableDefinition definition)
    {
        DropTable tableDefinition = DropTable.create(definition.getDatabase(), definition.getName())
                .ifExists();

        return this.getResponse(tableDefinition.build(), configure, definition);
    }

    /**
     * 截断表
     * Truncate table
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response truncateTable(RuntimeConfigure configure, TableDefinition definition)
    {
        TruncateTable tableDefinition = TruncateTable.create(definition.getDatabase(), definition.getName());

        return this.getResponse(tableDefinition.build(), configure, definition);
    }

    /**
     * 查询表
     * Query table
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response queryTable(RuntimeConfigure configure, TableDefinition definition)
    {
        SelectTable tableDefinition = SelectTable.create(definition.getDatabase(), definition.getName());
        // An unpaginated preview generates the full dataset SQL without executing it.
        if (definition.getPagination() != null) {
            tableDefinition.limit(definition.getPagination().getSize())
                    .offset((long) (definition.getPagination().getPage() - 1) * definition.getPagination().getSize());
        }
        else if (!definition.isPreview()) {
            throw new IllegalArgumentException("Pagination is required when querying table data");
        }

        // 添加查询列
        definition.getColumns().forEach(col -> tableDefinition.addColumn(SelectColumn.create(col.getName(), DataType.VARCHAR)));

        // 添加排序
        definition.getOrders().forEach(order -> tableDefinition.addOrderBy(OrderBy.create(order.getName(), order.getOrder())));

        // 添加筛选
        definition.getFilters().forEach(filter -> tableDefinition.addFilter(Filter.create(filter.getName(), filter.getOperator(), filter.getValue())));

        if (definition.isPreview()) {
            return this.getResponse(tableDefinition.build(), configure, definition);
        }
        return this.getResponse(
                tableDefinition.build(),
                configure,
                definition,
                this.getPagination(configure, definition)
        );
    }

    /**
     * 删除表数据
     * Delete table data
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response deleteData(RuntimeConfigure configure, TableDefinition definition)
    {
        Set<String> set = Sets.newHashSet();

        definition.getColumns().forEach(col -> {
            set.add(DeleteTable.create(definition.getDatabase(), definition.getName())
                    .addFilters(col.getOriginal())
                    .build());
        });

        return this.getResponse(
                String.join("\n", set),
                configure,
                definition
        );
    }

    /**
     * 创建列
     * Create column
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response createColumn(RuntimeConfigure configure, TableDefinition definition)
    {
        AlterTable tableDefinition = AlterTable.create(definition.getDatabase(), definition.getName());

        definition.getColumns().forEach(col -> tableDefinition.addColumn(getCreateColumn(col)));

        return this.getResponse(
                tableDefinition.build(),
                configure,
                definition
        );
    }

    /**
     * 删除列
     * Drop column
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response dropColumn(RuntimeConfigure configure, TableDefinition definition)
    {
        if (definition.isForce()) {
            return this.executeStatements(this.forceDropColumnStatements(configure, definition), configure, definition);
        }
        AlterTable tableDefinition = AlterTable.create(definition.getDatabase(), definition.getName());

        definition.getColumns().forEach(col -> tableDefinition.dropColumn(col.getName()));

        return this.getResponse(
                tableDefinition.build(),
                configure,
                definition
        );
    }

    /**
     * 表依赖对象 | Objects that depend on the table, checked before dropping it: foreign keys of other
     * tables referencing it (incoming), views naming it, stored procedures and functions of the
     * same schema mentioning it, and its own triggers, which are dropped with it. Routine bodies are
     * matched by name, so a routine listed may only mention the table. Each row has object_type
     * (foreign_key, view, routine or trigger), object_schema, object_name and object_detail.
     */
    default Response getTableDependencies(RuntimeConfigure configure, String database, String table)
    {
        String schema = sqlLiteral(database);
        String name = sqlLiteral(table);
        List<String> queries = Lists.newArrayList(
                "SELECT 'foreign_key' AS object_type, TABLE_SCHEMA AS object_schema, CONSTRAINT_NAME AS object_name,\n"
                        + "    CONCAT(TABLE_NAME, ' (', GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ', '), ') -> (',\n"
                        + "        GROUP_CONCAT(REFERENCED_COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ', '), ')') AS object_detail\n"
                        + "FROM information_schema.KEY_COLUMN_USAGE\n"
                        + "WHERE REFERENCED_TABLE_SCHEMA = " + schema + " AND REFERENCED_TABLE_NAME = " + name + "\n"
                        + "    AND NOT (TABLE_SCHEMA = " + schema + " AND TABLE_NAME = " + name + ")\n"
                        + "GROUP BY TABLE_SCHEMA, TABLE_NAME, CONSTRAINT_NAME",
                "SELECT 'view' AS object_type, TABLE_SCHEMA AS object_schema, TABLE_NAME AS object_name, '' AS object_detail\n"
                        + "FROM information_schema.VIEWS\n"
                        + "WHERE LOCATE(" + sqlLiteral("`" + database + "`.`" + table + "`") + ", VIEW_DEFINITION) > 0",
                "SELECT 'routine' AS object_type, ROUTINE_SCHEMA AS object_schema, ROUTINE_NAME AS object_name, ROUTINE_TYPE AS object_detail\n"
                        + "FROM information_schema.ROUTINES\n"
                        + "WHERE ROUTINE_SCHEMA = " + schema + " AND LOCATE(LOWER(" + name + "), LOWER(ROUTINE_DEFINITION)) > 0",
                "SELECT 'trigger' AS object_type, TRIGGER_SCHEMA AS object_schema, TRIGGER_NAME AS object_name,\n"
                        + "    CONCAT(ACTION_TIMING, ' ', EVENT_MANIPULATION) AS object_detail\n"
                        + "FROM information_schema.TRIGGERS\n"
                        + "WHERE EVENT_OBJECT_SCHEMA = " + schema + " AND EVENT_OBJECT_TABLE = " + name);
        Response response = Response.builder()
                .isSuccessful(true)
                .isConnected(true)
                .headers(Lists.newArrayList("object_type", "object_schema", "object_name", "object_detail"))
                .types(Lists.newArrayList("String", "String", "String", "String"))
                .columns(Lists.newArrayList())
                .build();
        List<String> failures = Lists.newArrayList();
        for (String query : queries) {
            Response part = this.execute(configure, query);
            if (part != null && Boolean.TRUE.equals(part.getIsSuccessful()) && part.getColumns() != null) {
                response.getColumns().addAll(part.getColumns());
            }
            else if (part != null && part.getMessage() != null) {
                failures.add(part.getMessage());
            }
        }
        if (!failures.isEmpty()) {
            response.setMessage(String.join("; ", failures));
        }
        return response;
    }

    /**
     * Objects that depend on the given columns: indexes, foreign keys in both directions, triggers on
     * the table, views and generated columns that reference them. Each kind is looked up on its own,
     * so a catalog view the account cannot read only hides that kind.
     * Rows: object_type (index, foreign_key, trigger, view, other), object_name, object_kind,
     * object_columns, object_schema, object_table, object_detail.
     */
    default Response getColumnDependencies(RuntimeConfigure configure, TableDefinition definition)
    {
        String database = sqlLiteral(definition.getDatabase());
        String table = sqlLiteral(definition.getName());
        List<String> names = definition.getColumns().stream().map(ColumnDefinition::getName).collect(Collectors.toList());
        String columns = names.stream().map(this::sqlLiteral).collect(Collectors.joining(", "));
        // Backtick-quoted names as MySQL writes them into trigger, view and generation expressions.
        java.util.function.Function<String, String> mentions = expression -> names.stream()
                .map(name -> "LOCATE(" + sqlLiteral("`" + name + "`") + ", " + expression + ") > 0")
                .collect(Collectors.joining(" OR ", "(", ")"));
        String select = "SELECT {type} AS object_type, {name} AS object_name, {kind} AS object_kind, {columns} AS object_columns,\n"
                + "    {schema} AS object_schema, {table} AS object_table, {detail} AS object_detail\n";
        List<String> queries = Lists.newArrayList(
                select.replace("{type}", "'index'").replace("{name}", "INDEX_NAME")
                        .replace("{kind}", "CASE WHEN INDEX_NAME = 'PRIMARY' THEN 'PRIMARY' WHEN MAX(NON_UNIQUE) = 0 THEN 'UNIQUE' "
                                + "WHEN MAX(INDEX_TYPE) = 'FULLTEXT' THEN 'FULLTEXT' ELSE 'NORMAL' END")
                        .replace("{columns}", "GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ')")
                        .replace("{schema}", "TABLE_SCHEMA").replace("{table}", "TABLE_NAME").replace("{detail}", "''")
                        + "FROM information_schema.STATISTICS\n"
                        + "WHERE TABLE_SCHEMA = " + database + " AND TABLE_NAME = " + table + "\n"
                        + "GROUP BY TABLE_SCHEMA, TABLE_NAME, INDEX_NAME\n"
                        + "HAVING SUM(COLUMN_NAME IN (" + columns + ")) > 0",
                select.replace("{type}", "'foreign_key'").replace("{name}", "CONSTRAINT_NAME")
                        .replace("{kind}", "IF(TABLE_SCHEMA = " + database + " AND TABLE_NAME = " + table + ", 'OUTGOING', 'INCOMING')")
                        .replace("{columns}", "GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ', ')")
                        .replace("{schema}", "TABLE_SCHEMA").replace("{table}", "TABLE_NAME")
                        .replace("{detail}", "CONCAT(REFERENCED_TABLE_SCHEMA, '.', REFERENCED_TABLE_NAME, ' (', "
                                + "GROUP_CONCAT(REFERENCED_COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ', '), ')')")
                        + "FROM information_schema.KEY_COLUMN_USAGE\n"
                        + "WHERE REFERENCED_TABLE_NAME IS NOT NULL AND (\n"
                        + "    (TABLE_SCHEMA = " + database + " AND TABLE_NAME = " + table + " AND COLUMN_NAME IN (" + columns + "))\n"
                        + "    OR (REFERENCED_TABLE_SCHEMA = " + database + " AND REFERENCED_TABLE_NAME = " + table
                        + " AND REFERENCED_COLUMN_NAME IN (" + columns + ")))\n"
                        + "GROUP BY TABLE_SCHEMA, TABLE_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_SCHEMA, REFERENCED_TABLE_NAME",
                select.replace("{type}", "'trigger'").replace("{name}", "TRIGGER_NAME")
                        .replace("{kind}", "CONCAT(ACTION_TIMING, ' ', EVENT_MANIPULATION)").replace("{columns}", "''")
                        .replace("{schema}", "TRIGGER_SCHEMA").replace("{table}", "EVENT_OBJECT_TABLE").replace("{detail}", "''")
                        + "FROM information_schema.TRIGGERS\n"
                        + "WHERE EVENT_OBJECT_SCHEMA = " + database + " AND EVENT_OBJECT_TABLE = " + table
                        + " AND " + mentions.apply("ACTION_STATEMENT"),
                select.replace("{type}", "'view'").replace("{name}", "TABLE_NAME").replace("{kind}", "'VIEW'").replace("{columns}", "''")
                        .replace("{schema}", "TABLE_SCHEMA").replace("{table}", "TABLE_NAME").replace("{detail}", "''")
                        + "FROM information_schema.VIEWS\n"
                        + "WHERE LOCATE(" + sqlLiteral("`" + definition.getDatabase() + "`.`" + definition.getName() + "`") + ", VIEW_DEFINITION) > 0"
                        + " AND " + mentions.apply("VIEW_DEFINITION"),
                select.replace("{type}", "'other'").replace("{name}", "COLUMN_NAME").replace("{kind}", "'GENERATED'").replace("{columns}", "COLUMN_NAME")
                        .replace("{schema}", "TABLE_SCHEMA").replace("{table}", "TABLE_NAME").replace("{detail}", "GENERATION_EXPRESSION")
                        + "FROM information_schema.COLUMNS\n"
                        + "WHERE TABLE_SCHEMA = " + database + " AND TABLE_NAME = " + table + " AND GENERATION_EXPRESSION <> ''"
                        + " AND COLUMN_NAME NOT IN (" + columns + ") AND " + mentions.apply("GENERATION_EXPRESSION"));

        Response response = Response.builder()
                .isSuccessful(true)
                .isConnected(true)
                .headers(Lists.newArrayList("object_type", "object_name", "object_kind", "object_columns", "object_schema", "object_table", "object_detail"))
                .types(Lists.newArrayList("String", "String", "String", "String", "String", "String", "String"))
                .columns(Lists.newArrayList())
                .build();
        List<String> failures = Lists.newArrayList();
        for (String query : queries) {
            Response part = this.execute(configure, query);
            if (part != null && Boolean.TRUE.equals(part.getIsSuccessful()) && part.getColumns() != null) {
                response.getColumns().addAll(part.getColumns());
            }
            else if (part != null && part.getMessage() != null) {
                failures.add(part.getMessage());
            }
        }
        if (!failures.isEmpty()) {
            response.setMessage(String.join("; ", failures));
        }
        response.setContent(String.join(";\n\n", queries) + ";");
        return response;
    }

    /**
     * Statements for a forced drop: foreign keys (on this table and on tables referencing it), triggers
     * and views go first, then one ALTER drops the dependent indexes, generated columns and the columns.
     */
    default List<String> forceDropColumnStatements(RuntimeConfigure configure, TableDefinition definition)
    {
        Response dependencies = this.getColumnDependencies(configure, definition);
        List<String> statements = Lists.newArrayList();
        List<String> alterations = Lists.newArrayList();
        for (Object item : dependencies.getColumns()) {
            if (!(item instanceof ObjectNode)) {
                continue;
            }
            ObjectNode node = (ObjectNode) item;
            String type = node.path("object_type").asText();
            String name = sqlIdentifier(node.path("object_name").asText());
            String owner = sqlIdentifier(node.path("object_schema").asText()) + "." + sqlIdentifier(node.path("object_table").asText());
            switch (type) {
                case "foreign_key":
                    statements.add("ALTER TABLE " + owner + " DROP FOREIGN KEY " + name);
                    break;
                case "trigger":
                    statements.add("DROP TRIGGER " + sqlIdentifier(node.path("object_schema").asText()) + "." + name);
                    break;
                case "view":
                    statements.add("DROP VIEW " + owner);
                    break;
                case "index":
                    alterations.add("PRIMARY".equals(node.path("object_name").asText()) ? "DROP PRIMARY KEY" : "DROP INDEX " + name);
                    break;
                case "other":
                    alterations.add("DROP COLUMN " + name);
                    break;
                default:
                    break;
            }
        }
        definition.getColumns().forEach(column -> alterations.add("DROP COLUMN " + sqlIdentifier(column.getName())));
        statements.add("ALTER TABLE " + sqlIdentifier(definition.getDatabase()) + "." + sqlIdentifier(definition.getName())
                + "\n    " + String.join(",\n    ", alterations));
        return statements;
    }

    /**
     * Runs statements one at a time and stops at the first failure; the response content lists every
     * statement that was run, the failing one included.
     */
    default Response executeStatements(List<String> statements, RuntimeConfigure configure, TableDefinition definition)
    {
        List<String> executed = Lists.newArrayList();
        Response response = null;
        for (String statement : statements) {
            executed.add(statement + ";");
            response = this.getResponse(statement + ";", configure, definition);
            if (!Boolean.TRUE.equals(response.getIsSuccessful())) {
                break;
            }
        }
        if (response != null) {
            response.setContent(String.join("\n", definition.isPreview() ? statements.stream().map(statement -> statement + ";").collect(Collectors.toList()) : executed));
        }
        return response;
    }

    default String sqlLiteral(String value)
    {
        return "'" + String.valueOf(value).replace("\\", "\\\\").replace("'", "''") + "'";
    }

    default String sqlIdentifier(String value)
    {
        return "`" + String.valueOf(value).replace("`", "``") + "`";
    }

    /**
     * 获取指定列
     * Get column
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response getColumn(RuntimeConfigure configure, TableDefinition definition)
    {
        Optional<ColumnDefinition> column = definition.getColumns().stream().findAny();

        if (column.isEmpty()) {
            throw new IllegalArgumentException("Column must be specified");
        }

        String sql = "SELECT \n" +
                "    detail.*\n" +
                "FROM (\n" +
                "    -- 列信息：优化了类型解析\n" +
                "    SELECT\n" +
                "        'column' as type_name,\n" +
                "        COLUMN_NAME as object_name,\n" +
                "        SUBSTRING_INDEX(COLUMN_TYPE, '(', 1) as object_data_type,  -- 更高效的类型提取\n" +
                "        NULLIF(\n" +
                "            SUBSTRING_INDEX(SUBSTRING_INDEX(COLUMN_TYPE, '(', -1), ')', 1),\n" +
                "            COLUMN_TYPE\n" +
                "        ) as object_length,  -- 更高效的长度提取\n" +
                "        IS_NULLABLE as object_nullable,\n" +
                "        COLUMN_DEFAULT as object_default_value,\n" +
                "        COLUMN_COMMENT as object_comment,\n" +
                "        ORDINAL_POSITION as object_position,\n" +
                "        '' as object_definition\n" +
                "    FROM\n" +
                "        information_schema.COLUMNS c\n" +
                "    WHERE\n" +
                "        TABLE_SCHEMA = '{0}'\n" +
                "        AND TABLE_NAME = '{1}'\n" +
                "        AND COLUMN_NAME = '{2}'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 主键信息：添加了索引以提升性能\n" +
                "    SELECT\n" +
                "        'primary' as type_name,\n" +
                "        COLUMN_NAME as object_name,\n" +
                "        '' as object_data_type,\n" +
                "        NULL as object_length,\n" +
                "        '' as object_nullable,\n" +
                "        '' as object_default_value,\n" +
                "        '' as object_comment,\n" +
                "        0 as object_position,\n" +
                "        CONCAT('PRIMARY KEY on (',\n" +
                "            GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION),\n" +
                "            ')'\n" +
                "        ) as object_definition\n" +
                "    FROM\n" +
                "        information_schema.KEY_COLUMN_USAGE\n" +
                "    WHERE\n" +
                "        TABLE_SCHEMA = '{0}'\n" +
                "        AND TABLE_NAME = '{1}'\n" +
                "        AND CONSTRAINT_NAME = 'PRIMARY'\n" +
                "        AND COLUMN_NAME = '{2}'\n" +
                "    GROUP BY\n" +
                "        TABLE_SCHEMA, TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 索引信息：优化了索引名称的处理\n" +
                "    SELECT DISTINCT\n" +
                "        'index' as type_name,\n" +
                "        COLUMN_NAME as object_name,\n" +
                "        '' as object_data_type,\n" +
                "        NULL as object_length,\n" +
                "        '' as object_nullable,\n" +
                "        '' as object_default_value,\n" +
                "        '' as object_comment,\n" +
                "        0 as object_position,\n" +
                "        CONCAT(\n" +
                "            CASE NON_UNIQUE\n" +
                "                WHEN 1 THEN 'Non-unique'\n" +
                "                ELSE 'Unique'\n" +
                "            END,\n" +
                "            ' index on (',\n" +
                "            GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX),\n" +
                "            ')'\n" +
                "        ) as object_definition\n" +
                "    FROM\n" +
                "        information_schema.STATISTICS\n" +
                "    WHERE\n" +
                "        TABLE_SCHEMA = '{0}'\n" +
                "        AND TABLE_NAME = '{1}'\n" +
                "        AND COLUMN_NAME = '{2}'\n" +
                "    GROUP BY\n" +
                "        TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, NON_UNIQUE\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 触发器信息：保持原有逻辑\n" +
                "    SELECT\n" +
                "        'trigger' as type_name,\n" +
                "        TRIGGER_NAME as object_name,\n" +
                "        '' as object_data_type,\n" +
                "        NULL as object_length,\n" +
                "        '' as object_nullable,\n" +
                "        '' as object_default_value,\n" +
                "        '' as object_comment,\n" +
                "        0 as object_position,\n" +
                "        CONCAT(\n" +
                "            'TRIGGER ',\n" +
                "            ACTION_TIMING, ' ',\n" +
                "            EVENT_MANIPULATION\n" +
                "        ) as object_definition\n" +
                "    FROM\n" +
                "        information_schema.TRIGGERS\n" +
                "    WHERE\n" +
                "        EVENT_OBJECT_SCHEMA = '{0}'\n" +
                "        AND EVENT_OBJECT_TABLE = '{1}'\n" +
                ") detail\n" +
                "ORDER BY\n" +
                "    FIELD(type_name, 'column', 'primary', 'index', 'trigger'),\n" +
                "    object_position,\n" +
                "    object_name;";

        return this.getResponse(
                sql.replace("{0}", definition.getDatabase())
                        .replace("{1}", definition.getName())
                        .replace("{2}", column.get().getName()),
                configure,
                definition
        );
    }

    /**
     * 修改列
     * Change column
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response changeColumn(RuntimeConfigure configure, TableDefinition definition)
    {
        AlterTable tableDefinition = AlterTable.create(definition.getDatabase(), definition.getName());

        definition.getColumns().forEach(col -> tableDefinition.modifyColumn(getCreateColumn(col)));

        return this.getResponse(
                tableDefinition.build(),
                configure,
                definition
        );
    }

    /**
     * 插入数据
     * Insert data
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response insertData(RuntimeConfigure configure, TableDefinition definition)
    {
        InsertTable tableDefinition = InsertTable.create(definition.getDatabase(), definition.getName())
                .addValues(definition.getRows())
                .addPrimaryKeys(this.formatPrimaryKeys(configure, definition.getDatabase(), definition.getName()));

        return this.getResponse(
                tableDefinition.build(),
                configure,
                definition
        );
    }

    /**
     * 更新数据
     * Update data
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response updateData(RuntimeConfigure configure, TableDefinition definition)
    {
        UpdateTable tableDefinition = UpdateTable.create(definition.getDatabase(), definition.getName())
                .addPrimaryKeys(this.formatPrimaryKeys(configure, definition.getDatabase(), definition.getName()));

        definition.getRows().forEach(row -> {
            tableDefinition.addUpdates(row.get("row"));
            tableDefinition.addFilters(row.get("original"));
        });

        return this.getResponse(
                tableDefinition.build(),
                configure,
                definition
        );
    }

    default Response getResponse(String sql, RuntimeConfigure configure, BaseDefinition definition)
    {
        Response response;
        if (definition.isPreview()) {
            response = Response.builder()
                    .isSuccessful(true)
                    .isConnected(true)
                    .headers(Lists.newArrayList())
                    .columns(Lists.newArrayList())
                    .types(Lists.newArrayList())
                    .content(sql)
                    .build();
        }
        else {
            response = this.execute(configure, sql);
            response.setContent(sql);
        }
        return response;
    }

    private Response getResponse(String sql, RuntimeConfigure configure, TableDefinition definition, Pagination pagination)
    {
        Response response = this.getResponse(sql, configure, definition);
        response.setPagination(pagination);
        return response;
    }

    default CreateColumn getCreateColumn(ColumnDefinition col)
    {
        CreateColumn column = CreateColumn.create(col.getName(), col.getType());

        column.comment(col.getComment())
                .defaultValue(col.getDefaultValue());

        if (col.getLength() != null) {
            column.length(col.getLength());
        }

        if (col.getPrecision() != null) {
            column.precision(col.getPrecision(), col.getScale() == null ? 0 : col.getScale());
        }

        if (StringUtils.isNotEmpty(col.getCharset())) {
            column.charset(col.getCharset());
        }

        if (StringUtils.isNotEmpty(col.getCollate())) {
            column.collate(col.getCollate());
        }

        if (StringUtils.isNotEmpty(col.getAfter())) {
            column.after(col.getAfter());
        }

        if (col.isFirst()) {
            column.first();
        }

        if (col.isAutoIncrement()) {
            column.autoIncrement();
        }

        // nullable=true keeps the column nullable; only explicit false adds NOT NULL
        if (!col.isNullable()) {
            column.notNull();
        }

        return column;
    }

    /**
     * 获取分页
     * Get pagination
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 分页 | Pagination
     */
    default Pagination getPagination(RuntimeConfigure configure, TableDefinition definition)
    {
        try {
            /*
             * {0} 数据库
             * {1} 数据表
             * {2} 页数
             * {3} 每页大小
             * {4} 筛选条件
             */
            String sql = "SELECT\n" +
                    "    {3} as object_size,\n" +
                    "    {2} as object_page,\n" +
                    "    total as object_total,\n" +
                    "    ceil(total / {3}) as object_total_pages,\n" +
                    "    if({2} > 1, 1, 0) as object_has_previous,\n" +
                    "    if(({3} * {2}) < total, 1, 0) as object_has_next,\n" +
                    "    ({2} - 1) * {3} + 1 as object_start_index,\n" +
                    "    least(({2} * {3}), total) as object_end_index\n" +
                    "FROM (\n" +
                    "    SELECT count(1) as total\n" +
                    "    FROM `{0}`.`{1}`{4}\n" +
                    ") t";

            return this.formatPagination(configure, sql, definition);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * The WHERE clause matching {@link #queryTable}, so the total counts the same rows the page shows.
     */
    default String formatFilterClause(TableDefinition definition)
    {
        if (definition.getFilters() == null || definition.getFilters().isEmpty()) {
            return "";
        }
        return "\n    WHERE " + definition.getFilters().stream()
                .map(filter -> Filter.create(filter.getName(), filter.getOperator(), filter.getValue()).build())
                .collect(Collectors.joining(" AND "));
    }

    default Pagination formatPagination(RuntimeConfigure configure, String sql, TableDefinition definition)
    {
        Response response = this.getResponse(
                sql.replace("{0}", definition.getDatabase())
                        .replace("{1}", definition.getName())
                        .replace("{2}", String.valueOf(definition.getPagination().getPage()))
                        .replace("{3}", String.valueOf(definition.getPagination().getSize()))
                        // Filter values are user input: substitute them last so they are never re-scanned for placeholders.
                        .replace("{4}", formatFilterClause(definition)),
                configure,
                definition
        );
        Preconditions.checkArgument(response.getIsSuccessful(), response.getMessage());

        ObjectNode node = (ObjectNode) response.getColumns().get(0);
        return Pagination.create(node.get("object_size").asInt(), node.get("object_page").asInt())
                .total(node.get("object_total").asInt())
                .pages(node.get("object_total_pages").asInt())
                .hasPrevious(node.get("object_has_previous").asInt() == 1)
                .hasNext(node.get("object_has_next").asInt() == 1)
                .startIndex(node.get("object_start_index").asInt())
                .endIndex(node.get("object_end_index").asInt());
    }

    default List<String> formatPrimaryKeys(RuntimeConfigure configure, String database, String table)
    {
        List<String> columns = Lists.newArrayList();

        Response primaryKeys = this.getPrimaryKeys(configure, database, table);
        if (primaryKeys.getIsSuccessful()) {
            List<String> keys = primaryKeys.getColumns()
                    .stream()
                    .map(col -> {
                        ObjectNode node = (ObjectNode) col;

                        return node.get("object_name").asText();
                    })
                    .collect(Collectors.toList());

            columns.addAll(keys);
        }

        return columns;
    }

    default void destroy()
    {
        Connection connection = local.get();
        if (connection != null) {
            connection.destroy();
            local.remove();
        }
    }
}
