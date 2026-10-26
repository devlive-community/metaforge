package org.devlive.metaforge.common.sql.builder;

public class DropBuilder
{
    private static final ThreadLocal<BaseBuilder> localSQL = new ThreadLocal<>();

    static {
        BEGIN();
    }

    private DropBuilder()
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

    public static void DROP(String table)
    {
        sql().DROP(table);
    }

    public static void END()
    {
        sql().END();
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
