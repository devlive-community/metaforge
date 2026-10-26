package org.devlive.metaforge.spi.generator.table;

public class DropTable
        extends AbstractTable
{
    private boolean ifExists = false;
    private boolean cascade = false;
    private boolean restrict = false;

    private DropTable(String database, String name)
    {
        super(database, name);
    }

    public static DropTable create(String database, String name)
    {
        return new DropTable(database, name);
    }

    public DropTable ifExists()
    {
        this.ifExists = true;
        return this;
    }

    public DropTable cascade()
    {
        this.cascade = true;
        this.restrict = false;
        return this;
    }

    public DropTable restrict()
    {
        this.restrict = true;
        this.cascade = false;
        return this;
    }

    @Override
    public String build()
    {
        StringBuilder sql = new StringBuilder();
        sql.append("DROP TABLE ");

        if (ifExists) {
            sql.append("IF EXISTS ");
        }

        sql.append("`")
                .append(database)
                .append("`.`")
                .append(name)
                .append("`");

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
