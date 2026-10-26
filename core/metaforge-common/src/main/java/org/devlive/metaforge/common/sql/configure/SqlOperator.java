package org.devlive.metaforge.common.sql.configure;

public enum SqlOperator
{
    EQ("="),
    NEQ("<>"),
    GT(">"),
    GTE(">="),
    LT("<"),
    LTE("<="),
    LIKE("LIKE"),
    NLIKE("NOT LIKE"),
    NULL("IS NULL"),
    NNULL("IS NOT NULL");

    private final String symbol;

    SqlOperator(String symbol)
    {
        this.symbol = symbol;
    }

    public String getSymbol()
    {
        return symbol;
    }
}
