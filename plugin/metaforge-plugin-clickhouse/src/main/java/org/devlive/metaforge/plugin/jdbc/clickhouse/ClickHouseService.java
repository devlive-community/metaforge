package org.devlive.metaforge.plugin.jdbc.clickhouse;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.generator.column.CreateColumn;
import org.devlive.metaforge.spi.generator.definition.ColumnDefinition;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.model.Configure;
import org.devlive.metaforge.spi.model.Response;

import java.util.Optional;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class ClickHouseService
        implements PluginService
{
    @Override
    public String connectType()
    {
        return "clickhouse";
    }

    @Override
    public String driver()
    {
        return "com.clickhouse.jdbc.ClickHouseDriver";
    }

    @Override
    public Boolean isSupportMeta()
    {
        return true;
    }

    @Override
    public Response getEngines()
    {
        return Response.builder()
                .columns(Lists.newArrayList(
                        "MergeTree",
                        "ReplicatedMergeTree",
                        "ReplacingMergeTree",
                        "SummingMergeTree",
                        "AggregatingMergeTree",
                        "CollapsingMergeTree",
                        "Log",
                        "StripeLog",
                        "TinyLog"
                ))
                .isConnected(true)
                .isSuccessful(true)
                .build();
    }

    @Override
    public Response getTables(Configure configure, String database)
    {
        String sql = "SELECT\n" +
                "    type_name,\n" +
                "    object_name,\n" +
                "    object_comment\n" +
                "FROM (\n" +
                "    -- 表\n" +
                "    SELECT\n" +
                "        'table' as type_name,\n" +
                "        name as object_name,\n" +
                "        comment as object_comment\n" +
                "    FROM system.tables\n" +
                "    WHERE database = '{0}'\n" +
                "        AND engine != 'View'\n" +
                "        AND engine != 'MaterializedView'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 视图\n" +
                "    SELECT\n" +
                "        'view' as type_name,\n" +
                "        name as object_name,\n" +
                "        comment as object_comment\n" +
                "    FROM system.tables\n" +
                "    WHERE database = '{0}'\n" +
                "        AND (engine = 'View' OR engine = 'MaterializedView')\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 函数\n" +
                "    SELECT\n" +
                "        'function' as type_name,\n" +
                "        name as object_name,\n" +
                "        '' as object_comment\n" +
                "    FROM system.functions\n" +
                "\n" +
                ") AS combined_objects\n" +
                "ORDER BY\n" +
                "    CASE type_name\n" +
                "        WHEN 'table' THEN 1\n" +
                "        WHEN 'view' THEN 2\n" +
                "        WHEN 'function' THEN 3\n" +
                "    END,\n" +
                "    object_name;";

        return this.execute(configure, sql.replace("{0}", database));
    }

    @Override
    public Response getColumns(Configure configure, String database, String table)
    {
        String sql = "SELECT * FROM (\n" +
                "    SELECT \n" +
                "        'column' as type_name,\n" +
                "        name as object_name,\n" +
                "        type as object_data_type,\n" +
                "        CASE WHEN type LIKE 'Nullable%' THEN 'YES' ELSE 'NO' END as object_nullable,\n" +
                "        default_expression as object_default_value,\n" +
                "        comment as object_comment,\n" +
                "        position as object_position,\n" +
                "        '' as object_definition\n" +
                "    FROM system.columns WHERE database = '{0}' AND table = '{1}'\n" +
                "    \n" +
                "    UNION ALL\n" +
                "    \n" +
                "    SELECT \n" +
                "        'primary' as type_name,\n" +
                "        name as object_name,\n" +
                "        '' as object_data_type,\n" +
                "        '' as object_nullable,\n" +
                "        '' as object_default_value,\n" +
                "        '' as object_comment,\n" +
                "        0 as object_position,\n" +
                "        'PRIMARY KEY' as object_definition\n" +
                "    FROM system.columns WHERE database = '{0}' AND table = '{1}' AND is_in_primary_key = 1\n" +
                "    \n" +
                "    UNION ALL\n" +
                "    \n" +
                "    SELECT \n" +
                "        'index' as type_name,\n" +
                "        name as object_name,\n" +
                "        '' as object_data_type,\n" +
                "        '' as object_nullable,\n" +
                "        '' as object_default_value,\n" +
                "        '' as object_comment,\n" +
                "        0 as object_position,\n" +
                "        concat(CASE WHEN type LIKE 'Nullable%' THEN 'Nullable ' ELSE '' END, 'index on (', name, ')') as object_definition\n" +
                "    FROM system.columns WHERE database = '{0}' AND table = '{1}' AND (is_in_sorting_key = 1 OR is_in_partition_key = 1)\n" +
                ") detail\n" +
                "ORDER BY \n" +
                "    CASE type_name WHEN 'column' THEN 1 WHEN 'primary' THEN 2 WHEN 'index' THEN 3 ELSE 4 END,\n" +
                "    object_position,\n" +
                "    object_name\n" +
                "SETTINGS async_insert = 0;";

        return this.execute(
                configure,
                sql.replace("{0}", database)
                        .replace("{1}", table)
        );
    }

    @Override
    public Response getPrimaryKeys(Configure configure, String database, String table)
    {
        String sql = "SELECT\n" +
                "    name as object_name,\n" +
                "    CONCAT('PRIMARY KEY on (', arrayStringConcat(groupArray(name), ', '), ')') as object_definition\n" +
                "FROM system.columns\n" +
                "WHERE database = '{0}'\n" +
                "    AND table = '{1}'\n" +
                "    AND name IN (\n" +
                "        SELECT arrayJoin(splitByChar(',', primary_key))\n" +
                "        FROM system.tables\n" +
                "        WHERE database = '{0}' AND name = '{1}'\n" +
                "    )\n" +
                "GROUP BY name;";

        return this.execute(
                configure,
                sql.replace("{0}", database)
                        .replace("{1}", table)
        );
    }

    @Override
    public Response getTable(Configure configure, String database, String table)
    {
        String sql = "SELECT \n" +
                "    t.engine as object_type,\n" +
                "    t.name as object_name,\n" +
                "    t.engine as object_engine,\n" +
                "    '-' as object_collation,\n" +
                "    t.comment as object_comment,\n" +
                "    t.metadata_modification_time as object_create_time,\n" +
                "    t.metadata_modification_time as object_update_time,\n" +
                "    CASE WHEN t.engine = 'View' THEN '0' ELSE formatReadableSize(t.total_bytes) END as object_data_size,\n" +
                "    CASE WHEN t.engine = 'View' THEN '0' ELSE formatReadableSize(t.total_bytes_uncompressed) END as object_index_size,\n" +
                "    CASE WHEN t.engine = 'View' THEN 0 ELSE t.total_rows END as object_rows,\n" +
                "    (SELECT count() FROM system.columns WHERE database = '{0}' AND table = '{1}') as object_column_count,\n" +
                "    CASE WHEN t.engine = 'View' THEN 0 ELSE (SELECT count() FROM system.data_skipping_indices WHERE database = '{0}' AND table = '{1}') END as object_index_count,\n" +
                "    CASE WHEN t.engine = 'View' THEN 'view' ELSE 'table' END as type_name,\n" +
                "    '-' as object_format,\n" +
                "    CASE WHEN t.engine = 'View' THEN 0 WHEN t.total_rows > 0 THEN round(t.total_bytes / t.total_rows, 2) ELSE 0 END as object_avg_row_length,\n" +
                "    0 as object_auto_increment\n" +
                "FROM system.tables t\n" +
                "WHERE t.database = '{0}'\n" +
                "    AND t.name = '{1}'\n" +
                "SETTINGS async_insert = 0;";

        return this.execute(
                configure,
                sql.replace("{0}", database)
                        .replace("{1}", table)
        );
    }

    @Override
    public Response getTableStatement(Configure configure, TableDefinition definition)
    {
        String sql = "SELECT formatQuery(create_table_query) AS create_table_sql " +
                "FROM system.tables " +
                "WHERE database = '{0}' AND name = '{1}'";

        return this.getResponse(
                sql.replace("{0}", definition.getDatabase())
                        .replace("{1}", definition.getName()),
                configure,
                definition
        );
    }

    @Override
    public Response getSuggests(Configure configure, String keyword)
    {
        String sql = "SELECT * FROM (\n" +
                "    -- 数据库\n" +
                "    SELECT\n" +
                "        name as object_name,\n" +
                "        'Database' as object_type,\n" +
                "        'common.database' as object_key\n" +
                "    FROM system.databases\n" +
                "    WHERE name LIKE '%{0}%'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 表和视图\n" +
                "    SELECT\n" +
                "        name as object_name,\n" +
                "        multiIf(\n" +
                "            engine = 'View', 'View',\n" +
                "            engine = 'MaterializedView', 'View',\n" +
                "            'Table'\n" +
                "        ) as object_type,\n" +
                "        multiIf(\n" +
                "            engine = 'View', 'common.view',\n" +
                "            engine = 'MaterializedView', 'common.view',\n" +
                "            'common.table'\n" +
                "        ) as object_key\n" +
                "    FROM system.tables\n" +
                "    WHERE name LIKE '%{0}%'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 列（包含主键信息）\n" +
                "    SELECT DISTINCT\n" +
                "        name as object_name,\n" +
                "        multiIf(\n" +
                "            is_in_primary_key = 1, 'Key',\n" +
                "            'Columns2'\n" +
                "        ) as object_type,\n" +
                "        multiIf(\n" +
                "            is_in_primary_key = 1, 'common.primary',\n" +
                "            'common.column'\n" +
                "        ) as object_key\n" +
                "    FROM system.columns\n" +
                "    WHERE name LIKE '%{0}%'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 函数\n" +
                "    SELECT\n" +
                "        name as object_name,\n" +
                "        'SquareFunction' as object_type,\n" +
                "        'common.function' as object_key\n" +
                "    FROM system.functions\n" +
                "    WHERE name LIKE '%{0}%'\n" +
                ") suggestions\n" +
                "ORDER BY\n" +
                "    multiIf(\n" +
                "        object_type = 'Database', 1,\n" +
                "        object_type = 'Table', 2,\n" +
                "        object_type = 'View', 3,\n" +
                "        object_type = 'Key', 4,\n" +
                "        object_type = 'Columns2', 5,\n" +
                "        object_type = 'SquareFunction', 6,\n" +
                "        7\n" +
                "    ),\n" +
                "    object_name;";

        return this.execute(configure, sql.replace("{0}", keyword));
    }

    @Override
    public Response getColumn(Configure configure, TableDefinition definition)
    {
        Optional<ColumnDefinition> column = definition.getColumns().stream().findAny();

        if (column.isEmpty()) {
            throw new IllegalArgumentException("Column must be specified");
        }

        String sql = "SELECT\n" +
                "    detail.*\n" +
                "FROM (\n" +
                "    -- 列信息\n" +
                "    SELECT\n" +
                "        'column' as type_name,\n" +
                "        name as object_name,\n" +
                "        type as object_data_type,\n" +
                "        NULL as object_length,\n" +
                "        if(is_in_primary_key = 0, 'YES', 'NO') as object_nullable,\n" +
                "        default_expression as object_default_value,\n" +
                "        comment as object_comment,\n" +
                "        position as object_position,\n" +
                "        '' as object_definition\n" +
                "    FROM\n" +
                "        system.columns\n" +
                "    WHERE\n" +
                "        database = '{0}'\n" +
                "        AND table = '{1}'\n" +
                "        AND name = '{2}'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 主键信息\n" +
                "    SELECT\n" +
                "        'primary' as type_name,\n" +
                "        name as object_name,\n" +
                "        '' as object_data_type,\n" +
                "        NULL as object_length,\n" +
                "        '' as object_nullable,\n" +
                "        '' as object_default_value,\n" +
                "        '' as object_comment,\n" +
                "        0 as object_position,\n" +
                "        concat('PRIMARY KEY on (', name, ')') as object_definition\n" +
                "    FROM\n" +
                "        system.columns\n" +
                "    WHERE\n" +
                "        database = '{0}'\n" +
                "        AND table = '{1}'\n" +
                "        AND name = '{2}'\n" +
                "        AND is_in_primary_key = 1\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 索引信息\n" +
                "    SELECT\n" +
                "        'index' as type_name,\n" +
                "        name as object_name,\n" +
                "        '' as object_data_type,\n" +
                "        NULL as object_length,\n" +
                "        '' as object_nullable,\n" +
                "        '' as object_default_value,\n" +
                "        '' as object_comment,\n" +
                "        0 as object_position,\n" +
                "        concat(\n" +
                "            'Index on (',\n" +
                "            name,\n" +
                "            ')'\n" +
                "        ) as object_definition\n" +
                "    FROM\n" +
                "        system.columns\n" +
                "    WHERE\n" +
                "        database = '{0}'\n" +
                "        AND table = '{1}'\n" +
                "        AND name = '{2}'\n" +
                "        AND is_in_sorting_key = 1\n" +
                "\n" +
                ") detail\n" +
                "ORDER BY\n" +
                "    multiIf(\n" +
                "        type_name = 'column', 1,\n" +
                "        type_name = 'primary', 2,\n" +
                "        type_name = 'index', 3,\n" +
                "        4\n" +
                "    ),\n" +
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

    @Override
    public CreateColumn getCreateColumn(ColumnDefinition col)
    {
        CreateColumn column = org.devlive.metaforge.plugin.jdbc.clickhouse.generator.CreateColumn.create(col.getName(), col.getType());

        column.comment(col.getComment())
                .length(col.getLength())
                .defaultValue(col.getDefaultValue());

        if (col.isNullable()) {
            column.notNull();
        }

        return column;
    }
}
