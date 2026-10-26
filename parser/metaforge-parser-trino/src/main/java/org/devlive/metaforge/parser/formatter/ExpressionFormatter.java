package org.devlive.metaforge.parser.formatter;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.devlive.metaforge.parser.ast.AstVisitor;
import org.devlive.metaforge.parser.node.Node;
import org.devlive.metaforge.parser.tree.AllColumns;
import org.devlive.metaforge.parser.tree.AllRows;
import org.devlive.metaforge.parser.tree.ArithmeticBinaryExpression;
import org.devlive.metaforge.parser.tree.ArithmeticUnaryExpression;
import org.devlive.metaforge.parser.tree.ArrayConstructor;
import org.devlive.metaforge.parser.tree.AtTimeZone;
import org.devlive.metaforge.parser.tree.BetweenPredicate;
import org.devlive.metaforge.parser.tree.BinaryLiteral;
import org.devlive.metaforge.parser.tree.BindExpression;
import org.devlive.metaforge.parser.tree.BooleanLiteral;
import org.devlive.metaforge.parser.tree.Cast;
import org.devlive.metaforge.parser.tree.CharLiteral;
import org.devlive.metaforge.parser.tree.CoalesceExpression;
import org.devlive.metaforge.parser.tree.ComparisonExpression;
import org.devlive.metaforge.parser.tree.Cube;
import org.devlive.metaforge.parser.tree.CurrentPath;
import org.devlive.metaforge.parser.tree.CurrentTime;
import org.devlive.metaforge.parser.tree.CurrentUser;
import org.devlive.metaforge.parser.tree.DateTimeDataType;
import org.devlive.metaforge.parser.tree.DecimalLiteral;
import org.devlive.metaforge.parser.tree.DereferenceExpression;
import org.devlive.metaforge.parser.tree.DoubleLiteral;
import org.devlive.metaforge.parser.tree.ExistsPredicate;
import org.devlive.metaforge.parser.tree.Expression;
import org.devlive.metaforge.parser.tree.Extract;
import org.devlive.metaforge.parser.tree.FieldReference;
import org.devlive.metaforge.parser.tree.Format;
import org.devlive.metaforge.parser.tree.FrameBound;
import org.devlive.metaforge.parser.tree.FunctionCall;
import org.devlive.metaforge.parser.tree.GenericDataType;
import org.devlive.metaforge.parser.tree.GenericLiteral;
import org.devlive.metaforge.parser.tree.GroupingElement;
import org.devlive.metaforge.parser.tree.GroupingOperation;
import org.devlive.metaforge.parser.tree.GroupingSets;
import org.devlive.metaforge.parser.tree.Identifier;
import org.devlive.metaforge.parser.tree.IfExpression;
import org.devlive.metaforge.parser.tree.InListExpression;
import org.devlive.metaforge.parser.tree.InPredicate;
import org.devlive.metaforge.parser.tree.IntervalDayTimeDataType;
import org.devlive.metaforge.parser.tree.IntervalLiteral;
import org.devlive.metaforge.parser.tree.IsNotNullPredicate;
import org.devlive.metaforge.parser.tree.IsNullPredicate;
import org.devlive.metaforge.parser.tree.LambdaArgumentDeclaration;
import org.devlive.metaforge.parser.tree.LambdaExpression;
import org.devlive.metaforge.parser.tree.LikePredicate;
import org.devlive.metaforge.parser.tree.LogicalBinaryExpression;
import org.devlive.metaforge.parser.tree.LongLiteral;
import org.devlive.metaforge.parser.tree.NotExpression;
import org.devlive.metaforge.parser.tree.NullIfExpression;
import org.devlive.metaforge.parser.tree.NullLiteral;
import org.devlive.metaforge.parser.tree.NumericParameter;
import org.devlive.metaforge.parser.tree.OrderBy;
import org.devlive.metaforge.parser.tree.Parameter;
import org.devlive.metaforge.parser.tree.QuantifiedComparisonExpression;
import org.devlive.metaforge.parser.tree.Rollup;
import org.devlive.metaforge.parser.tree.Row;
import org.devlive.metaforge.parser.tree.RowDataType;
import org.devlive.metaforge.parser.tree.SearchedCaseExpression;
import org.devlive.metaforge.parser.tree.SimpleCaseExpression;
import org.devlive.metaforge.parser.tree.SimpleGroupBy;
import org.devlive.metaforge.parser.tree.SortItem;
import org.devlive.metaforge.parser.tree.StringLiteral;
import org.devlive.metaforge.parser.tree.SubqueryExpression;
import org.devlive.metaforge.parser.tree.SubscriptExpression;
import org.devlive.metaforge.parser.tree.SymbolReference;
import org.devlive.metaforge.parser.tree.TimeLiteral;
import org.devlive.metaforge.parser.tree.TimestampLiteral;
import org.devlive.metaforge.parser.tree.TryExpression;
import org.devlive.metaforge.parser.tree.TypeParameter;
import org.devlive.metaforge.parser.tree.WhenClause;
import org.devlive.metaforge.parser.tree.Window;
import org.devlive.metaforge.parser.tree.WindowFrame;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.PrimitiveIterator;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.devlive.metaforge.parser.formatter.SqlFormatter.formatName;
import static org.devlive.metaforge.parser.formatter.SqlFormatter.formatSql;

public final class ExpressionFormatter
{
    private static final ThreadLocal<DecimalFormat> doubleFormatter = ThreadLocal.withInitial(
            () -> new DecimalFormat("0.###################E0###", new DecimalFormatSymbols(Locale.US)));

    private ExpressionFormatter()
    {
    }

    public static String formatExpression(Expression expression)
    {
        return new Formatter().process(expression, null);
    }

    private static String formatIdentifier(String s)
    {
        return '"' + s.replace("\"", "\"\"") + '"';
    }

    static String formatStringLiteral(String s)
    {
        s = s.replace("'", "''");
        if (CharMatcher.inRange((char) 0x20, (char) 0x7E).matchesAllOf(s)) {
            return "'" + s + "'";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("U&'");
        PrimitiveIterator.OfInt iterator = s.codePoints().iterator();
        while (iterator.hasNext()) {
            int codePoint = iterator.nextInt();
            checkArgument(codePoint >= 0, "Invalid UTF-8 encoding in characters: %s", s);
            if (isAsciiPrintable(codePoint)) {
                char ch = (char) codePoint;
                if (ch == '\\') {
                    builder.append(ch);
                }
                builder.append(ch);
            }
            else if (codePoint <= 0xFFFF) {
                builder.append('\\');
                builder.append(format("%04X", codePoint));
            }
            else {
                builder.append("\\+");
                builder.append(format("%06X", codePoint));
            }
        }
        builder.append("'");
        return builder.toString();
    }

    public static String formatOrderBy(OrderBy orderBy)
    {
        return "ORDER BY " + formatSortItems(orderBy.getSortItems());
    }

    private static String formatSortItems(List<SortItem> sortItems)
    {
        return Joiner.on(", ").join(sortItems.stream()
                .map(sortItemFormatterFunction())
                .iterator());
    }

    static String formatGroupBy(List<GroupingElement> groupingElements)
    {
        ImmutableList.Builder<String> resultStrings = ImmutableList.builder();

        for (GroupingElement groupingElement : groupingElements) {
            String result = "";
            if (groupingElement instanceof SimpleGroupBy) {
                List<Expression> columns = groupingElement.getExpressions();
                if (columns.size() == 1) {
                    result = formatExpression(getOnlyElement(columns));
                }
                else {
                    result = formatGroupingSet(columns);
                }
            }
            else if (groupingElement instanceof GroupingSets) {
                result = format("GROUPING SETS (%s)", Joiner.on(", ").join(
                        ((GroupingSets) groupingElement).getSets().stream()
                                .map(ExpressionFormatter::formatGroupingSet)
                                .iterator()));
            }
            else if (groupingElement instanceof Cube) {
                result = format("CUBE %s", formatGroupingSet(groupingElement.getExpressions()));
            }
            else if (groupingElement instanceof Rollup) {
                result = format("ROLLUP %s", formatGroupingSet(groupingElement.getExpressions()));
            }
            resultStrings.add(result);
        }
        return Joiner.on(", ").join(resultStrings.build());
    }

    private static boolean isAsciiPrintable(int codePoint)
    {
        return codePoint >= 0x20 && codePoint < 0x7F;
    }

    private static String formatGroupingSet(List<Expression> groupingSet)
    {
        return format("(%s)", Joiner.on(", ").join(groupingSet.stream()
                .map(ExpressionFormatter::formatExpression)
                .iterator()));
    }

    private static Function<SortItem, String> sortItemFormatterFunction()
    {
        return input -> {
            StringBuilder builder = new StringBuilder();

            builder.append(formatExpression(input.getSortKey()));

            switch (input.getOrdering()) {
                case ASCENDING:
                    builder.append(" ASC");
                    break;
                case DESCENDING:
                    builder.append(" DESC");
                    break;
                default:
                    throw new UnsupportedOperationException("unknown ordering: " + input.getOrdering());
            }

            switch (input.getNullOrdering()) {
                case FIRST:
                    builder.append(" NULLS FIRST");
                    break;
                case LAST:
                    builder.append(" NULLS LAST");
                    break;
                case UNDEFINED:
                    // no op
                    break;
                default:
                    throw new UnsupportedOperationException("unknown null ordering: " + input.getNullOrdering());
            }

            return builder.toString();
        };
    }

    public static class Formatter
            extends AstVisitor<String, Void>
    {
        @Override
        public String visitNode(Node node, Void context)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String visitRow(Row node, Void context)
        {
            return "ROW (" + Joiner.on(", ").join(node.getItems().stream()
                    .map((child) -> process(child, context))
                    .collect(toList())) + ")";
        }

        @Override
        public String visitExpression(Expression node, Void context)
        {
            throw new UnsupportedOperationException(format("not yet implemented: %s.visit%s", getClass().getName(), node.getClass().getSimpleName()));
        }

        @Override
        public String visitAtTimeZone(AtTimeZone node, Void context)
        {
            return new StringBuilder()
                    .append(process(node.getValue(), context))
                    .append(" AT TIME ZONE ")
                    .append(process(node.getTimeZone(), context)).toString();
        }

        @Override
        public String visitCurrentUser(CurrentUser node, Void context)
        {
            return "CURRENT_USER";
        }

        @Override
        public String visitCurrentPath(CurrentPath node, Void context)
        {
            return "CURRENT_PATH";
        }

        @Override
        public String visitFormat(Format node, Void context)
        {
            return "format(" + joinExpressions(node.getArguments()) + ")";
        }

        @Override
        public String visitCurrentTime(CurrentTime node, Void context)
        {
            StringBuilder builder = new StringBuilder();

            builder.append(node.getFunction().getName());

            if (node.getPrecision() != null) {
                builder.append('(')
                        .append(node.getPrecision())
                        .append(')');
            }

            return builder.toString();
        }

        @Override
        public String visitExtract(Extract node, Void context)
        {
            return "EXTRACT(" + node.getField() + " FROM " + process(node.getExpression(), context) + ")";
        }

        @Override
        public String visitBooleanLiteral(BooleanLiteral node, Void context)
        {
            return String.valueOf(node.getValue());
        }

        @Override
        public String visitStringLiteral(StringLiteral node, Void context)
        {
            return formatStringLiteral(node.getValue());
        }

        @Override
        public String visitCharLiteral(CharLiteral node, Void context)
        {
            return "CHAR " + formatStringLiteral(node.getValue());
        }

        @Override
        public String visitBinaryLiteral(BinaryLiteral node, Void context)
        {
            return "X'" + node.toHexString() + "'";
        }

        @Override
        public String visitParameter(Parameter node, Void context)
        {
            return "?";
        }

        @Override
        public String visitAllRows(AllRows node, Void context)
        {
            return "ALL";
        }

        @Override
        public String visitArrayConstructor(ArrayConstructor node, Void context)
        {
            ImmutableList.Builder<String> valueStrings = ImmutableList.builder();
            for (Expression value : node.getValues()) {
                valueStrings.add(formatSql(value));
            }
            return "ARRAY[" + Joiner.on(",").join(valueStrings.build()) + "]";
        }

        @Override
        public String visitSubscriptExpression(SubscriptExpression node, Void context)
        {
            return formatSql(node.getBase()) + "[" + formatSql(node.getIndex()) + "]";
        }

        @Override
        public String visitLongLiteral(LongLiteral node, Void context)
        {
            return Long.toString(node.getValue());
        }

        @Override
        public String visitDoubleLiteral(DoubleLiteral node, Void context)
        {
            return doubleFormatter.get().format(node.getValue());
        }

        @Override
        public String visitDecimalLiteral(DecimalLiteral node, Void context)
        {
            // TODO return node value without "DECIMAL '..'" when FeaturesConfig#parseDecimalLiteralsAsDouble switch is removed
            return "DECIMAL '" + node.getValue() + "'";
        }

        @Override
        public String visitGenericLiteral(GenericLiteral node, Void context)
        {
            return node.getType() + " " + formatStringLiteral(node.getValue());
        }

        @Override
        public String visitTimeLiteral(TimeLiteral node, Void context)
        {
            return "TIME '" + node.getValue() + "'";
        }

        @Override
        public String visitTimestampLiteral(TimestampLiteral node, Void context)
        {
            return "TIMESTAMP '" + node.getValue() + "'";
        }

        @Override
        public String visitNullLiteral(NullLiteral node, Void context)
        {
            return "null";
        }

        @Override
        public String visitIntervalLiteral(IntervalLiteral node, Void context)
        {
            String sign = (node.getSign() == IntervalLiteral.Sign.NEGATIVE) ? "- " : "";
            StringBuilder builder = new StringBuilder()
                    .append("INTERVAL ")
                    .append(sign)
                    .append(" '").append(node.getValue()).append("' ")
                    .append(node.getStartField());

            if (node.getEndField().isPresent()) {
                builder.append(" TO ").append(node.getEndField().get());
            }
            return builder.toString();
        }

        @Override
        public String visitSubqueryExpression(SubqueryExpression node, Void context)
        {
            return "(" + formatSql(node.getQuery()) + ")";
        }

        @Override
        public String visitExists(ExistsPredicate node, Void context)
        {
            return "(EXISTS " + formatSql(node.getSubquery()) + ")";
        }

        @Override
        public String visitIdentifier(Identifier node, Void context)
        {
            if (!node.isDelimited()) {
                return node.getValue();
            }
            else {
                return '"' + node.getValue().replace("\"", "\"\"") + '"';
            }
        }

        @Override
        public String visitLambdaArgumentDeclaration(LambdaArgumentDeclaration node, Void context)
        {
            return formatExpression(node.getName());
        }

        @Override
        public String visitSymbolReference(SymbolReference node, Void context)
        {
            return formatIdentifier(node.getName());
        }

        @Override
        public String visitDereferenceExpression(DereferenceExpression node, Void context)
        {
            String baseString = process(node.getBase(), context);
            return baseString + "." + process(node.getField());
        }

        @Override
        public String visitFieldReference(FieldReference node, Void context)
        {
            // add colon so this won't parse
            return ":input(" + node.getFieldIndex() + ")";
        }

        @Override
        public String visitFunctionCall(FunctionCall node, Void context)
        {
            StringBuilder builder = new StringBuilder();

            String arguments = joinExpressions(node.getArguments());
            if (node.getArguments().isEmpty() && "count".equalsIgnoreCase(node.getName().getSuffix())) {
                arguments = "*";
            }
            if (node.isDistinct()) {
                arguments = "DISTINCT " + arguments;
            }

            builder.append(formatName(node.getName()))
                    .append('(').append(arguments);

            if (node.getOrderBy().isPresent()) {
                builder.append(' ').append(formatOrderBy(node.getOrderBy().get()));
            }

            builder.append(')');

            node.getNullTreatment().ifPresent(nullTreatment -> {
                switch (nullTreatment) {
                    case IGNORE:
                        builder.append(" IGNORE NULLS");
                        break;
                    case RESPECT:
                        builder.append(" RESPECT NULLS");
                        break;
                }
            });

            if (node.getFilter().isPresent()) {
                builder.append(" FILTER ").append(visitFilter(node.getFilter().get(), context));
            }

            if (node.getWindow().isPresent()) {
                builder.append(" OVER ").append(visitWindow(node.getWindow().get(), context));
            }

            return builder.toString();
        }

        @Override
        public String visitLambdaExpression(LambdaExpression node, Void context)
        {
            StringBuilder builder = new StringBuilder();

            builder.append('(');
            Joiner.on(", ").appendTo(builder, node.getArguments());
            builder.append(") -> ");
            builder.append(process(node.getBody(), context));
            return builder.toString();
        }

        @Override
        public String visitBindExpression(BindExpression node, Void context)
        {
            StringBuilder builder = new StringBuilder();

            builder.append("\"$INTERNAL$BIND\"(");
            for (Expression value : node.getValues()) {
                builder.append(process(value, context))
                        .append(", ");
            }
            builder.append(process(node.getFunction(), context))
                    .append(")");
            return builder.toString();
        }

        @Override
        public String visitLogicalBinaryExpression(LogicalBinaryExpression node, Void context)
        {
            return formatBinaryExpression(node.getOperator().toString(), node.getLeft(), node.getRight());
        }

        @Override
        public String visitNotExpression(NotExpression node, Void context)
        {
            return "(NOT " + process(node.getValue(), context) + ")";
        }

        @Override
        public String visitComparisonExpression(ComparisonExpression node, Void context)
        {
            return formatBinaryExpression(node.getOperator().getValue(), node.getLeft(), node.getRight());
        }

        @Override
        public String visitIsNullPredicate(IsNullPredicate node, Void context)
        {
            return "(" + process(node.getValue(), context) + " IS NULL)";
        }

        @Override
        public String visitIsNotNullPredicate(IsNotNullPredicate node, Void context)
        {
            return "(" + process(node.getValue(), context) + " IS NOT NULL)";
        }

        @Override
        public String visitNullIfExpression(NullIfExpression node, Void context)
        {
            return "NULLIF(" + process(node.getFirst(), context) + ", " + process(node.getSecond(), context) + ')';
        }

        @Override
        public String visitIfExpression(IfExpression node, Void context)
        {
            StringBuilder builder = new StringBuilder();
            builder.append("IF(")
                    .append(process(node.getCondition(), context))
                    .append(", ")
                    .append(process(node.getTrueValue(), context));
            if (node.getFalseValue().isPresent()) {
                builder.append(", ")
                        .append(process(node.getFalseValue().get(), context));
            }
            builder.append(")");
            return builder.toString();
        }

        @Override
        public String visitTryExpression(TryExpression node, Void context)
        {
            return "TRY(" + process(node.getInnerExpression(), context) + ")";
        }

        @Override
        public String visitCoalesceExpression(CoalesceExpression node, Void context)
        {
            return "COALESCE(" + joinExpressions(node.getOperands()) + ")";
        }

        @Override
        public String visitArithmeticUnary(ArithmeticUnaryExpression node, Void context)
        {
            String value = process(node.getValue(), context);

            switch (node.getSign()) {
                case MINUS:
                    // Unary is ambiguous with respect to negative numbers. "-1" parses as a number, but "-(1)" parses as "unaryMinus(number)"
                    // The parentheses are needed to ensure the parsing roundtrips properly.
                    return "-(" + value + ")";
                case PLUS:
                    return "+" + value;
                default:
                    throw new UnsupportedOperationException("Unsupported sign: " + node.getSign());
            }
        }

        @Override
        public String visitArithmeticBinary(ArithmeticBinaryExpression node, Void context)
        {
            return formatBinaryExpression(node.getOperator().getValue(), node.getLeft(), node.getRight());
        }

        @Override
        public String visitLikePredicate(LikePredicate node, Void context)
        {
            StringBuilder builder = new StringBuilder();

            builder.append('(')
                    .append(process(node.getValue(), context))
                    .append(" LIKE ")
                    .append(process(node.getPattern(), context));

            node.getEscape().ifPresent(escape -> builder.append(" ESCAPE ")
                    .append(process(escape, context)));

            builder.append(')');

            return builder.toString();
        }

        @Override
        public String visitAllColumns(AllColumns node, Void context)
        {
            StringBuilder builder = new StringBuilder();
            if (node.getTarget().isPresent()) {
                builder.append(process(node.getTarget().get(), context));
                builder.append(".*");
            }
            else {
                builder.append("*");
            }

            if (!node.getAliases().isEmpty()) {
                builder.append(" AS (");
                Joiner.on(", ").appendTo(builder, node.getAliases().stream()
                        .map(alias -> process(alias, context))
                        .collect(toList()));
                builder.append(")");
            }

            return builder.toString();
        }

        @Override
        public String visitCast(Cast node, Void context)
        {
            return (node.isSafe() ? "TRY_CAST" : "CAST") +
                    "(" + process(node.getExpression(), context) + " AS " + process(node.getType(), context) + ")";
        }

        @Override
        public String visitSearchedCaseExpression(SearchedCaseExpression node, Void context)
        {
            ImmutableList.Builder<String> parts = ImmutableList.builder();
            parts.add("CASE");
            for (WhenClause whenClause : node.getWhenClauses()) {
                parts.add(process(whenClause, context));
            }

            node.getDefaultValue()
                    .ifPresent((value) -> parts.add("ELSE").add(process(value, context)));

            parts.add("END");

            return "(" + Joiner.on(' ').join(parts.build()) + ")";
        }

        @Override
        public String visitSimpleCaseExpression(SimpleCaseExpression node, Void context)
        {
            ImmutableList.Builder<String> parts = ImmutableList.builder();

            parts.add("CASE")
                    .add(process(node.getOperand(), context));

            for (WhenClause whenClause : node.getWhenClauses()) {
                parts.add(process(whenClause, context));
            }

            node.getDefaultValue()
                    .ifPresent((value) -> parts.add("ELSE").add(process(value, context)));

            parts.add("END");

            return "(" + Joiner.on(' ').join(parts.build()) + ")";
        }

        @Override
        public String visitWhenClause(WhenClause node, Void context)
        {
            return "WHEN " + process(node.getOperand(), context) + " THEN " + process(node.getResult(), context);
        }

        @Override
        public String visitBetweenPredicate(BetweenPredicate node, Void context)
        {
            return "(" + process(node.getValue(), context) + " BETWEEN " +
                    process(node.getMin(), context) + " AND " + process(node.getMax(), context) + ")";
        }

        @Override
        public String visitInPredicate(InPredicate node, Void context)
        {
            return "(" + process(node.getValue(), context) + " IN " + process(node.getValueList(), context) + ")";
        }

        @Override
        public String visitInListExpression(InListExpression node, Void context)
        {
            return "(" + joinExpressions(node.getValues()) + ")";
        }

        private String visitFilter(Expression node, Void context)
        {
            return "(WHERE " + process(node, context) + ')';
        }

        @Override
        public String visitWindow(Window node, Void context)
        {
            List<String> parts = new ArrayList<>();

            if (!node.getPartitionBy().isEmpty()) {
                parts.add("PARTITION BY " + joinExpressions(node.getPartitionBy()));
            }
            if (node.getOrderBy().isPresent()) {
                parts.add(formatOrderBy(node.getOrderBy().get()));
            }
            if (node.getFrame().isPresent()) {
                parts.add(process(node.getFrame().get(), context));
            }

            return '(' + Joiner.on(' ').join(parts) + ')';
        }

        @Override
        public String visitWindowFrame(WindowFrame node, Void context)
        {
            StringBuilder builder = new StringBuilder();

            builder.append(node.getType().toString()).append(' ');

            if (node.getEnd().isPresent()) {
                builder.append("BETWEEN ")
                        .append(process(node.getStart(), context))
                        .append(" AND ")
                        .append(process(node.getEnd().get(), context));
            }
            else {
                builder.append(process(node.getStart(), context));
            }

            return builder.toString();
        }

        @Override
        public String visitFrameBound(FrameBound node, Void context)
        {
            switch (node.getType()) {
                case UNBOUNDED_PRECEDING:
                    return "UNBOUNDED PRECEDING";
                case PRECEDING:
                    return process(node.getValue().get(), context) + " PRECEDING";
                case CURRENT_ROW:
                    return "CURRENT ROW";
                case FOLLOWING:
                    return process(node.getValue().get(), context) + " FOLLOWING";
                case UNBOUNDED_FOLLOWING:
                    return "UNBOUNDED FOLLOWING";
            }
            throw new IllegalArgumentException("unhandled type: " + node.getType());
        }

        @Override
        public String visitQuantifiedComparisonExpression(QuantifiedComparisonExpression node, Void context)
        {
            return new StringBuilder()
                    .append("(")
                    .append(process(node.getValue(), context))
                    .append(' ')
                    .append(node.getOperator().getValue())
                    .append(' ')
                    .append(node.getQuantifier().toString())
                    .append(' ')
                    .append(process(node.getSubquery(), context))
                    .append(")")
                    .toString();
        }

        @Override
        public String visitGroupingOperation(GroupingOperation node, Void context)
        {
            return "GROUPING (" + joinExpressions(node.getGroupingColumns()) + ")";
        }

        @Override
        public String visitRowDataType(RowDataType node, Void context)
        {
            return node.getFields().stream()
                    .map(this::process)
                    .collect(Collectors.joining(", ", "ROW(", ")"));
        }

        @Override
        public String visitRowField(RowDataType.Field node, Void context)
        {
            StringBuilder result = new StringBuilder();

            if (node.getName().isPresent()) {
                result.append(process(node.getName().get(), context));
                result.append(" ");
            }

            result.append(process(node.getType(), context));

            return result.toString();
        }

        @Override
        public String visitGenericDataType(GenericDataType node, Void context)
        {
            StringBuilder result = new StringBuilder();
            result.append(node.getName());

            if (!node.getArguments().isEmpty()) {
                result.append(node.getArguments().stream()
                        .map(this::process)
                        .collect(Collectors.joining(", ", "(", ")")));
            }

            return result.toString();
        }

        @Override
        public String visitTypeParameter(TypeParameter node, Void context)
        {
            return process(node.getValue(), context);
        }

        @Override
        public String visitNumericTypeParameter(NumericParameter node, Void context)
        {
            return node.getValue();
        }

        @Override
        public String visitIntervalDataType(IntervalDayTimeDataType node, Void context)
        {
            StringBuilder builder = new StringBuilder();

            builder.append("INTERVAL ");
            builder.append(node.getFrom());
            if (node.getFrom() != node.getTo()) {
                builder.append(" TO ")
                        .append(node.getTo());
            }

            return builder.toString();
        }

        @Override
        public String visitDateTimeType(DateTimeDataType node, Void context)
        {
            StringBuilder builder = new StringBuilder();

            builder.append(node.getType().toString().toLowerCase(Locale.ENGLISH)); // TODO: normalize to upper case according to standard SQL semantics
            if (node.getPrecision().isPresent()) {
                builder.append("(")
                        .append(node.getPrecision().get())
                        .append(")");
            }

            if (node.isWithTimeZone()) {
                builder.append(" with time zone"); // TODO: normalize to upper case according to standard SQL semantics
            }

            return builder.toString();
        }

        private String formatBinaryExpression(String operator, Expression left, Expression right)
        {
            return '(' + process(left, null) + ' ' + operator + ' ' + process(right, null) + ')';
        }

        private String joinExpressions(List<Expression> expressions)
        {
            return Joiner.on(", ").join(expressions.stream()
                    .map((e) -> process(e, null))
                    .iterator());
        }
    }
}
