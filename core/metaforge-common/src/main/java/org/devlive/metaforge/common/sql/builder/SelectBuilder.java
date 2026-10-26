package org.devlive.metaforge.common.sql.builder;

import java.util.List;

public class SelectBuilder
{
    private static final ThreadLocal<BaseBuilder> localSQL = new ThreadLocal<>();
    private static String symbol = ", ";

    static {
        BEGIN();
    }

    private SelectBuilder()
    {
        // Prevent Instantiation
    }

    public static void BEGIN()
    {
        RESET();
    }

    public static void RESET()
    {
        localSQL.set(new BaseBuilder());
    }

    public static void SELECT(String columns)
    {
        sql().SELECT(columns);
    }

    public static void SELECT(List<String> columns)
    {
        sql().SELECT(String.join(symbol, columns));
    }

    public static void SELECT_DISTINCT(String columns)
    {
        sql().SELECT_DISTINCT(columns);
    }

    public static void FROM(String table)
    {
        sql().FROM(table);
    }

    public static void WHERE(String conditions)
    {
        sql().WHERE(conditions);
    }

    public static void OR()
    {
        sql().OR();
    }

    public static void AND()
    {
        sql().AND();
    }

    public static void GROUP_BY(String columns)
    {
        sql().GROUP_BY(columns);
    }

    public static void GROUP_BY(List<String> columns)
    {
        sql().GROUP_BY(String.join(symbol, columns));
    }

    public static void HAVING(String conditions)
    {
        sql().HAVING(conditions);
    }

    public static void ORDER_BY(String columns)
    {
        sql().ORDER_BY(columns);
    }

    public static void ORDER_BY(List<String> columns)
    {
        sql().ORDER_BY(String.join(symbol, columns));
    }

    public static void LIMIT(long value)
    {
        sql().LIMIT(value);
    }

    public static void OFFSET(long value)
    {
        sql().OFFSET(value);
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
