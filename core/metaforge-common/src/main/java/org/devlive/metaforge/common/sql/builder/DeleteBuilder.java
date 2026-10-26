package org.devlive.metaforge.common.sql.builder;

import java.util.List;

public class DeleteBuilder
{
    private static final ThreadLocal<BaseBuilder> localSQL = new ThreadLocal<>();
    private static String symbol = " AND ";

    static {
        BEGIN();
    }

    private DeleteBuilder()
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

    public static void DELETE_FROM(String table)
    {
        sql().DELETE_FROM(table);
    }

    public static void WHERE(String conditions)
    {
        sql().WHERE(conditions);
    }

    public static void WHERE(List<String> conditions)
    {
        sql().WHERE(String.join(symbol, conditions));
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
