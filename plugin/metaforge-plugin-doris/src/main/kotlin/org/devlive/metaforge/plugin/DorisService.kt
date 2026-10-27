package org.devlive.metaforge.plugin

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.generator.definition.TableDefinition
import org.devlive.metaforge.spi.model.RuntimeConfigure
import org.devlive.metaforge.spi.model.Response

@SuppressFBWarnings(value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE"])
class DorisService : PluginService
{
    override fun connectType(): String
    {
        return "mysql"
    }

    override fun driver(): String
    {
        return "com.mysql.cj.jdbc.Driver"
    }

    override fun isSupportMeta(): Boolean
    {
        return true
    }

    override fun getTables(configure: RuntimeConfigure?, database: String?): Response
    {
        val sql = """SELECT
                        CASE
                            WHEN type = 'BASE TABLE' THEN 'table'
                            WHEN type = 'VIEW' THEN 'view'
                            WHEN type = 'FUNCTION' THEN 'function'
                            WHEN type = 'PROCEDURE' THEN 'procedure'
                        END AS type_name,
                        object_name,
                        object_comment
                    FROM (
                        -- 表
                        SELECT
                            'BASE TABLE' as type,
                            TABLE_NAME as object_name,
                            TABLE_COMMENT as object_comment
                        FROM information_schema.TABLES
                        WHERE TABLE_SCHEMA = '{0}'
                            AND TABLE_TYPE = 'BASE TABLE'
                    
                        UNION ALL
                    
                        -- 视图
                        SELECT
                            'VIEW' as type,
                            TABLE_NAME as object_name,
                            TABLE_COMMENT as object_comment
                        FROM information_schema.TABLES
                        WHERE TABLE_SCHEMA = '{0}'
                            AND TABLE_TYPE = 'VIEW'
                    
                        UNION ALL
                    
                        -- 函数
                        SELECT
                            'FUNCTION' as type,
                            ROUTINE_NAME as object_name,
                            ROUTINE_COMMENT as object_comment
                        FROM information_schema.ROUTINES
                        WHERE ROUTINE_SCHEMA = '{0}'
                            AND ROUTINE_TYPE = 'FUNCTION'
                    
                        UNION ALL
                    
                        -- 存储过程（查询）
                        SELECT
                            'PROCEDURE' as type,
                            ROUTINE_NAME as object_name,
                            ROUTINE_COMMENT as object_comment
                        FROM information_schema.ROUTINES
                        WHERE ROUTINE_SCHEMA = '{0}'
                            AND ROUTINE_TYPE = 'PROCEDURE'
                    ) AS combined_objects
                    ORDER BY
                        CASE type
                            WHEN 'BASE TABLE' THEN 1
                            WHEN 'VIEW' THEN 2
                            WHEN 'FUNCTION' THEN 3
                            WHEN 'PROCEDURE' THEN 4
                            ELSE 5
                        END,
                        object_name;"""

        return this.execute(configure, sql.replace("{0}", database !!))
    }

    override fun getColumns(configure: RuntimeConfigure?, database: String?, table: String?): Response
    {
        val sql = """SELECT detail.*
                        FROM (
                            -- 列信息
                            SELECT
                                'column' as type_name,
                                COLUMN_NAME as object_name,
                                COLUMN_TYPE as object_data_type,
                                IS_NULLABLE as object_nullable,
                                COLUMN_DEFAULT as object_default_value,
                                COLUMN_COMMENT as object_comment,
                                ORDINAL_POSITION as object_position,
                                '' as object_definition
                            FROM
                                information_schema.COLUMNS
                            WHERE
                                TABLE_SCHEMA = '{0}'
                                AND TABLE_NAME = '{1}'
                        
                            UNION ALL
                        
                            -- 主键信息
                            SELECT
                                'primary' as type_name,
                                COLUMN_NAME as object_name,
                                '' as object_data_type,
                                '' as object_nullable,
                                '' as object_default_value,
                                '' as object_comment,
                                0 as object_position,
                                'PRIMARY KEY' as object_definition
                            FROM
                                information_schema.columns
                            WHERE
                                TABLE_SCHEMA = '{0}'
                                AND TABLE_NAME = '{1}'
                                AND COLUMN_KEY = 'PRI'
                        
                            UNION ALL
                        
                            -- 索引信息
                            SELECT
                                'index' as type_name,
                                INDEX_NAME as object_name,
                                '' as object_data_type,
                                '' as object_nullable,
                                '' as object_default_value,
                                '' as object_comment,
                                0 as object_position,
                                CONCAT(
                                    CASE NON_UNIQUE
                                        WHEN 1 THEN 'Non-unique'
                                        ELSE 'Unique'
                                    END,
                                    ' index'
                                ) as object_definition
                            FROM
                                information_schema.STATISTICS
                            WHERE
                                TABLE_SCHEMA = '{0}'
                                AND TABLE_NAME = '{1}'
                            GROUP BY
                                INDEX_NAME, NON_UNIQUE
                        
                            UNION ALL
                        
                            -- 触发器信息
                            SELECT
                                'trigger' as type_name,
                                TRIGGER_NAME as object_name,
                                '' as object_data_type,
                                '' as object_nullable,
                                '' as object_default_value,
                                '' as object_comment,
                                0 as object_position,
                                CONCAT(
                                    'TRIGGER ',
                                    ACTION_TIMING, ' ',
                                    EVENT_MANIPULATION
                                ) as object_definition
                            FROM
                                information_schema.TRIGGERS
                            WHERE
                                EVENT_OBJECT_SCHEMA = '{0}'
                                AND EVENT_OBJECT_TABLE = '{1}'
                        ) detail
                        ORDER BY
                            CASE type_name
                                WHEN 'column' THEN 1
                                WHEN 'primary' THEN 2
                                WHEN 'index' THEN 3
                                WHEN 'trigger' THEN 4
                                ELSE 5
                            END,
                            object_position,
                            object_name;"""

        return this.execute(
            configure,
            sql.replace("{0}", database !!)
                .replace("{1}", table !!)
        )
    }

    override fun getSuggests(configure: RuntimeConfigure?, keyword: String?): Response
    {
        val sql = """SELECT * FROM (
                        -- 数据库
                        SELECT
                            SCHEMA_NAME as object_name,
                            'Database' as object_type,
                            'common.database' as object_key,
                            1 as sort_order
                        FROM information_schema.SCHEMATA
                        WHERE SCHEMA_NAME LIKE '%{0}%'
                    
                        UNION ALL
                        
                        -- 表和视图
                        SELECT
                            TABLE_NAME as object_name,
                            CASE TABLE_TYPE
                                WHEN 'BASE TABLE' THEN 'Table'
                                WHEN 'VIEW' THEN 'View'
                                ELSE 'Table'
                            END as object_type,
                            CASE TABLE_TYPE
                                WHEN 'BASE TABLE' THEN 'common.table'
                                WHEN 'VIEW' THEN 'common.view'
                                ELSE 'common.table'
                            END as object_key,
                            CASE TABLE_TYPE
                                WHEN 'BASE TABLE' THEN 2
                                WHEN 'VIEW' THEN 3
                                ELSE 2
                            END as sort_order
                        FROM information_schema.TABLES
                        WHERE TABLE_NAME LIKE '%{0}%'
                    
                        UNION ALL
                        
                        -- 列（包含主键、索引信息）
                        SELECT DISTINCT
                            COLUMN_NAME as object_name,
                            CASE
                                WHEN COLUMN_KEY = 'PRI' THEN 'Key'
                                WHEN COLUMN_KEY = 'UNI' THEN 'Signature'
                                WHEN COLUMN_KEY = 'MUL' THEN 'Hash'
                                ELSE 'Columns2'
                            END as object_type,
                            CASE
                                WHEN COLUMN_KEY = 'PRI' THEN 'common.primary'
                                WHEN COLUMN_KEY = 'UNI' THEN 'common.unique'
                                WHEN COLUMN_KEY = 'MUL' THEN 'common.index'
                                ELSE 'common.column'
                            END as object_key,
                            CASE
                                WHEN COLUMN_KEY = 'PRI' THEN 4
                                WHEN COLUMN_KEY = 'UNI' THEN 5
                                WHEN COLUMN_KEY = 'MUL' THEN 6
                                ELSE 7
                            END as sort_order
                        FROM information_schema.COLUMNS
                        WHERE COLUMN_NAME LIKE '%{0}%'
                    
                        UNION ALL
                        
                        -- 触发器
                        SELECT
                            TRIGGER_NAME as object_name,
                            'Pi' as object_type,
                            'common.trigger' as object_key,
                            8 as sort_order
                        FROM information_schema.TRIGGERS
                        WHERE TRIGGER_NAME LIKE '%{0}%'
                    
                        UNION ALL
                        
                        -- 存储过程
                        SELECT
                            ROUTINE_NAME as object_name,
                            'Microchip' as object_type,
                            'common.procedure' as object_key,
                            9 as sort_order
                        FROM information_schema.ROUTINES
                        WHERE ROUTINE_TYPE = 'PROCEDURE'
                            AND ROUTINE_NAME LIKE '%{0}%'
                    
                        UNION ALL
                        
                        -- 函数
                        SELECT
                            ROUTINE_NAME as object_name,
                            'SquareFunction' as object_type,
                            'common.function' as object_key,
                            10 as sort_order
                        FROM information_schema.ROUTINES
                        WHERE ROUTINE_TYPE = 'FUNCTION'
                            AND ROUTINE_NAME LIKE '%{0}%'
                        ) t
                        ORDER BY sort_order, object_name limit 10;"""
        return this.execute(
            configure,
            sql.replace("{0}", keyword !!)
        )
    }

    override fun getColumn(configure: RuntimeConfigure?, definition: TableDefinition?): Response
    {
        val column = definition !!.columns.stream().findAny()

        require(! column.isEmpty) { "Column must be specified" }

        val sql = """SELECT
                            detail.*
                        FROM
                            (
                            -- 列信息：优化了类型解析
                            SELECT
                                'column' as type_name,
                                COLUMN_NAME as object_name,
                                SUBSTRING_INDEX(COLUMN_TYPE, '(', 1) as object_data_type,
                                -- 更高效的类型提取
                                                NULLIF(
                                                    SUBSTRING_INDEX(SUBSTRING_INDEX(COLUMN_TYPE, '(', -1), ')', 1),
                                                    COLUMN_TYPE
                                                ) as object_length,
                                -- 更高效的长度提取
                                IS_NULLABLE as object_nullable,
                                COLUMN_DEFAULT as object_default_value,
                                COLUMN_COMMENT as object_comment,
                                ORDINAL_POSITION as object_position,
                                '' as object_definition
                            FROM
                                information_schema.COLUMNS c
                            WHERE
                                TABLE_SCHEMA = '{0}'
                                AND TABLE_NAME = '{1}'
                                AND COLUMN_NAME = '{2}'
                        UNION ALL
                            -- 主键信息：添加了索引以提升性能
                            SELECT
                                'primary' as type_name,
                                COLUMN_NAME as object_name,
                                '' as object_data_type,
                                NULL as object_length,
                                '' as object_nullable,
                                '' as object_default_value,
                                '' as object_comment,
                                0 as object_position,
                                CONCAT('PRIMARY KEY on (',
                                                    GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION),
                                                    ')'
                                                ) as object_definition
                            FROM
                                information_schema.columns
                            WHERE
                                TABLE_SCHEMA = '{0}'
                                AND TABLE_NAME = '{1}'
                                AND COLUMN_KEY = 'PRI'
                                AND COLUMN_NAME = '{2}'
                            GROUP BY
                                TABLE_SCHEMA,
                                TABLE_NAME,
                                COLUMN_KEY,
                                COLUMN_NAME
                        UNION ALL
                            -- 索引信息：优化了索引名称的处理
                            SELECT
                            'index' as type_name,
                            COLUMN_NAME as object_name,
                            '' as object_data_type,
                            NULL as object_length,
                            '' as object_nullable,
                            '' as object_default_value,
                            '' as object_comment,
                            0 as object_position,
                            CONCAT(
                                CASE NON_UNIQUE
                                    WHEN 1 THEN 'Non-unique'
                                    ELSE 'Unique'
                                END,
                                ' index on ',
                                COLUMN_NAME
                            ) as object_definition
                        FROM
                            information_schema.STATISTICS
                        WHERE
                            TABLE_SCHEMA = '{0}'
                            AND TABLE_NAME = '{1}'
                            AND COLUMN_NAME = '{2}'
                        GROUP BY
                            TABLE_SCHEMA,
                            TABLE_NAME,
                            COLUMN_NAME,
                            NON_UNIQUE
                        UNION ALL
                            -- 触发器信息：保持原有逻辑
                            SELECT
                                'trigger' as type_name,
                                TRIGGER_NAME as object_name,
                                '' as object_data_type,
                                NULL as object_length,
                                '' as object_nullable,
                                '' as object_default_value,
                                '' as object_comment,
                                0 as object_position,
                                CONCAT(
                                                    'TRIGGER ',
                                                    ACTION_TIMING, ' ',
                                                    EVENT_MANIPULATION
                                                ) as object_definition
                            FROM
                                information_schema.TRIGGERS
                            WHERE
                                EVENT_OBJECT_SCHEMA = '{0}'
                                AND EVENT_OBJECT_TABLE = '{1}'
                                        ) detail
                        ORDER BY
                            CASE type_name
                                WHEN 'column' THEN 1
                                WHEN 'primary' THEN 2
                                WHEN 'index' THEN 3
                                WHEN 'trigger' THEN 4
                                ELSE 5
                            END,
                            object_position,
                            object_name;"""

        return this.getResponse(
            sql.replace("{0}", definition.database)
                .replace("{1}", definition.name)
                .replace("{2}", column.get().name),
            configure,
            definition
        )
    }
}
