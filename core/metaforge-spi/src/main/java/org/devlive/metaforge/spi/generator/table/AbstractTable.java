package org.devlive.metaforge.spi.generator.table;

import org.devlive.metaforge.spi.generator.Index;
import org.devlive.metaforge.spi.generator.column.Column;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractTable
        implements Table
{
    protected final String database;
    protected final String name;
    protected final List<String> columns = new ArrayList<>();
    protected final List<String> primaryKeys = new ArrayList<>();
    protected final List<Index> indexes = new ArrayList<>();
    protected String engine;
    protected String charset = "utf8mb4";
    protected String collate = "utf8mb4_general_ci";
    protected String comment;
    protected String rowFormat;

    protected AbstractTable(String database, String name)
    {
        this.database = database;
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public AbstractTable addColumn(Column column)
    {
        columns.add(column.build());
        return this;
    }

    public AbstractTable addPrimaryKey(String... columns)
    {
        primaryKeys.addAll(Arrays.asList(columns));
        return this;
    }

    public AbstractTable addIndex(Index index)
    {
        indexes.add(index);
        return this;
    }

    public AbstractTable engine(String engine)
    {
        this.engine = engine;
        return this;
    }

    public AbstractTable charset(String charset)
    {
        this.charset = charset;
        return this;
    }

    public AbstractTable collate(String collate)
    {
        this.collate = collate;
        return this;
    }

    public AbstractTable comment(String comment)
    {
        this.comment = comment;
        return this;
    }

    public AbstractTable rowFormat(String rowFormat)
    {
        this.rowFormat = rowFormat;
        return this;
    }

    protected void appendTableOptions(StringBuilder sql)
    {
        if (!engine.isEmpty()) {
            sql.append(" ENGINE=").append(engine);
        }

        if (charset != null) {
            sql.append(" DEFAULT CHARSET=").append(charset);
        }

        if (collate != null) {
            sql.append(" COLLATE=").append(collate);
        }

        if (rowFormat != null) {
            sql.append(" ROW_FORMAT=").append(rowFormat);
        }

        if (comment != null) {
            sql.append(" COMMENT='").append(comment).append("'");
        }
    }
}
