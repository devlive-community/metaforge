package org.devlive.metaforge.spi.generator.table;

import org.devlive.metaforge.spi.generator.Index;

import java.util.stream.Collectors;

public class CreateTable
        extends AbstractTable
{
    // Preview sections: the create-table page renders the full statement and per-section fragments.
    private boolean includeColumns = true;
    private boolean includeIndexes = true;
    private boolean includeOptions = true;

    private CreateTable(String database, String name)
    {
        super(database, name);
    }

    public static CreateTable create(String database, String name)
    {
        return new CreateTable(database, name);
    }

    public CreateTable includeColumns(boolean value)
    {
        this.includeColumns = value;
        return this;
    }

    public CreateTable includeIndexes(boolean value)
    {
        this.includeIndexes = value;
        return this;
    }

    public CreateTable includeOptions(boolean value)
    {
        this.includeOptions = value;
        return this;
    }

    @Override
    public String build()
    {
        return build("full");
    }

    public String build(String kind)
    {
        if ("columns".equals(kind)) {
            includeIndexes = false;
            includeOptions = false;
        }
        else if ("indexes".equals(kind)) {
            includeColumns = false;
            includeOptions = false;
        }
        else if ("options".equals(kind)) {
            includeColumns = false;
            includeIndexes = false;
        }
        else {
            includeColumns = true;
            includeIndexes = true;
            includeOptions = true;
        }
        return render();
    }

    private String render()
    {
        StringBuilder sql = new StringBuilder();

        if (!includeColumns && includeIndexes && !includeOptions) {
            // 仅索引约束：输出可执行的 ALTER 语句
            sql.append("ALTER TABLE `").append(database).append("`.`").append(name).append("`");
            boolean first = true;
            if (!primaryKeys.isEmpty()) {
                sql.append(" ADD PRIMARY KEY (")
                        .append(primaryKeys.stream()
                                .map(col -> "`" + col + "`")
                                .collect(Collectors.joining(", ")))
                        .append(")");
                first = false;
            }
            for (Index index : indexes) {
                sql.append(first ? " ADD " : ", ADD ").append(index.build());
                first = false;
            }
            sql.append(";");
            return sql.toString();
        }

        sql.append("CREATE TABLE `")
                .append(database).append("`.`")
                .append(name)
                .append("` (\n");

        boolean body = false;
        if (includeColumns) {
            sql.append(columns.stream()
                    .map(col -> "  " + col)
                    .collect(Collectors.joining(",\n")));
            body = !columns.isEmpty();
        }

        if (includeColumns && !primaryKeys.isEmpty()) {
            sql.append(",\n  PRIMARY KEY (")
                    .append(primaryKeys.stream()
                            .map(col -> "`" + col + "`")
                            .collect(Collectors.joining(", ")))
                    .append(")");
            body = true;
        }

        if (includeIndexes) {
            for (Index index : indexes) {
                sql.append(",\n  ").append(index.build());
                body = true;
            }
        }

        if (!body) {
            sql.append("  ").append("`placeholder` INT");
        }

        sql.append("\n)");

        if (includeOptions) {
            appendTableOptions(sql);
        }

        sql.append(";");
        return sql.toString();
    }
}
