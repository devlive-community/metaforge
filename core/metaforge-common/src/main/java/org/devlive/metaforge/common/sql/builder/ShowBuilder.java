package org.devlive.metaforge.common.sql.builder;

public class ShowBuilder
{
    private static final ThreadLocal<BaseBuilder> localSQL = new ThreadLocal<>();

    static {
        BEGIN();
    }

    private ShowBuilder()
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

    public static void SHOW_CREATE_TABLE(String table)
    {
        sql().SHOW_CREATE_TABLE(table);
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
