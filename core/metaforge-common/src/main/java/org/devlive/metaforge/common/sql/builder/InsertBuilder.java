package org.devlive.metaforge.common.sql.builder;

public class InsertBuilder
{
    private static final ThreadLocal<BaseBuilder> localSQL = new ThreadLocal<>();

    static {
        BEGIN();
    }

    private InsertBuilder()
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

    public static void INSERT_INTO(String table)
    {
        sql().INSERT_INTO(table);
    }

    public static void INTO_COLUMNS(String... values)
    {
        sql().INTO_COLUMNS(values);
    }

    public static void INTO_VALUES(String... values)
    {
        sql().INTO_VALUES(values);
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
