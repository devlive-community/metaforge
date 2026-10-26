package org.devlive.metaforge.spi.generator.table;

public class TruncateTable
        extends AbstractTable
{
    private boolean ifExists = false;
    private boolean restartIdentity = false;
    private boolean cascade = false;
    private boolean restrict = false;

    private TruncateTable(String database, String name)
    {
        super(database, name);
    }

    public static TruncateTable create(String database, String name)
    {
        return new TruncateTable(database, name);
    }

    public TruncateTable ifExists()
    {
        this.ifExists = true;
        return this;
    }

    public TruncateTable restartIdentity()
    {
        this.restartIdentity = true;
        return this;
    }

    public TruncateTable cascade()
    {
        this.cascade = true;
        this.restrict = false;
        return this;
    }

    public TruncateTable restrict()
    {
        this.restrict = true;
        this.cascade = false;
        return this;
    }

    @Override
    public String build()
    {
        StringBuilder sql = new StringBuilder();
        sql.append("TRUNCATE TABLE ");

        if (ifExists) {
            sql.append("IF EXISTS ");
        }

        sql.append("`")
                .append(database)
                .append("`.`")
                .append(name)
                .append("`");

        if (restartIdentity) {
            sql.append(" RESTART IDENTITY");
        }

        if (cascade) {
            sql.append(" CASCADE");
        }
        else if (restrict) {
            sql.append(" RESTRICT");
        }

        sql.append(";");
        return sql.toString();
    }
}
