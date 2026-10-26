package org.devlive.metaforge.spi.generator.table;

import org.devlive.metaforge.spi.generator.Index;

import java.util.stream.Collectors;

public class CreateTable
        extends AbstractTable
{
    private CreateTable(String database, String name)
    {
        super(database, name);
    }

    public static CreateTable create(String database, String name)
    {
        return new CreateTable(database, name);
    }

    @Override
    public String build()
    {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE `")
                .append(database).append("`.`")
                .append(name)
                .append("` (\n");

        // 添加列定义
        sql.append(columns.stream()
                .map(col -> "  " + col)
                .collect(Collectors.joining(",\n")));

        // 添加主键
        if (!primaryKeys.isEmpty()) {
            sql.append(",\n  PRIMARY KEY (")
                    .append(primaryKeys.stream()
                            .map(col -> "`" + col + "`")
                            .collect(Collectors.joining(", ")))
                    .append(")");
        }

        // 添加索引
        for (Index index : indexes) {
            sql.append(",\n  ").append(index.build());
        }

        sql.append("\n)");

        // 添加表选项
        appendTableOptions(sql);

        sql.append(";");
        return sql.toString();
    }
}
