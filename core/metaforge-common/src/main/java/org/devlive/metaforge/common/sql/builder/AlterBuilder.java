package org.devlive.metaforge.common.sql.builder;

public class AlterBuilder
{
    private static final ThreadLocal<BaseBuilder> localSQL = new ThreadLocal<>();

    static {
        BEGIN();
    }

    private AlterBuilder()
    {
    }

    public static void BEGIN()
    {
        RESET();
    }

    public static void RESET()
    {
        localSQL.set(new BaseBuilder());
    }

    public static void ALTER_TABLE(String table)
    {
        sql().ALTER_TABLE(table);
    }

    public static void AUTO_INCREMENT(String value)
    {
        sql().AUTO_INCREMENT(value);
    }

    public static String SQL()
    {
        try {
            return sql().toString();
        }
        finally {
            RESET();
        }
    }

    private static BaseBuilder sql()
    {
        return localSQL.get();
    }
}
