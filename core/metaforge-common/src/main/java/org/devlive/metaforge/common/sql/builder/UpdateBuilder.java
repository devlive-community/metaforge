package org.devlive.metaforge.common.sql.builder;

import java.util.List;

public class UpdateBuilder
{
    private static final ThreadLocal<BaseBuilder> localSQL = new ThreadLocal<>();
    private static String symbol = " AND ";

    static {
        BEGIN();
    }

    private UpdateBuilder()
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

    public static void UPDATE(String table)
    {
        sql().UPDATE(table);
    }

    public static void SET(String sets)
    {
        sql().SET(sets);
    }

    public static void SET(List<String> sets)
    {
        sql().SET(String.join(symbol, sets));
    }

    public static void WHERE(String conditions)
    {
        sql().WHERE(conditions);
    }

    public static void WHERE(List<String> conditions)
    {
        sql().WHERE(String.join(symbol, conditions));
    }

    public static void OR()
    {
        sql().OR();
    }

    public static void AND()
    {
        sql().AND();
    }

    public static void END()
    {
        sql().END();
    }

    public static String SQL()
    {
        try {
            sql().END();
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
