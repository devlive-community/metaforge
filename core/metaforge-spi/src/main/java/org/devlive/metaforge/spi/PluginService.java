package org.devlive.metaforge.spi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import org.devlive.metaforge.spi.generator.OrderBy;
import org.devlive.metaforge.spi.generator.column.CreateColumn;
import org.devlive.metaforge.spi.generator.column.SelectColumn;
import org.devlive.metaforge.spi.generator.definition.BaseDefinition;
import org.devlive.metaforge.spi.generator.definition.ColumnDefinition;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.generator.table.AbstractTable;
import org.devlive.metaforge.spi.generator.table.AlterTable;
import org.devlive.metaforge.spi.generator.table.CreateTable;
import org.devlive.metaforge.spi.generator.table.DeleteTable;
import org.devlive.metaforge.spi.generator.table.DropTable;
import org.devlive.metaforge.spi.generator.table.InsertTable;
import org.devlive.metaforge.spi.generator.table.SelectTable;
import org.devlive.metaforge.spi.generator.table.TruncateTable;
import org.devlive.metaforge.spi.generator.table.UpdateTable;
import org.devlive.metaforge.spi.model.Configure;
import org.devlive.metaforge.spi.model.Pagination;
import org.devlive.metaforge.spi.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
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

    default void connect(Configure configure)
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
    default String url(Configure configure)
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

    default Response execute(Configure configure, String content)
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
    default void executeStream(Configure configure, String content, int fetchSize, RowCallback callback)
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
            Configure configure,
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
    default Response getDatabases(Configure configure)
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
    default Response metadataDatabases(Configure configure)
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
    private java.sql.Connection openJdbcConnection(Configure configure)
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
    default Response getTables(Configure configure, String database)
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
    default Response metadataTables(Configure configure, String database)
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
    default Response getColumns(Configure configure, String database, String table)
    {
        String sql = "SELECT detail.*\n" +
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
    default Response metadataColumns(Configure configure, String database, String table)
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
                "object_definition"));
        response.setTypes(Lists.newArrayList(
                "String", "String", "String", "String", "String", "String", "Integer", "String"));
        List<Object> rows = Lists.newArrayList();
        response.setColumns(rows);
        try {
            DatabaseMetaData metaData = openJdbcConnection(configure).getMetaData();
            try (ResultSet resultSet = metaData.getColumns(null, database, table, "%")) {
                while (resultSet.next()) {
                    String defaultValue = resultSet.getString("COLUMN_DEF");
                    String comment = resultSet.getString("REMARKS");
                    rows.add(Lists.newArrayList(
                            "column",
                            resultSet.getString("COLUMN_NAME"),
                            resultSet.getString("TYPE_NAME"),
                            resultSet.getInt("NULLABLE") == java.sql.ResultSetMetaData.columnNullable ? "YES" : "NO",
                            defaultValue == null ? "" : defaultValue,
                            comment == null ? "" : comment,
                            resultSet.getInt("ORDINAL_POSITION"),
                            ""));
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
                            "PRIMARY KEY on (" + String.join(", ", primaryColumns) + ")"));
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

    default Response getPrimaryKeys(Configure configure, String database, String table)
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
    default Response getDatabase(Configure configure, String database)
    {
        String sql = "SELECT \n" +
                "    s.SCHEMA_NAME as object_name,\n" +
                "    s.DEFAULT_CHARACTER_SET_NAME as object_charset,\n" +
                "    s.DEFAULT_COLLATION_NAME as object_collation,\n" +
                "    db_stats.object_create_time,\n" +
                "    db_stats.object_update_time,\n" +
                "    db_stats.object_data_size,\n" +
                "    db_stats.object_index_size,\n" +
                "    db_stats.object_total_size,\n" +
                "    db_stats.object_table_count,\n" +
                "    db_stats.object_column_count,\n" +
                "    db_stats.object_index_count,\n" +
                "    db_stats.object_view_count,\n" +
                "    db_stats.object_procedure_count,\n" +
                "    db_stats.object_trigger_count,\n" +
                "    db_stats.object_foreign_key_count,\n" +
                "    db_stats.object_total_rows\n" +
                "FROM\n" +
                "    information_schema.SCHEMATA s,\n" +
                "    (\n" +
                "        SELECT\n" +
                "            TABLE_SCHEMA,\n" +
                "            MIN(CREATE_TIME) as object_create_time,\n" +
                "            MAX(UPDATE_TIME) as object_update_time,\n" +
                "            ROUND(SUM(DATA_LENGTH)/1024/1024, 2) as object_data_size,\n" +
                "            ROUND(SUM(INDEX_LENGTH)/1024/1024, 2) as object_index_size,\n" +
                "            ROUND(SUM(DATA_LENGTH + INDEX_LENGTH)/1024/1024, 2) as object_total_size,\n" +
                "            COUNT(*) as object_table_count,\n" +
                "            SUM(TABLE_ROWS) as object_total_rows,\n" +
                "            (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = '{0}') as object_column_count,\n" +
                "            (SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = '{0}') as object_index_count,\n" +
                "            (SELECT COUNT(*) FROM information_schema.VIEWS WHERE TABLE_SCHEMA = '{0}') as object_view_count,\n" +
                "            (SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = '{0}' AND ROUTINE_TYPE = 'PROCEDURE') as object_procedure_count,\n" +
                "            (SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = '{0}') as object_trigger_count,\n" +
                "            (SELECT COUNT(DISTINCT CONSTRAINT_NAME) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = '{0}') as object_foreign_key_count\n" +
                "        FROM\n" +
                "            information_schema.TABLES\n" +
                "        WHERE\n" +
                "            TABLE_SCHEMA = '{0}'\n" +
                "            AND TABLE_TYPE = 'BASE TABLE'\n" +
                "        GROUP BY\n" +
                "            TABLE_SCHEMA\n" +
                "    ) as db_stats\n" +
                "WHERE\n" +
                "    s.SCHEMA_NAME = '{0}';";

        return this.execute(
                configure,
                sql.replace("{0}", database)
        );
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
    default Response getTable(Configure configure, String database, String table)
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

    default Response getTableStatement(Configure configure, TableDefinition definition)
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

    default Response getSuggests(Configure configure, String keyword)
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
    default Response changeAutoIncrement(Configure configure, TableDefinition definition)
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
    default Response createTable(Configure configure, TableDefinition definition)
    {
        AbstractTable tableDefinition = CreateTable.create(definition.getDatabase(), definition.getName())
                .comment(definition.getComment())
                .engine(definition.getEngine());

        definition.getColumns().forEach(col -> {
            if (col.isPrimaryKey()) {
                tableDefinition.addPrimaryKey(col.getName());
            }

            CreateColumn column = this.getCreateColumn(col);

            tableDefinition.addColumn(column);
        });

        return this.getResponse(
                tableDefinition.build(),
                configure,
                definition
        );
    }

    /**
     * 删除表
     * Delete table
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response dropTable(Configure configure, TableDefinition definition)
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
    default Response truncateTable(Configure configure, TableDefinition definition)
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
    default Response queryTable(Configure configure, TableDefinition definition)
    {
        SelectTable tableDefinition = SelectTable.create(definition.getDatabase(), definition.getName())
                .limit(definition.getPagination().getSize())
                .offset((long) (definition.getPagination().getPage() - 1) * definition.getPagination().getSize());

        // 添加查询列
        definition.getColumns().forEach(col -> tableDefinition.addColumn(SelectColumn.create(col.getName(), DataType.VARCHAR)));

        // 添加排序
        definition.getOrders().forEach(order -> tableDefinition.addOrderBy(OrderBy.create(order.getName(), order.getOrder())));

        // 添加筛选
        definition.getFilters().forEach(filter -> tableDefinition.addFilter(Filter.create(filter.getName(), filter.getOperator(), filter.getValue())));

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
    default Response deleteData(Configure configure, TableDefinition definition)
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
    default Response createColumn(Configure configure, TableDefinition definition)
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
    default Response dropColumn(Configure configure, TableDefinition definition)
    {
        AlterTable tableDefinition = AlterTable.create(definition.getDatabase(), definition.getName());

        definition.getColumns().forEach(col -> tableDefinition.dropColumn(col.getName()));

        return this.getResponse(
                tableDefinition.build(),
                configure,
                definition
        );
    }

    /**
     * 获取指定列
     * Get column
     *
     * @param configure 配置信息 | Configuration information
     * @param definition 表配置定义 | Table configuration definition
     * @return 执行结果 | Execution result
     */
    default Response getColumn(Configure configure, TableDefinition definition)
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
    default Response changeColumn(Configure configure, TableDefinition definition)
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
    default Response insertData(Configure configure, TableDefinition definition)
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
    default Response updateData(Configure configure, TableDefinition definition)
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

    default Response getResponse(String sql, Configure configure, BaseDefinition definition)
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

    private Response getResponse(String sql, Configure configure, TableDefinition definition, Pagination pagination)
    {
        Response response = this.getResponse(sql, configure, definition);
        response.setPagination(pagination);
        return response;
    }

    default CreateColumn getCreateColumn(ColumnDefinition col)
    {
        CreateColumn column = CreateColumn.create(col.getName(), col.getType());

        column.comment(col.getComment())
                .length(col.getLength())
                .defaultValue(col.getDefaultValue());

        if (col.isAutoIncrement()) {
            column.autoIncrement();
        }

        if (col.isNullable()) {
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
    default Pagination getPagination(Configure configure, TableDefinition definition)
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
                    "    FROM `{0}`.`{1}`\n" +
                    ") t";

            return this.formatPagination(configure, sql, definition);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    default Pagination formatPagination(Configure configure, String sql, TableDefinition definition)
    {
        Response response = this.getResponse(
                sql.replace("{0}", definition.getDatabase())
                        .replace("{1}", definition.getName())
                        .replace("{2}", String.valueOf(definition.getPagination().getPage()))
                        .replace("{3}", String.valueOf(definition.getPagination().getSize())),
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

    default List<String> formatPrimaryKeys(Configure configure, String database, String table)
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
