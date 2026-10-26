package org.devlive.metaforge.common.sql.builder;

public class TruncateBuilder
{
    private static final ThreadLocal<BaseBuilder> localSQL = new ThreadLocal<>();

    static {
        BEGIN();
    }

    private TruncateBuilder()
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

    public static void TRUNCATE(String table)
    {
        sql().TRUNCATE(table);
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
