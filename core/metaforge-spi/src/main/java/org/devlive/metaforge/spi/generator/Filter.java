package org.devlive.metaforge.spi.generator;

public class Filter
{
    private final String column;
    private final Operator operator;
    private final Object value;

    public enum Operator
    {
        EQ("="),
        NE("!="),
        GT(">"),
        GE(">="),
        LT("<"),
        LE("<="),
        LIKE("LIKE"),
        IN("IN"),
        NOT_IN("NOT IN"),
        IS_NULL("IS NULL"),
        IS_NOT_NULL("IS NOT NULL");

        private final String symbol;

        Operator(String symbol)
        {
            this.symbol = symbol;
        }

        @Override
        public String toString()
        {
            return symbol;
        }
    }

    private Filter(String column, Operator operator, Object value)
    {
        this.column = column;
        this.operator = operator;
        this.value = value;
    }

    public static Filter create(String column, Operator operator, Object value)
    {
        return new Filter(column, operator, value);
    }

    public static Filter create(String column, Operator operator)
    {
        return new Filter(column, operator, null);
    }

    public String build()
    {
        StringBuilder condition = new StringBuilder();
        condition.append("`").append(column).append("` ");

        if (operator == Operator.IS_NULL || operator == Operator.IS_NOT_NULL) {
            condition.append(operator);
        }
        else if (operator == Operator.IN || operator == Operator.NOT_IN) {
            condition.append(operator).append(" (")
                    .append(formatValue(value))
                    .append(")");
        }
        else {
            condition.append(operator).append(" ")
                    .append(formatValue(value));
        }

        return condition.toString();
    }

    private Object formatValue(Object value)
    {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return "'" + ((String) value).replace("'", "''") + "'";
        }
        if (value instanceof Object[]) {
            return String.join(", ", formatArrayValues((Object[]) value));
        }
        return value;
    }

    private String[] formatArrayValues(Object[] values)
    {
        String[] formattedValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof String) {
                formattedValues[i] = "'" + ((String) values[i]).replace("'", "''") + "'";
            }
            else {
                formattedValues[i] = String.valueOf(values[i]);
            }
        }
        return formattedValues;
    }
}
