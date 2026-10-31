package org.devlive.metaforge.driver.sql;

import org.devlive.metaforge.sql.node.Expression;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Evaluates a WHERE clause on a row given as column name to value. */
public final class Predicates
{
    private Predicates()
    {
    }

    public static Predicate<Map<String, Object>> of(Expression where)
            throws SQLException
    {
        if (where == null) {
            return row -> true;
        }
        check(where);
        return row -> Boolean.TRUE.equals(evaluate(where, row));
    }

    /** Refuses what the evaluator does not know before any row is read. */
    private static void check(Expression expression)
            throws SQLException
    {
        if (expression == null) {
            throw new SQLException("Unsupported condition");
        }
        if (expression.getType() == Expression.ExpressionType.FUNCTION || expression.getType() == Expression.ExpressionType.FUNCTION_CALL) {
            throw new SQLException("Functions are not supported in conditions: " + expression.getValue());
        }
        if (expression.getChildren() != null) {
            for (Expression child : expression.getChildren()) {
                check(child);
            }
        }
    }

    private static Object evaluate(Expression expression, Map<String, Object> row)
    {
        List<Expression> children = expression.getChildren();
        switch (expression.getType()) {
            case LITERAL:
                return Names.literal(expression.getValue());
            case COLUMN_REFERENCE:
                return row.get(column(String.valueOf(expression.getValue())));
            case UNARY_OP:
                Object operand = evaluate(children.get(0), row);
                switch (String.valueOf(expression.getValue())) {
                    case "NOT":
                        return operand == null ? null : !Boolean.TRUE.equals(operand);
                    case "IS NULL":
                        return operand == null;
                    case "IS NOT NULL":
                        return operand != null;
                    default:
                        return null;
                }
            case BINARY_OP:
                String operator = String.valueOf(expression.getValue()).toUpperCase();
                if (operator.equals("AND")) {
                    return Boolean.TRUE.equals(evaluate(children.get(0), row)) && Boolean.TRUE.equals(evaluate(children.get(1), row));
                }
                if (operator.equals("OR")) {
                    return Boolean.TRUE.equals(evaluate(children.get(0), row)) || Boolean.TRUE.equals(evaluate(children.get(1), row));
                }
                Object left = evaluate(children.get(0), row);
                if (operator.equals("IN") || operator.equals("NOT IN")) {
                    boolean found = false;
                    for (int i = 1; i < children.size(); i++) {
                        found |= compare(left, evaluate(children.get(i), row)) == 0;
                    }
                    return operator.equals("IN") == found;
                }
                Object right = evaluate(children.get(1), row);
                if (left == null || right == null) {
                    return null;
                }
                switch (operator) {
                    case "LIKE":
                        return like(String.valueOf(left), String.valueOf(right));
                    case "NOT LIKE":
                        return !like(String.valueOf(left), String.valueOf(right));
                    case "=":
                    case "<=>":
                        return compare(left, right) == 0;
                    case "<>":
                    case "!=":
                        return compare(left, right) != 0;
                    case ">":
                        return compare(left, right) > 0;
                    case ">=":
                        return compare(left, right) >= 0;
                    case "<":
                        return compare(left, right) < 0;
                    case "<=":
                        return compare(left, right) <= 0;
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /** A column reference without its table prefix or quotes. */
    public static String column(String reference)
    {
        int dot = reference.lastIndexOf('.');
        return Names.unquote(dot >= 0 ? reference.substring(dot + 1) : reference);
    }

    /** Numbers compare as numbers, everything else as text. */
    public static int compare(Object left, Object right)
    {
        if (left == null || right == null) {
            return Objects.equals(left, right) ? 0 : left == null ? -1 : 1;
        }
        BigDecimal a = number(left);
        BigDecimal b = number(right);
        if (a != null && b != null) {
            return a.compareTo(b);
        }
        return String.valueOf(left).compareTo(String.valueOf(right));
    }

    private static BigDecimal number(Object value)
    {
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        }
        catch (NumberFormatException error) {
            return null;
        }
    }

    private static boolean like(String value, String pattern)
    {
        StringBuilder regex = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            if (c == '%') {
                regex.append(".*");
            }
            else if (c == '_') {
                regex.append('.');
            }
            else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(regex.toString(), Pattern.DOTALL).matcher(value).matches();
    }
}
