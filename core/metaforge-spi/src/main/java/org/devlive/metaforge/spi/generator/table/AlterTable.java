package org.devlive.metaforge.spi.generator.table;

import org.devlive.metaforge.spi.generator.Index;
import org.devlive.metaforge.spi.generator.column.CreateColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AlterTable
{
    private final String database;
    private final String tableName;
    private final List<String> addColumns = new ArrayList<>();
    private final List<String> modifyColumns = new ArrayList<>();
    private final List<String> dropColumns = new ArrayList<>();
    private final List<String> addIndexes = new ArrayList<>();
    private final List<String> dropIndexes = new ArrayList<>();
    private String renameTo;
    private String engine;
    private String charset;
    private String collate;
    private String comment;
    private String rowFormat;
    private Long autoIncrement;

    private AlterTable(String database, String tableName)
    {
        this.database = database;
        this.tableName = tableName;
    }

    public static AlterTable create(String database, String tableName)
    {
        return new AlterTable(database, tableName);
    }

    // 添加列
    public AlterTable addColumn(CreateColumn column)
    {
        addColumns.add("ADD COLUMN " + column.build());
        return this;
    }

    // 添加列（指定位置）
    public AlterTable addColumn(CreateColumn column, String afterColumn)
    {
        addColumns.add("ADD COLUMN " + column.build() + " AFTER `" + afterColumn + "`");
        return this;
    }

    // 添加列（作为第一列）
    public AlterTable addColumnFirst(CreateColumn column)
    {
        addColumns.add("ADD COLUMN " + column.build() + " FIRST");
        return this;
    }

    // 修改列
    public AlterTable modifyColumn(CreateColumn column)
    {
        modifyColumns.add("MODIFY COLUMN " + column.build());
        return this;
    }

    // 删除列
    public AlterTable dropColumn(String columnName)
    {
        dropColumns.add("DROP COLUMN `" + columnName + "`");
        return this;
    }

    // 添加索引
    public AlterTable addIndex(Index index)
    {
        addIndexes.add("ADD " + index.build());
        return this;
    }

    // 删除索引
    public AlterTable dropIndex(String indexName)
    {
        dropIndexes.add("DROP INDEX `" + indexName + "`");
        return this;
    }

    // 添加主键
    public AlterTable addPrimaryKey(String... columns)
    {
        String primaryKeyColumns = Arrays.stream(columns)
                .map(col -> "`" + col + "`")
                .collect(Collectors.joining(", "));
        addIndexes.add("ADD PRIMARY KEY (" + primaryKeyColumns + ")");
        return this;
    }

    // 删除主键
    public AlterTable dropPrimaryKey()
    {
        dropIndexes.add("DROP PRIMARY KEY");
        return this;
    }

    // 重命名表
    public AlterTable renameTo(String newTableName)
    {
        this.renameTo = newTableName;
        return this;
    }

    // 修改表引擎
    public AlterTable engine(String engine)
    {
        this.engine = engine;
        return this;
    }

    // 修改表字符集
    public AlterTable charset(String charset)
    {
        this.charset = charset;
        return this;
    }

    // 修改表排序规则
    public AlterTable collate(String collate)
    {
        this.collate = collate;
        return this;
    }

    // 修改表注释
    public AlterTable comment(String comment)
    {
        this.comment = comment;
        return this;
    }

    // 修改表行格式
    public AlterTable rowFormat(String rowFormat)
    {
        this.rowFormat = rowFormat;
        return this;
    }

    // 设置表的自增值
    public AlterTable autoIncrement(long value)
    {
        this.autoIncrement = value;
        return this;
    }

    public String build()
    {
        List<String> alterations = new ArrayList<>();

        // 添加所有变更操作
        alterations.addAll(addColumns);
        alterations.addAll(modifyColumns);
        alterations.addAll(dropColumns);
        alterations.addAll(addIndexes);
        alterations.addAll(dropIndexes);

        // 添加表选项
        if (renameTo != null) {
            alterations.add("RENAME TO `" + renameTo + "`");
        }
        if (engine != null) {
            alterations.add("ENGINE = " + engine);
        }
        if (charset != null) {
            alterations.add("DEFAULT CHARACTER SET = " + charset);
        }
        if (collate != null) {
            alterations.add("COLLATE = " + collate);
        }
        if (rowFormat != null) {
            alterations.add("ROW_FORMAT = " + rowFormat);
        }
        if (comment != null) {
            alterations.add("COMMENT = '" + comment + "'");
        }
        if (autoIncrement != null) {
            alterations.add("AUTO_INCREMENT = " + autoIncrement);
        }

        // 构建完整的 ALTER TABLE 语句
        String sql = "ALTER TABLE `" + database + "`.`" + tableName + "` \n" +
                alterations.stream()
                        .collect(
                                Collectors.joining(",\n  ", "  ", ";")
                        );

        return sql;
    }
}
