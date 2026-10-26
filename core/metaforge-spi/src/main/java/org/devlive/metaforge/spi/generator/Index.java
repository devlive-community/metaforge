package org.devlive.metaforge.spi.generator;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Index
{
    private final String name;
    private final List<String> columns;
    private boolean unique;
    private String type = "BTREE";
    private String comment;

    private Index(String name, String... columns)
    {
        this.name = name;
        this.columns = Arrays.asList(columns);
    }

    public static Index create(String name, String... columns)
    {
        return new Index(name, columns);
    }

    public Index unique()
    {
        this.unique = true;
        return this;
    }

    public Index type(String type)
    {
        this.type = type;
        return this;
    }

    public Index comment(String comment)
    {
        this.comment = comment;
        return this;
    }

    public String build()
    {
        StringBuilder sql = new StringBuilder();

        if (unique) {
            sql.append("UNIQUE ");
        }

        sql.append("KEY `").append(name).append("` ");

        if (!"BTREE".equals(type)) {
            sql.append("USING ").append(type).append(" ");
        }

        sql.append("(")
                .append(columns.stream()
                        .map(col -> "`" + col + "`")
                        .collect(Collectors.joining(", ")))
                .append(")");

        if (comment != null) {
            sql.append(" COMMENT '").append(comment).append("'");
        }

        return sql.toString();
    }
}
