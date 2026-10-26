package org.devlive.metaforge.condor.condition;

import org.devlive.metaforge.condor.ComparisonOperator;
import org.devlive.metaforge.condor.metadata.RowDefinition;

public class SimpleCondition
        implements Condition
{
    private final String columnName;
    private final Object value;
    private final ComparisonOperator operator;

    public SimpleCondition(String columnName, Object value, ComparisonOperator operator)
    {
        this.columnName = columnName;
        this.value = value;
        this.operator = operator;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean evaluate(RowDefinition row)
    {
        Object rowValue = row.getValue(columnName);
        if (rowValue == null || value == null) {
            return operator == ComparisonOperator.EQUALS && rowValue == value;
        }

        switch (operator) {
            case EQUALS:
                return value.equals(rowValue);
            case NOT_EQUALS:
                return !value.equals(rowValue);
            case GREATER_THAN:
                return compareValues(rowValue, value) > 0;
            case GREATER_THAN_OR_EQUALS:
                return compareValues(rowValue, value) >= 0;
            case LESS_THAN:
                return compareValues(rowValue, value) < 0;
            case LESS_THAN_OR_EQUALS:
                return compareValues(rowValue, value) <= 0;
            default:
                return false;
        }
    }

    @SuppressWarnings("unchecked")
    private int compareValues(Object left, Object right)
    {
        if (left instanceof Comparable && right instanceof Comparable) {
            if (left.getClass() == right.getClass()) {
                return ((Comparable<Object>) left).compareTo(right);
            }
            if (left instanceof Number && right instanceof Number) {
                return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue());
            }
        }
        return left.toString().compareTo(right.toString());
    }
}
