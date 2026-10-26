package org.devlive.metaforge.parser.ast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.devlive.metaforge.parser.ParsingException;
import org.devlive.metaforge.parser.antlr.SqlBaseBaseVisitor;
import org.devlive.metaforge.parser.antlr.SqlBaseLexer;
import org.devlive.metaforge.parser.antlr.SqlBaseParser;
import org.devlive.metaforge.parser.node.Node;
import org.devlive.metaforge.parser.node.NodeLocation;
import org.devlive.metaforge.parser.parser.ParserOptions;
import org.devlive.metaforge.parser.tree.AddColumn;
import org.devlive.metaforge.parser.tree.AliasedRelation;
import org.devlive.metaforge.parser.tree.AllColumns;
import org.devlive.metaforge.parser.tree.AllRows;
import org.devlive.metaforge.parser.tree.Analyze;
import org.devlive.metaforge.parser.tree.ArithmeticBinaryExpression;
import org.devlive.metaforge.parser.tree.ArithmeticUnaryExpression;
import org.devlive.metaforge.parser.tree.ArrayConstructor;
import org.devlive.metaforge.parser.tree.AtTimeZone;
import org.devlive.metaforge.parser.tree.BetweenPredicate;
import org.devlive.metaforge.parser.tree.BinaryLiteral;
import org.devlive.metaforge.parser.tree.BindExpression;
import org.devlive.metaforge.parser.tree.BooleanLiteral;
import org.devlive.metaforge.parser.tree.Call;
import org.devlive.metaforge.parser.tree.CallArgument;
import org.devlive.metaforge.parser.tree.Cast;
import org.devlive.metaforge.parser.tree.CharLiteral;
import org.devlive.metaforge.parser.tree.CoalesceExpression;
import org.devlive.metaforge.parser.tree.ColumnDefinition;
import org.devlive.metaforge.parser.tree.Comment;
import org.devlive.metaforge.parser.tree.Commit;
import org.devlive.metaforge.parser.tree.ComparisonExpression;
import org.devlive.metaforge.parser.tree.CreateRole;
import org.devlive.metaforge.parser.tree.CreateSchema;
import org.devlive.metaforge.parser.tree.CreateTable;
import org.devlive.metaforge.parser.tree.CreateTableAsSelect;
import org.devlive.metaforge.parser.tree.CreateView;
import org.devlive.metaforge.parser.tree.Cube;
import org.devlive.metaforge.parser.tree.CurrentPath;
import org.devlive.metaforge.parser.tree.CurrentTime;
import org.devlive.metaforge.parser.tree.CurrentUser;
import org.devlive.metaforge.parser.tree.DataType;
import org.devlive.metaforge.parser.tree.DataTypeParameter;
import org.devlive.metaforge.parser.tree.DateTimeDataType;
import org.devlive.metaforge.parser.tree.Deallocate;
import org.devlive.metaforge.parser.tree.DecimalLiteral;
import org.devlive.metaforge.parser.tree.Delete;
import org.devlive.metaforge.parser.tree.DereferenceExpression;
import org.devlive.metaforge.parser.tree.DescribeInput;
import org.devlive.metaforge.parser.tree.DescribeOutput;
import org.devlive.metaforge.parser.tree.DoubleLiteral;
import org.devlive.metaforge.parser.tree.DropColumn;
import org.devlive.metaforge.parser.tree.DropRole;
import org.devlive.metaforge.parser.tree.DropSchema;
import org.devlive.metaforge.parser.tree.DropTable;
import org.devlive.metaforge.parser.tree.DropView;
import org.devlive.metaforge.parser.tree.Except;
import org.devlive.metaforge.parser.tree.Execute;
import org.devlive.metaforge.parser.tree.ExistsPredicate;
import org.devlive.metaforge.parser.tree.Explain;
import org.devlive.metaforge.parser.tree.ExplainFormat;
import org.devlive.metaforge.parser.tree.ExplainOption;
import org.devlive.metaforge.parser.tree.ExplainType;
import org.devlive.metaforge.parser.tree.Expression;
import org.devlive.metaforge.parser.tree.Extract;
import org.devlive.metaforge.parser.tree.FetchFirst;
import org.devlive.metaforge.parser.tree.Format;
import org.devlive.metaforge.parser.tree.FrameBound;
import org.devlive.metaforge.parser.tree.FunctionCall.NullTreatment;
import org.devlive.metaforge.parser.tree.FunctionCall;
import org.devlive.metaforge.parser.tree.GenericDataType;
import org.devlive.metaforge.parser.tree.GenericLiteral;
import org.devlive.metaforge.parser.tree.Grant;
import org.devlive.metaforge.parser.tree.GrantRoles;
import org.devlive.metaforge.parser.tree.GrantorSpecification;
import org.devlive.metaforge.parser.tree.GroupBy;
import org.devlive.metaforge.parser.tree.GroupingElement;
import org.devlive.metaforge.parser.tree.GroupingOperation;
import org.devlive.metaforge.parser.tree.GroupingSets;
import org.devlive.metaforge.parser.tree.Identifier;
import org.devlive.metaforge.parser.tree.IfExpression;
import org.devlive.metaforge.parser.tree.InListExpression;
import org.devlive.metaforge.parser.tree.InPredicate;
import org.devlive.metaforge.parser.tree.Insert;
import org.devlive.metaforge.parser.tree.Intersect;
import org.devlive.metaforge.parser.tree.IntervalDayTimeDataType;
import org.devlive.metaforge.parser.tree.IntervalLiteral;
import org.devlive.metaforge.parser.tree.IsNotNullPredicate;
import org.devlive.metaforge.parser.tree.IsNullPredicate;
import org.devlive.metaforge.parser.tree.Isolation;
import org.devlive.metaforge.parser.tree.LambdaArgumentDeclaration;
import org.devlive.metaforge.parser.tree.LambdaExpression;
import org.devlive.metaforge.parser.tree.Lateral;
import org.devlive.metaforge.parser.tree.LikeClause;
import org.devlive.metaforge.parser.tree.LikePredicate;
import org.devlive.metaforge.parser.tree.Limit;
import org.devlive.metaforge.parser.tree.LogicalBinaryExpression;
import org.devlive.metaforge.parser.tree.LongLiteral;
import org.devlive.metaforge.parser.tree.NaturalJoin;
import org.devlive.metaforge.parser.tree.NotExpression;
import org.devlive.metaforge.parser.tree.NullIfExpression;
import org.devlive.metaforge.parser.tree.NullLiteral;
import org.devlive.metaforge.parser.tree.NumericParameter;
import org.devlive.metaforge.parser.tree.Offset;
import org.devlive.metaforge.parser.tree.OrderBy;
import org.devlive.metaforge.parser.tree.Parameter;
import org.devlive.metaforge.parser.tree.PathElement;
import org.devlive.metaforge.parser.tree.PathSpecification;
import org.devlive.metaforge.parser.tree.Prepare;
import org.devlive.metaforge.parser.tree.PrincipalSpecification;
import org.devlive.metaforge.parser.tree.Property;
import org.devlive.metaforge.parser.tree.QualifiedName;
import org.devlive.metaforge.parser.tree.QuantifiedComparisonExpression;
import org.devlive.metaforge.parser.tree.Relation;
import org.devlive.metaforge.parser.tree.RenameColumn;
import org.devlive.metaforge.parser.tree.RenameSchema;
import org.devlive.metaforge.parser.tree.RenameTable;
import org.devlive.metaforge.parser.tree.RenameView;
import org.devlive.metaforge.parser.tree.ResetSession;
import org.devlive.metaforge.parser.tree.Revoke;
import org.devlive.metaforge.parser.tree.RevokeRoles;
import org.devlive.metaforge.parser.tree.Rollback;
import org.devlive.metaforge.parser.tree.Rollup;
import org.devlive.metaforge.parser.tree.Row;
import org.devlive.metaforge.parser.tree.RowDataType;
import org.devlive.metaforge.parser.tree.SampledRelation;
import org.devlive.metaforge.parser.tree.SearchedCaseExpression;
import org.devlive.metaforge.parser.tree.Select;
import org.devlive.metaforge.parser.tree.SelectItem;
import org.devlive.metaforge.parser.tree.SetPath;
import org.devlive.metaforge.parser.tree.SetRole;
import org.devlive.metaforge.parser.tree.SetSchemaAuthorization;
import org.devlive.metaforge.parser.tree.SetSession;
import org.devlive.metaforge.parser.tree.ShowCatalogs;
import org.devlive.metaforge.parser.tree.ShowColumns;
import org.devlive.metaforge.parser.tree.ShowCreate;
import org.devlive.metaforge.parser.tree.ShowFunctions;
import org.devlive.metaforge.parser.tree.ShowGrants;
import org.devlive.metaforge.parser.tree.ShowRoleGrants;
import org.devlive.metaforge.parser.tree.ShowRoles;
import org.devlive.metaforge.parser.tree.ShowSchemas;
import org.devlive.metaforge.parser.tree.ShowSession;
import org.devlive.metaforge.parser.tree.ShowStats;
import org.devlive.metaforge.parser.tree.ShowTables;
import org.devlive.metaforge.parser.tree.SimpleCaseExpression;
import org.devlive.metaforge.parser.tree.SimpleGroupBy;
import org.devlive.metaforge.parser.tree.SingleColumn;
import org.devlive.metaforge.parser.tree.SortItem;
import org.devlive.metaforge.parser.tree.StartTransaction;
import org.devlive.metaforge.parser.tree.Statement;
import org.devlive.metaforge.parser.tree.StringLiteral;
import org.devlive.metaforge.parser.tree.SubqueryExpression;
import org.devlive.metaforge.parser.tree.SubscriptExpression;
import org.devlive.metaforge.parser.tree.TimeLiteral;
import org.devlive.metaforge.parser.tree.TimestampLiteral;
import org.devlive.metaforge.parser.tree.TransactionAccessMode;
import org.devlive.metaforge.parser.tree.TransactionMode;
import org.devlive.metaforge.parser.tree.TryExpression;
import org.devlive.metaforge.parser.tree.TypeParameter;
import org.devlive.metaforge.parser.tree.Union;
import org.devlive.metaforge.parser.tree.Unnest;
import org.devlive.metaforge.parser.tree.Use;
import org.devlive.metaforge.parser.tree.Values;
import org.devlive.metaforge.parser.tree.WhenClause;
import org.devlive.metaforge.parser.tree.Window;
import org.devlive.metaforge.parser.tree.WindowFrame;
import org.devlive.metaforge.parser.tree.With;
import org.devlive.metaforge.parser.tree.WithQuery;
import org.devlive.metaforge.parser.tree.join.Join;
import org.devlive.metaforge.parser.tree.join.JoinCriteria;
import org.devlive.metaforge.parser.tree.join.JoinOn;
import org.devlive.metaforge.parser.tree.join.JoinUsing;
import org.devlive.metaforge.parser.tree.query.Query;
import org.devlive.metaforge.parser.tree.query.QueryBody;
import org.devlive.metaforge.parser.tree.query.QuerySpecification;
import org.devlive.metaforge.parser.tree.table.Table;
import org.devlive.metaforge.parser.tree.table.TableElement;
import org.devlive.metaforge.parser.tree.table.TableSubQuery;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class AstVisitorBuilder
        extends SqlBaseBaseVisitor<Node>
{
    private final ParserOptions parserOptions;
    private int parameterPosition;

    public AstVisitorBuilder(ParserOptions parserOptions)
    {
        this.parserOptions = requireNonNull(parserOptions, "parsingOptions is null");
    }

    private static String decodeUnicodeLiteral(SqlBaseParser.UnicodeStringLiteralContext context)
    {
        char escape;
        if (context.UESCAPE() != null) {
            String escapeString = unquote(context.STRING().getText());
            check(!escapeString.isEmpty(), "Empty Unicode escape character", context);
            check(escapeString.length() == 1, "Invalid Unicode escape character: " + escapeString, context);
            escape = escapeString.charAt(0);
            check(isValidUnicodeEscape(escape), "Invalid Unicode escape character: " + escapeString, context);
        }
        else {
            escape = '\\';
        }

        String rawContent = unquote(context.UNICODE_STRING().getText().substring(2));
        StringBuilder unicodeStringBuilder = new StringBuilder();
        StringBuilder escapedCharacterBuilder = new StringBuilder();
        int charactersNeeded = 0;
        UnicodeDecodeState state = UnicodeDecodeState.EMPTY;
        for (int i = 0; i < rawContent.length(); i++) {
            char ch = rawContent.charAt(i);
            switch (state) {
                case EMPTY:
                    if (ch == escape) {
                        state = UnicodeDecodeState.ESCAPED;
                    }
                    else {
                        unicodeStringBuilder.append(ch);
                    }
                    break;
                case ESCAPED:
                    if (ch == escape) {
                        unicodeStringBuilder.append(escape);
                        state = UnicodeDecodeState.EMPTY;
                    }
                    else if (ch == '+') {
                        state = UnicodeDecodeState.UNICODE_SEQUENCE;
                        charactersNeeded = 6;
                    }
                    else if (isHexDigit(ch)) {
                        state = UnicodeDecodeState.UNICODE_SEQUENCE;
                        charactersNeeded = 4;
                        escapedCharacterBuilder.append(ch);
                    }
                    else {
                        throw parseError("Invalid hexadecimal digit: " + ch, context);
                    }
                    break;
                case UNICODE_SEQUENCE:
                    check(isHexDigit(ch), "Incomplete escape sequence: " + escapedCharacterBuilder.toString(), context);
                    escapedCharacterBuilder.append(ch);
                    if (charactersNeeded == escapedCharacterBuilder.length()) {
                        String currentEscapedCode = escapedCharacterBuilder.toString();
                        escapedCharacterBuilder.setLength(0);
                        int codePoint = Integer.parseInt(currentEscapedCode, 16);
                        check(Character.isValidCodePoint(codePoint), "Invalid escaped character: " + currentEscapedCode, context);
                        if (Character.isSupplementaryCodePoint(codePoint)) {
                            unicodeStringBuilder.appendCodePoint(codePoint);
                        }
                        else {
                            char currentCodePoint = (char) codePoint;
                            check(!Character.isSurrogate(currentCodePoint), format("Invalid escaped character: %s. Escaped character is a surrogate. Use '\\+123456' instead.", currentEscapedCode), context);
                            unicodeStringBuilder.append(currentCodePoint);
                        }
                        state = UnicodeDecodeState.EMPTY;
                        charactersNeeded = -1;
                    }
                    else {
                        check(charactersNeeded > escapedCharacterBuilder.length(), "Unexpected escape sequence length: " + escapedCharacterBuilder.length(), context);
                    }
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        check(state == UnicodeDecodeState.EMPTY, "Incomplete escape sequence: " + escapedCharacterBuilder.toString(), context);
        return unicodeStringBuilder.toString();
    }

    private static String unquote(String value)
    {
        return value.substring(1, value.length() - 1)
                .replace("''", "'");
    }

    private static LikeClause.PropertiesOption getPropertiesOption(Token token)
    {
        switch (token.getType()) {
            case SqlBaseLexer.INCLUDING:
                return LikeClause.PropertiesOption.INCLUDING;
            case SqlBaseLexer.EXCLUDING:
                return LikeClause.PropertiesOption.EXCLUDING;
        }
        throw new IllegalArgumentException("Unsupported LIKE option type: " + token.getText());
    }

    private static boolean isDistinct(SqlBaseParser.SetQuantifierContext setQuantifier)
    {
        return setQuantifier != null && setQuantifier.DISTINCT() != null;
    }

    // ******************* statements **********************

    private static boolean isHexDigit(char c)
    {
        return ((c >= '0') && (c <= '9')) ||
                ((c >= 'A') && (c <= 'F')) ||
                ((c >= 'a') && (c <= 'f'));
    }

    private static boolean isValidUnicodeEscape(char c)
    {
        return c < 0x7F && c > 0x20 && !isHexDigit(c) && c != '"' && c != '+' && c != '\'';
    }

    private static Optional<String> getTextIfPresent(ParserRuleContext context)
    {
        return Optional.ofNullable(context)
                .map(ParseTree::getText);
    }

    private static Optional<String> getTextIfPresent(Token token)
    {
        return Optional.ofNullable(token)
                .map(Token::getText);
    }

    private static ArithmeticBinaryExpression.Operator getArithmeticBinaryOperator(Token operator)
    {
        switch (operator.getType()) {
            case SqlBaseLexer.PLUS:
                return ArithmeticBinaryExpression.Operator.ADD;
            case SqlBaseLexer.MINUS:
                return ArithmeticBinaryExpression.Operator.SUBTRACT;
            case SqlBaseLexer.ASTERISK:
                return ArithmeticBinaryExpression.Operator.MULTIPLY;
            case SqlBaseLexer.SLASH:
                return ArithmeticBinaryExpression.Operator.DIVIDE;
            case SqlBaseLexer.PERCENT:
                return ArithmeticBinaryExpression.Operator.MODULUS;
        }

        throw new UnsupportedOperationException("Unsupported operator: " + operator.getText());
    }

    private static ComparisonExpression.Operator getComparisonOperator(Token symbol)
    {
        switch (symbol.getType()) {
            case SqlBaseLexer.EQ:
                return ComparisonExpression.Operator.EQUAL;
            case SqlBaseLexer.NEQ:
                return ComparisonExpression.Operator.NOT_EQUAL;
            case SqlBaseLexer.LT:
                return ComparisonExpression.Operator.LESS_THAN;
            case SqlBaseLexer.LTE:
                return ComparisonExpression.Operator.LESS_THAN_OR_EQUAL;
            case SqlBaseLexer.GT:
                return ComparisonExpression.Operator.GREATER_THAN;
            case SqlBaseLexer.GTE:
                return ComparisonExpression.Operator.GREATER_THAN_OR_EQUAL;
        }

        throw new IllegalArgumentException("Unsupported operator: " + symbol.getText());
    }

    private static CurrentTime.Function getDateTimeFunctionType(Token token)
    {
        switch (token.getType()) {
            case SqlBaseLexer.CURRENT_DATE:
                return CurrentTime.Function.DATE;
            case SqlBaseLexer.CURRENT_TIME:
                return CurrentTime.Function.TIME;
            case SqlBaseLexer.CURRENT_TIMESTAMP:
                return CurrentTime.Function.TIMESTAMP;
            case SqlBaseLexer.LOCALTIME:
                return CurrentTime.Function.LOCALTIME;
            case SqlBaseLexer.LOCALTIMESTAMP:
                return CurrentTime.Function.LOCALTIMESTAMP;
        }

        throw new IllegalArgumentException("Unsupported special function: " + token.getText());
    }

    private static IntervalLiteral.IntervalField getIntervalFieldType(Token token)
    {
        switch (token.getType()) {
            case SqlBaseLexer.YEAR:
                return IntervalLiteral.IntervalField.YEAR;
            case SqlBaseLexer.MONTH:
                return IntervalLiteral.IntervalField.MONTH;
            case SqlBaseLexer.DAY:
                return IntervalLiteral.IntervalField.DAY;
            case SqlBaseLexer.HOUR:
                return IntervalLiteral.IntervalField.HOUR;
            case SqlBaseLexer.MINUTE:
                return IntervalLiteral.IntervalField.MINUTE;
            case SqlBaseLexer.SECOND:
                return IntervalLiteral.IntervalField.SECOND;
        }

        throw new IllegalArgumentException("Unsupported interval field: " + token.getText());
    }

    private static IntervalLiteral.Sign getIntervalSign(Token token)
    {
        switch (token.getType()) {
            case SqlBaseLexer.MINUS:
                return IntervalLiteral.Sign.NEGATIVE;
            case SqlBaseLexer.PLUS:
                return IntervalLiteral.Sign.POSITIVE;
        }

        throw new IllegalArgumentException("Unsupported sign: " + token.getText());
    }

    private static WindowFrame.Type getFrameType(Token type)
    {
        switch (type.getType()) {
            case SqlBaseLexer.RANGE:
                return WindowFrame.Type.RANGE;
            case SqlBaseLexer.ROWS:
                return WindowFrame.Type.ROWS;
        }

        throw new IllegalArgumentException("Unsupported frame type: " + type.getText());
    }

    private static FrameBound.Type getBoundedFrameBoundType(Token token)
    {
        switch (token.getType()) {
            case SqlBaseLexer.PRECEDING:
                return FrameBound.Type.PRECEDING;
            case SqlBaseLexer.FOLLOWING:
                return FrameBound.Type.FOLLOWING;
        }

        throw new IllegalArgumentException("Unsupported bound type: " + token.getText());
    }

    private static FrameBound.Type getUnboundedFrameBoundType(Token token)
    {
        switch (token.getType()) {
            case SqlBaseLexer.PRECEDING:
                return FrameBound.Type.UNBOUNDED_PRECEDING;
            case SqlBaseLexer.FOLLOWING:
                return FrameBound.Type.UNBOUNDED_FOLLOWING;
        }

        throw new IllegalArgumentException("Unsupported bound type: " + token.getText());
    }

    private static SampledRelation.Type getSamplingMethod(Token token)
    {
        switch (token.getType()) {
            case SqlBaseLexer.BERNOULLI:
                return SampledRelation.Type.BERNOULLI;
            case SqlBaseLexer.SYSTEM:
                return SampledRelation.Type.SYSTEM;
        }

        throw new IllegalArgumentException("Unsupported sampling method: " + token.getText());
    }

    private static LogicalBinaryExpression.Operator getLogicalBinaryOperator(Token token)
    {
        switch (token.getType()) {
            case SqlBaseLexer.AND:
                return LogicalBinaryExpression.Operator.AND;
            case SqlBaseLexer.OR:
                return LogicalBinaryExpression.Operator.OR;
        }

        throw new IllegalArgumentException("Unsupported operator: " + token.getText());
    }

    private static SortItem.NullOrdering getNullOrderingType(Token token)
    {
        switch (token.getType()) {
            case SqlBaseLexer.FIRST:
                return SortItem.NullOrdering.FIRST;
            case SqlBaseLexer.LAST:
                return SortItem.NullOrdering.LAST;
        }

        throw new IllegalArgumentException("Unsupported ordering: " + token.getText());
    }

    private static SortItem.Ordering getOrderingType(Token token)
    {
        switch (token.getType()) {
            case SqlBaseLexer.ASC:
                return SortItem.Ordering.ASCENDING;
            case SqlBaseLexer.DESC:
                return SortItem.Ordering.DESCENDING;
        }

        throw new IllegalArgumentException("Unsupported ordering: " + token.getText());
    }

    private static QuantifiedComparisonExpression.Quantifier getComparisonQuantifier(Token symbol)
    {
        switch (symbol.getType()) {
            case SqlBaseLexer.ALL:
                return QuantifiedComparisonExpression.Quantifier.ALL;
            case SqlBaseLexer.ANY:
                return QuantifiedComparisonExpression.Quantifier.ANY;
            case SqlBaseLexer.SOME:
                return QuantifiedComparisonExpression.Quantifier.SOME;
        }

        throw new IllegalArgumentException("Unsupported quantifier: " + symbol.getText());
    }

    private static void check(boolean condition, String message, ParserRuleContext context)
    {
        if (!condition) {
            throw parseError(message, context);
        }
    }

    public static NodeLocation getLocation(TerminalNode terminalNode)
    {
        requireNonNull(terminalNode, "terminalNode is null");
        return getLocation(terminalNode.getSymbol());
    }

    public static NodeLocation getLocation(ParserRuleContext parserRuleContext)
    {
        requireNonNull(parserRuleContext, "parserRuleContext is null");
        return getLocation(parserRuleContext.getStart());
    }

    public static NodeLocation getLocation(Token token)
    {
        requireNonNull(token, "token is null");
        return new NodeLocation(token.getLine(), token.getCharPositionInLine());
    }

    private static ParsingException parseError(String message, ParserRuleContext context)
    {
        return new ParsingException(message, null, context.getStart().getLine(), context.getStart().getCharPositionInLine());
    }

    @Override
    public Node visitSingleStatement(SqlBaseParser.SingleStatementContext context)
    {
        return visit(context.statement());
    }

    @Override
    public Node visitStandaloneExpression(SqlBaseParser.StandaloneExpressionContext context)
    {
        return visit(context.expression());
    }

    @Override
    public Node visitStandaloneType(SqlBaseParser.StandaloneTypeContext context)
    {
        return visit(context.type());
    }

    @Override
    public Node visitStandalonePathSpecification(SqlBaseParser.StandalonePathSpecificationContext context)
    {
        return visit(context.pathSpecification());
    }

    @Override
    public Node visitUse(SqlBaseParser.UseContext context)
    {
        return new Use(
                getLocation(context),
                visitIfPresent(context.catalog, Identifier.class),
                (Identifier) visit(context.schema));
    }

    @Override
    public Node visitCreateSchema(SqlBaseParser.CreateSchemaContext context)
    {
        Optional<PrincipalSpecification> principal = Optional.empty();
        if (context.AUTHORIZATION() != null) {
            principal = Optional.of(getPrincipalSpecification(context.principal()));
        }

        List<Property> properties = ImmutableList.of();
        if (context.properties() != null) {
            properties = visit(context.properties().propertyAssignments().property(), Property.class);
        }

        return new CreateSchema(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                context.EXISTS() != null,
                properties,
                principal);
    }

    @Override
    public Node visitDropSchema(SqlBaseParser.DropSchemaContext context)
    {
        return new DropSchema(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                context.EXISTS() != null,
                context.CASCADE() != null);
    }

    @Override
    public Node visitRenameSchema(SqlBaseParser.RenameSchemaContext context)
    {
        return new RenameSchema(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                (Identifier) visit(context.identifier()));
    }

    @Override
    public Node visitSetSchemaAuthorization(SqlBaseParser.SetSchemaAuthorizationContext context)
    {
        return new SetSchemaAuthorization(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                getPrincipalSpecification(context.principal()));
    }

    @Override
    public Node visitCreateTableAsSelect(SqlBaseParser.CreateTableAsSelectContext context)
    {
        Optional<String> comment = Optional.empty();
        if (context.COMMENT() != null) {
            comment = Optional.of(((StringLiteral) visit(context.string())).getValue());
        }

        Optional<List<Identifier>> columnAliases = Optional.empty();
        if (context.columnAliases() != null) {
            columnAliases = Optional.of(visit(context.columnAliases().identifier(), Identifier.class));
        }

        List<Property> properties = ImmutableList.of();
        if (context.properties() != null) {
            properties = visit(context.properties().propertyAssignments().property(), Property.class);
        }

        return new CreateTableAsSelect(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                (Query) visit(context.query()),
                context.EXISTS() != null,
                properties,
                context.NO() == null,
                columnAliases,
                comment);
    }

    @Override
    public Node visitCreateTable(SqlBaseParser.CreateTableContext context)
    {
        Optional<String> comment = Optional.empty();
        if (context.COMMENT() != null) {
            comment = Optional.of(((StringLiteral) visit(context.string())).getValue());
        }
        List<Property> properties = ImmutableList.of();
        if (context.properties() != null) {
            properties = visit(context.properties().propertyAssignments().property(), Property.class);
        }
        return new CreateTable(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                visit(context.tableElement(), TableElement.class),
                context.EXISTS() != null,
                properties,
                comment);
    }

    @Override
    public Node visitShowCreateTable(SqlBaseParser.ShowCreateTableContext context)
    {
        return new ShowCreate(getLocation(context), ShowCreate.Type.TABLE, getQualifiedName(context.qualifiedName()));
    }

    @Override
    public Node visitDropTable(SqlBaseParser.DropTableContext context)
    {
        return new DropTable(getLocation(context), getQualifiedName(context.qualifiedName()), context.EXISTS() != null);
    }

    @Override
    public Node visitDropView(SqlBaseParser.DropViewContext context)
    {
        return new DropView(getLocation(context), getQualifiedName(context.qualifiedName()), context.EXISTS() != null);
    }

    @Override
    public Node visitInsertInto(SqlBaseParser.InsertIntoContext context)
    {
        Optional<List<Identifier>> columnAliases = Optional.empty();
        if (context.columnAliases() != null) {
            columnAliases = Optional.of(visit(context.columnAliases().identifier(), Identifier.class));
        }

        return new Insert(
                getQualifiedName(context.qualifiedName()),
                columnAliases,
                (Query) visit(context.query()));
    }

    // ********************** query expressions ********************

    @Override
    public Node visitDelete(SqlBaseParser.DeleteContext context)
    {
        return new Delete(
                getLocation(context),
                new Table(getLocation(context), getQualifiedName(context.qualifiedName())),
                visitIfPresent(context.booleanExpression(), Expression.class));
    }

    @Override
    public Node visitRenameTable(SqlBaseParser.RenameTableContext context)
    {
        return new RenameTable(getLocation(context), getQualifiedName(context.from), getQualifiedName(context.to));
    }

    @Override
    public Node visitCommentTable(SqlBaseParser.CommentTableContext context)
    {
        Optional<String> comment = Optional.empty();

        if (context.string() != null) {
            comment = Optional.of(((StringLiteral) visit(context.string())).getValue());
        }

        return new Comment(getLocation(context), Comment.Type.TABLE, getQualifiedName(context.qualifiedName()), comment);
    }

    @Override
    public Node visitCommentColumn(SqlBaseParser.CommentColumnContext context)
    {
        Optional<String> comment = Optional.empty();

        if (context.string() != null) {
            comment = Optional.of(((StringLiteral) visit(context.string())).getValue());
        }

        return new Comment(getLocation(context), Comment.Type.COLUMN, getQualifiedName(context.qualifiedName()), comment);
    }

    @Override
    public Node visitRenameColumn(SqlBaseParser.RenameColumnContext context)
    {
        return new RenameColumn(
                getLocation(context),
                getQualifiedName(context.tableName),
                (Identifier) visit(context.from),
                (Identifier) visit(context.to));
    }

    @Override
    public Node visitAnalyze(SqlBaseParser.AnalyzeContext context)
    {
        List<Property> properties = ImmutableList.of();
        if (context.properties() != null) {
            properties = visit(context.properties().propertyAssignments().property(), Property.class);
        }
        return new Analyze(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                properties);
    }

    @Override
    public Node visitAddColumn(SqlBaseParser.AddColumnContext context)
    {
        return new AddColumn(getLocation(context), getQualifiedName(context.qualifiedName()), (ColumnDefinition) visit(context.columnDefinition()));
    }

    @Override
    public Node visitDropColumn(SqlBaseParser.DropColumnContext context)
    {
        return new DropColumn(getLocation(context), getQualifiedName(context.tableName), (Identifier) visit(context.column));
    }

    @Override
    public Node visitCreateView(SqlBaseParser.CreateViewContext context)
    {
        Optional<String> comment = Optional.empty();
        if (context.COMMENT() != null) {
            comment = Optional.of(((StringLiteral) visit(context.string())).getValue());
        }

        Optional<CreateView.Security> security = Optional.empty();
        if (context.DEFINER() != null) {
            security = Optional.of(CreateView.Security.DEFINER);
        }
        else if (context.INVOKER() != null) {
            security = Optional.of(CreateView.Security.INVOKER);
        }

        return new CreateView(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                (Query) visit(context.query()),
                context.REPLACE() != null,
                comment,
                security);
    }

    @Override
    public Node visitRenameView(SqlBaseParser.RenameViewContext context)
    {
        return new RenameView(getLocation(context), getQualifiedName(context.from), getQualifiedName(context.to));
    }

    @Override
    public Node visitStartTransaction(SqlBaseParser.StartTransactionContext context)
    {
        return new StartTransaction(visit(context.transactionMode(), TransactionMode.class));
    }

    @Override
    public Node visitCommit(SqlBaseParser.CommitContext context)
    {
        return new Commit(getLocation(context));
    }

    @Override
    public Node visitRollback(SqlBaseParser.RollbackContext context)
    {
        return new Rollback(getLocation(context));
    }

    @Override
    public Node visitTransactionAccessMode(SqlBaseParser.TransactionAccessModeContext context)
    {
        return new TransactionAccessMode(getLocation(context), context.accessMode.getType() == SqlBaseLexer.ONLY);
    }

    @Override
    public Node visitIsolationLevel(SqlBaseParser.IsolationLevelContext context)
    {
        return visit(context.levelOfIsolation());
    }

    @Override
    public Node visitReadUncommitted(SqlBaseParser.ReadUncommittedContext context)
    {
        return new Isolation(getLocation(context), Isolation.Level.READ_UNCOMMITTED);
    }

    @Override
    public Node visitReadCommitted(SqlBaseParser.ReadCommittedContext context)
    {
        return new Isolation(getLocation(context), Isolation.Level.READ_COMMITTED);
    }

    @Override
    public Node visitRepeatableRead(SqlBaseParser.RepeatableReadContext context)
    {
        return new Isolation(getLocation(context), Isolation.Level.REPEATABLE_READ);
    }

    @Override
    public Node visitSerializable(SqlBaseParser.SerializableContext context)
    {
        return new Isolation(getLocation(context), Isolation.Level.SERIALIZABLE);
    }

    @Override
    public Node visitCall(SqlBaseParser.CallContext context)
    {
        return new Call(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                visit(context.callArgument(), CallArgument.class));
    }

    @Override
    public Node visitPrepare(SqlBaseParser.PrepareContext context)
    {
        return new Prepare(
                getLocation(context),
                (Identifier) visit(context.identifier()),
                (Statement) visit(context.statement()));
    }

    @Override
    public Node visitDeallocate(SqlBaseParser.DeallocateContext context)
    {
        return new Deallocate(
                getLocation(context),
                (Identifier) visit(context.identifier()));
    }

    @Override
    public Node visitExecute(SqlBaseParser.ExecuteContext context)
    {
        return new Execute(
                getLocation(context),
                (Identifier) visit(context.identifier()),
                visit(context.expression(), Expression.class));
    }

    @Override
    public Node visitDescribeOutput(SqlBaseParser.DescribeOutputContext context)
    {
        return new DescribeOutput(
                getLocation(context),
                (Identifier) visit(context.identifier()));
    }

    @Override
    public Node visitDescribeInput(SqlBaseParser.DescribeInputContext context)
    {
        return new DescribeInput(
                getLocation(context),
                (Identifier) visit(context.identifier()));
    }

    @Override
    public Node visitProperty(SqlBaseParser.PropertyContext context)
    {
        return new Property(getLocation(context), (Identifier) visit(context.identifier()), (Expression) visit(context));
    }

    @Override
    public Node visitQuery(SqlBaseParser.QueryContext context)
    {
        Query body = (Query) visit(context.queryNoWith());

        return new Query(
                getLocation(context),
                visitIfPresent(context.with(), With.class),
                body.getQueryBody(),
                body.getOrderBy(),
                body.getOffset(),
                body.getLimit());
    }

    @Override
    public Node visitWith(SqlBaseParser.WithContext context)
    {
        return new With(getLocation(context), context.RECURSIVE() != null, visit(context.namedQuery(), WithQuery.class));
    }

    @Override
    public Node visitNamedQuery(SqlBaseParser.NamedQueryContext context)
    {
        Optional<List<Identifier>> columns = Optional.empty();
        if (context.columnAliases() != null) {
            columns = Optional.of(visit(context.columnAliases().identifier(), Identifier.class));
        }

        return new WithQuery(
                getLocation(context),
                (Identifier) visit(context.name),
                (Query) visit(context.query()),
                columns);
    }

    @Override
    public Node visitQueryNoWith(SqlBaseParser.QueryNoWithContext context)
    {
        QueryBody term = (QueryBody) visit(context.queryTerm());

        Optional<OrderBy> orderBy = Optional.empty();
        if (context.ORDER() != null) {
            orderBy = Optional.of(new OrderBy(getLocation(context.ORDER()), visit(context.sortItem(), SortItem.class)));
        }

        Optional<Node> limit = Optional.empty();
        if (context.FETCH() != null) {
            limit = Optional.of(new FetchFirst(Optional.of(getLocation(context.FETCH())), getTextIfPresent(context.fetchFirst), context.TIES() != null));
        }
        else if (context.LIMIT() != null) {
            if (context.limitRowCount() == null) {
                throw new IllegalStateException("Missing LIMIT value");
            }
            Expression rowCount;
            if (context.limitRowCount().ALL() != null) {
                rowCount = new AllRows(getLocation(context.limitRowCount().ALL()));
            }
            else if (context.limitRowCount().rowCount().INTEGER_VALUE() != null) {
                rowCount = new LongLiteral(getLocation(context.limitRowCount().rowCount().INTEGER_VALUE()), context.limitRowCount().getText());
            }
            else {
                rowCount = new Parameter(getLocation(context.limitRowCount().rowCount().INTEGER_VALUE()), parameterPosition);
                parameterPosition++;
            }

            limit = Optional.of(new Limit(Optional.of(getLocation(context.LIMIT())), rowCount));
        }

        Optional<Offset> offset = Optional.empty();
        if (context.OFFSET() != null) {
            offset = Optional.of(new Offset(Optional.of(getLocation(context.OFFSET())), getTextIfPresent(context.offset).orElseThrow(() -> new IllegalStateException("Missing OFFSET row count"))));
        }

        if (term instanceof QuerySpecification) {
            // When we have a simple query specification
            // followed by order by, offset, limit or fetch,
            // fold the order by, limit, offset or fetch clauses
            // into the query specification (analyzer/planner
            // expects this structure to resolve references with respect
            // to columns defined in the query specification)
            QuerySpecification query = (QuerySpecification) term;

            return new Query(
                    getLocation(context),
                    Optional.empty(),
                    new QuerySpecification(
                            getLocation(context),
                            query.getSelect(),
                            query.getFrom(),
                            query.getWhere(),
                            query.getGroupBy(),
                            query.getHaving(),
                            orderBy,
                            offset,
                            limit),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());
        }

        return new Query(
                getLocation(context),
                Optional.empty(),
                term,
                orderBy,
                offset,
                limit);
    }

    @Override
    public Node visitQuerySpecification(SqlBaseParser.QuerySpecificationContext context)
    {
        Optional<Relation> from = Optional.empty();
        List<SelectItem> selectItems = visit(context.selectItem(), SelectItem.class);

        List<Relation> relations = visit(context.relation(), Relation.class);
        if (!relations.isEmpty()) {
            // synthesize implicit join nodes
            Iterator<Relation> iterator = relations.iterator();
            Relation relation = iterator.next();

            while (iterator.hasNext()) {
                relation = new Join(getLocation(context), Join.Type.IMPLICIT, relation, iterator.next(), Optional.empty());
            }

            from = Optional.of(relation);
        }

        return new QuerySpecification(
                getLocation(context),
                new Select(getLocation(context.SELECT()), isDistinct(context.setQuantifier()), selectItems),
                from,
                visitIfPresent(context.where, Expression.class),
                visitIfPresent(context.groupBy(), GroupBy.class),
                visitIfPresent(context.having, Expression.class),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    @Override
    public Node visitGroupBy(SqlBaseParser.GroupByContext context)
    {
        return new GroupBy(getLocation(context), isDistinct(context.setQuantifier()), visit(context.groupingElement(), GroupingElement.class));
    }

    @Override
    public Node visitSingleGroupingSet(SqlBaseParser.SingleGroupingSetContext context)
    {
        return new SimpleGroupBy(getLocation(context), visit(context.groupingSet().expression(), Expression.class));
    }

    @Override
    public Node visitRollup(SqlBaseParser.RollupContext context)
    {
        return new Rollup(getLocation(context), visit(context.expression(), Expression.class));
    }

    @Override
    public Node visitCube(SqlBaseParser.CubeContext context)
    {
        return new Cube(getLocation(context), visit(context.expression(), Expression.class));
    }

    @Override
    public Node visitMultipleGroupingSets(SqlBaseParser.MultipleGroupingSetsContext context)
    {
        return new GroupingSets(getLocation(context), context.groupingSet().stream()
                .map(groupingSet -> visit(groupingSet.expression(), Expression.class))
                .collect(toList()));
    }

    @Override
    public Node visitSetOperation(SqlBaseParser.SetOperationContext context)
    {
        QueryBody left = (QueryBody) visit(context.left);
        QueryBody right = (QueryBody) visit(context.right);

        boolean distinct = context.setQuantifier() == null || context.setQuantifier().DISTINCT() != null;

        switch (context.operator.getType()) {
            case SqlBaseLexer.UNION:
                return new Union(getLocation(context.UNION()), ImmutableList.of(left, right), distinct);
            case SqlBaseLexer.INTERSECT:
                return new Intersect(getLocation(context.INTERSECT()), ImmutableList.of(left, right), distinct);
            case SqlBaseLexer.EXCEPT:
                return new Except(getLocation(context.EXCEPT()), left, right, distinct);
        }

        throw new IllegalArgumentException("Unsupported set operation: " + context.operator.getText());
    }

    @Override
    public Node visitSelectAll(SqlBaseParser.SelectAllContext context)
    {
        List<Identifier> aliases = ImmutableList.of();
        if (context.columnAliases() != null) {
            aliases = visit(context.columnAliases().identifier(), Identifier.class);
        }

        return new AllColumns(
                getLocation(context),
                visitIfPresent(context.primaryExpression(), Expression.class),
                aliases);
    }

    @Override
    public Node visitSelectSingle(SqlBaseParser.SelectSingleContext context)
    {
        return new SingleColumn(
                getLocation(context),
                (Expression) visit(context.expression()),
                visitIfPresent(context.identifier(), Identifier.class));
    }

    @Override
    public Node visitTable(SqlBaseParser.TableContext context)
    {
        return new Table(getLocation(context), getQualifiedName(context.qualifiedName()));
    }

    @Override
    public Node visitSubquery(SqlBaseParser.SubqueryContext context)
    {
        return new TableSubQuery(getLocation(context), (Query) visit(context.queryNoWith()));
    }

    @Override
    public Node visitInlineTable(SqlBaseParser.InlineTableContext context)
    {
        return new Values(getLocation(context), visit(context.expression(), Expression.class));
    }

    // ***************** boolean expressions ******************

    @Override
    public Node visitExplain(SqlBaseParser.ExplainContext context)
    {
        return new Explain(getLocation(context), context.EXPLAIN() != null, context.EXPLAIN() != null, (Statement) visit(context.statement()), visit(context.explainOption(), ExplainOption.class));
    }

    @Override
    public Node visitExplainFormat(SqlBaseParser.ExplainFormatContext context)
    {
        switch (context.value.getType()) {
            case SqlBaseLexer.GRAPHVIZ:
                return new ExplainFormat(getLocation(context), ExplainFormat.Type.GRAPHVIZ);
            case SqlBaseLexer.TEXT:
                return new ExplainFormat(getLocation(context), ExplainFormat.Type.TEXT);
            case SqlBaseLexer.JSON:
                return new ExplainFormat(getLocation(context), ExplainFormat.Type.JSON);
        }

        throw new IllegalArgumentException("Unsupported EXPLAIN format: " + context.value.getText());
    }

    // *************** from clause *****************

    @Override
    public Node visitExplainType(SqlBaseParser.ExplainTypeContext context)
    {
        switch (context.value.getType()) {
            case SqlBaseLexer.LOGICAL:
                return new ExplainType(getLocation(context), ExplainType.Type.LOGICAL);
            case SqlBaseLexer.DISTRIBUTED:
                return new ExplainType(getLocation(context), ExplainType.Type.DISTRIBUTED);
            case SqlBaseLexer.VALIDATE:
                return new ExplainType(getLocation(context), ExplainType.Type.VALIDATE);
            case SqlBaseLexer.IO:
                return new ExplainType(getLocation(context), ExplainType.Type.IO);
        }

        throw new IllegalArgumentException("Unsupported EXPLAIN type: " + context.value.getText());
    }

    @Override
    public Node visitShowTables(SqlBaseParser.ShowTablesContext context)
    {
        return new ShowTables(
                getLocation(context),
                Optional.ofNullable(context.qualifiedName())
                        .map(this::getQualifiedName),
                getTextIfPresent(context.pattern)
                        .map(AstVisitorBuilder::unquote),
                getTextIfPresent(context.escape)
                        .map(AstVisitorBuilder::unquote));
    }

    @Override
    public Node visitShowSchemas(SqlBaseParser.ShowSchemasContext context)
    {
        return new ShowSchemas(
                getLocation(context),
                visitIfPresent(context.identifier(), Identifier.class),
                getTextIfPresent(context.pattern)
                        .map(AstVisitorBuilder::unquote),
                getTextIfPresent(context.escape)
                        .map(AstVisitorBuilder::unquote));
    }

    @Override
    public Node visitShowCatalogs(SqlBaseParser.ShowCatalogsContext context)
    {
        return new ShowCatalogs(getLocation(context),
                getTextIfPresent(context.pattern)
                        .map(AstVisitorBuilder::unquote),
                getTextIfPresent(context.escape)
                        .map(AstVisitorBuilder::unquote));
    }

    @Override
    public Node visitShowColumns(SqlBaseParser.ShowColumnsContext context)
    {
        return new ShowColumns(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                getTextIfPresent(context.pattern)
                        .map(AstVisitorBuilder::unquote),
                getTextIfPresent(context.escape)
                        .map(AstVisitorBuilder::unquote));
    }

    @Override
    public Node visitShowStats(SqlBaseParser.ShowStatsContext context)
    {
        return new ShowStats(Optional.of(getLocation(context)), new Table(getQualifiedName(context.qualifiedName())));
    }

//    @Override
//    public Node visitShowStatsForQuery(SqlBaseParser.ShowStatsForQueryContext context)
//    {
//        QuerySpecification specification = (QuerySpecification) visitQuerySpecification(context.query());
//        return new ShowStats(Optional.of(getLocation(context)), new TableSubQuery(QueryBase.query(specification)));
//    }

    @Override
    public Node visitShowCreateSchema(SqlBaseParser.ShowCreateSchemaContext context)
    {
        return new ShowCreate(getLocation(context), ShowCreate.Type.SCHEMA, getQualifiedName(context.qualifiedName()));
    }

    // ********************* predicates *******************

    @Override
    public Node visitShowCreateView(SqlBaseParser.ShowCreateViewContext context)
    {
        return new ShowCreate(getLocation(context), ShowCreate.Type.VIEW, getQualifiedName(context.qualifiedName()));
    }

    @Override
    public Node visitShowFunctions(SqlBaseParser.ShowFunctionsContext context)
    {
        return new ShowFunctions(getLocation(context),
                getTextIfPresent(context.pattern)
                        .map(AstVisitorBuilder::unquote),
                getTextIfPresent(context.escape)
                        .map(AstVisitorBuilder::unquote));
    }

    @Override
    public Node visitShowSession(SqlBaseParser.ShowSessionContext context)
    {
        return new ShowSession(getLocation(context),
                getTextIfPresent(context.pattern)
                        .map(AstVisitorBuilder::unquote),
                getTextIfPresent(context.escape)
                        .map(AstVisitorBuilder::unquote));
    }

    @Override
    public Node visitSetSession(SqlBaseParser.SetSessionContext context)
    {
        return new SetSession(getLocation(context), getQualifiedName(context.qualifiedName()), (Expression) visit(context.expression()));
    }

    @Override
    public Node visitResetSession(SqlBaseParser.ResetSessionContext context)
    {
        return new ResetSession(getLocation(context), getQualifiedName(context.qualifiedName()));
    }

    @Override
    public Node visitCreateRole(SqlBaseParser.CreateRoleContext context)
    {
        return new CreateRole(
                getLocation(context),
                (Identifier) visit(context.name),
                getGrantorSpecificationIfPresent(context.grantor()));
    }

    @Override
    public Node visitDropRole(SqlBaseParser.DropRoleContext context)
    {
        return new DropRole(
                getLocation(context),
                (Identifier) visit(context.name));
    }

    @Override
    public Node visitGrantRoles(SqlBaseParser.GrantRolesContext context)
    {
        return new GrantRoles(
                getLocation(context),
                ImmutableSet.copyOf(getIdentifiers(context.roles().identifier())),
                ImmutableSet.copyOf(getPrincipalSpecifications(context.principal())),
                context.OPTION() != null,
                getGrantorSpecificationIfPresent(context.grantor()));
    }

    @Override
    public Node visitRevokeRoles(SqlBaseParser.RevokeRolesContext context)
    {
        return new RevokeRoles(
                getLocation(context),
                ImmutableSet.copyOf(getIdentifiers(context.roles().identifier())),
                ImmutableSet.copyOf(getPrincipalSpecifications(context.principal())),
                context.OPTION() != null,
                getGrantorSpecificationIfPresent(context.grantor()));
    }

    @Override
    public Node visitSetRole(SqlBaseParser.SetRoleContext context)
    {
        SetRole.Type type = SetRole.Type.ROLE;
        if (context.ALL() != null) {
            type = SetRole.Type.ALL;
        }
        else if (context.NONE() != null) {
            type = SetRole.Type.NONE;
        }
        return new SetRole(getLocation(context), type, getIdentifierIfPresent(context.role));
    }

    // ************** value expressions **************

    @Override
    public Node visitGrant(SqlBaseParser.GrantContext context)
    {
        Optional<List<String>> privileges;
        if (context.ALL() != null) {
            privileges = Optional.empty();
        }
        else {
            privileges = Optional.of(context.privilege().stream()
                    .map(SqlBaseParser.PrivilegeContext::getText)
                    .collect(toList()));
        }
        return new Grant(
                getLocation(context),
                privileges,
                context.TABLE() != null,
                getQualifiedName(context.qualifiedName()),
                getPrincipalSpecification(context.grantee),
                context.OPTION() != null);
    }

    @Override
    public Node visitRevoke(SqlBaseParser.RevokeContext context)
    {
        Optional<List<String>> privileges;
        if (context.ALL() != null) {
            privileges = Optional.empty();
        }
        else {
            privileges = Optional.of(context.privilege().stream()
                    .map(SqlBaseParser.PrivilegeContext::getText)
                    .collect(toList()));
        }
        return new Revoke(
                getLocation(context),
                context.OPTION() != null,
                privileges,
                context.TABLE() != null,
                getQualifiedName(context.qualifiedName()),
                getPrincipalSpecification(context.grantee));
    }

    @Override
    public Node visitShowGrants(SqlBaseParser.ShowGrantsContext context)
    {
        Optional<QualifiedName> tableName = Optional.empty();

        if (context.qualifiedName() != null) {
            tableName = Optional.of(getQualifiedName(context.qualifiedName()));
        }

        return new ShowGrants(
                getLocation(context),
                context.TABLE() != null,
                tableName);
    }

    @Override
    public Node visitShowRoles(SqlBaseParser.ShowRolesContext context)
    {
        return new ShowRoles(
                getLocation(context),
                getIdentifierIfPresent(context.identifier()),
                context.CURRENT() != null);
    }

    @Override
    public Node visitShowRoleGrants(SqlBaseParser.ShowRoleGrantsContext context)
    {
        return new ShowRoleGrants(
                getLocation(context),
                getIdentifierIfPresent(context.identifier()));
    }

    @Override
    public Node visitSetPath(SqlBaseParser.SetPathContext context)
    {
        return new SetPath(getLocation(context), (PathSpecification) visit(context.pathSpecification()));
    }

    // ********************* primary expressions **********************

    @Override
    public Node visitLogicalNot(SqlBaseParser.LogicalNotContext context)
    {
        return new NotExpression(getLocation(context), (Expression) visit(context.booleanExpression()));
    }

    @Override
    public Node visitJoinRelation(SqlBaseParser.JoinRelationContext context)
    {
        Relation left = (Relation) visit(context.left);
        Relation right;

        if (context.CROSS() != null) {
            right = (Relation) visit(context.right);
            return new Join(getLocation(context), Join.Type.CROSS, left, right, Optional.empty());
        }

        JoinCriteria criteria;
        if (context.NATURAL() != null) {
            right = (Relation) visit(context.right);
            criteria = new NaturalJoin();
        }
        else {
            right = (Relation) visit(context.rightRelation);
            if (context.joinCriteria().ON() != null) {
                criteria = new JoinOn((Expression) visit(context.joinCriteria().booleanExpression()));
            }
            else if (context.joinCriteria().USING() != null) {
                criteria = new JoinUsing(visit(context.joinCriteria().identifier(), Identifier.class));
            }
            else {
                throw new IllegalArgumentException("Unsupported join criteria");
            }
        }

        Join.Type joinType;
        if (context.joinType().LEFT() != null) {
            joinType = Join.Type.LEFT;
        }
        else if (context.joinType().RIGHT() != null) {
            joinType = Join.Type.RIGHT;
        }
        else if (context.joinType().FULL() != null) {
            joinType = Join.Type.FULL;
        }
        else {
            joinType = Join.Type.INNER;
        }

        return new Join(getLocation(context), joinType, left, right, Optional.of(criteria));
    }

    @Override
    public Node visitSampledRelation(SqlBaseParser.SampledRelationContext context)
    {
        Relation child = (Relation) visit(context.patternRecognition().aliasedRelation());

        if (context.TABLESAMPLE() == null) {
            return child;
        }

        return new SampledRelation(
                getLocation(context),
                child,
                getSamplingMethod((Token) context.sampleType().getChild(0).getPayload()),
                (Expression) visit(context.percentage));
    }

    @Override
    public Node visitAliasedRelation(SqlBaseParser.AliasedRelationContext context)
    {
        Relation child = (Relation) visit(context.relationPrimary());

        if (context.identifier() == null) {
            return child;
        }

        List<Identifier> aliases = null;
        if (context.columnAliases() != null) {
            aliases = visit(context.columnAliases().identifier(), Identifier.class);
        }

        return new AliasedRelation(getLocation(context), child, (Identifier) visit(context.identifier()), aliases);
    }

    @Override
    public Node visitTableName(SqlBaseParser.TableNameContext context)
    {
        return new Table(getLocation(context), getQualifiedName(context.qualifiedName()));
    }

    @Override
    public Node visitSubqueryRelation(SqlBaseParser.SubqueryRelationContext context)
    {
        return new TableSubQuery(getLocation(context), (Query) visit(context.query()));
    }

    @Override
    public Node visitUnnest(SqlBaseParser.UnnestContext context)
    {
        return new Unnest(getLocation(context), visit(context.expression(), Expression.class), context.ORDINALITY() != null);
    }

    @Override
    public Node visitLateral(SqlBaseParser.LateralContext context)
    {
        return new Lateral(getLocation(context), (Query) visit(context.query()));
    }

    @Override
    public Node visitParenthesizedRelation(SqlBaseParser.ParenthesizedRelationContext context)
    {
        return visit(context.relation());
    }

    @Override
    public Node visitPredicated(SqlBaseParser.PredicatedContext context)
    {
        if (context.predicate() != null) {
            return visit(context.predicate());
        }

        return visit(context.valueExpression);
    }

    @Override
    public Node visitComparison(SqlBaseParser.ComparisonContext context)
    {
        return new ComparisonExpression(
                getLocation(context.comparisonOperator()),
                getComparisonOperator(((TerminalNode) context.comparisonOperator().getChild(0)).getSymbol()),
                (Expression) visit(context.value),
                (Expression) visit(context.right));
    }

    @Override
    public Node visitDistinctFrom(SqlBaseParser.DistinctFromContext context)
    {
        Expression expression = new ComparisonExpression(
                getLocation(context),
                ComparisonExpression.Operator.IS_DISTINCT_FROM,
                (Expression) visit(context.value),
                (Expression) visit(context.right));

        if (context.NOT() != null) {
            expression = new NotExpression(getLocation(context), expression);
        }

        return expression;
    }

    @Override
    public Node visitBetween(SqlBaseParser.BetweenContext context)
    {
        Expression expression = new BetweenPredicate(
                getLocation(context),
                (Expression) visit(context.value),
                (Expression) visit(context.lower),
                (Expression) visit(context.upper));

        if (context.NOT() != null) {
            expression = new NotExpression(getLocation(context), expression);
        }

        return expression;
    }

    @Override
    public Node visitNullPredicate(SqlBaseParser.NullPredicateContext context)
    {
        Expression child = (Expression) visit(context.value);

        if (context.NOT() == null) {
            return new IsNullPredicate(getLocation(context), child);
        }

        return new IsNotNullPredicate(getLocation(context), child);
    }

    @Override
    public Node visitLike(SqlBaseParser.LikeContext context)
    {
        Expression result = new LikePredicate(
                getLocation(context),
                (Expression) visit(context.value),
                (Expression) visit(context.pattern),
                visitIfPresent(context.escape, Expression.class));

        if (context.NOT() != null) {
            result = new NotExpression(getLocation(context), result);
        }

        return result;
    }

    @Override
    public Node visitInList(SqlBaseParser.InListContext context)
    {
        Expression result = new InPredicate(
                getLocation(context),
                (Expression) visit(context.value),
                new InListExpression(getLocation(context), visit(context.expression(), Expression.class)));

        if (context.NOT() != null) {
            result = new NotExpression(getLocation(context), result);
        }

        return result;
    }

    @Override
    public Node visitInSubquery(SqlBaseParser.InSubqueryContext context)
    {
        Expression result = new InPredicate(
                getLocation(context),
                (Expression) visit(context.value),
                new SubqueryExpression(getLocation(context), (Query) visit(context.query())));

        if (context.NOT() != null) {
            result = new NotExpression(getLocation(context), result);
        }

        return result;
    }

    @Override
    public Node visitExists(SqlBaseParser.ExistsContext context)
    {
        return new ExistsPredicate(getLocation(context), new SubqueryExpression(getLocation(context), (Query) visit(context.query())));
    }

    @Override
    public Node visitQuantifiedComparison(SqlBaseParser.QuantifiedComparisonContext context)
    {
        return new QuantifiedComparisonExpression(
                getLocation(context.comparisonOperator()),
                getComparisonOperator(((TerminalNode) context.comparisonOperator().getChild(0)).getSymbol()),
                getComparisonQuantifier(((TerminalNode) context.comparisonQuantifier().getChild(0)).getSymbol()),
                (Expression) visit(context.value),
                new SubqueryExpression(getLocation(context.query()), (Query) visit(context.query())));
    }

    @Override
    public Node visitArithmeticUnary(SqlBaseParser.ArithmeticUnaryContext context)
    {
        Expression child = (Expression) visit(context.valueExpression());

        switch (context.operator.getType()) {
            case SqlBaseLexer.MINUS:
                return ArithmeticUnaryExpression.negative(getLocation(context), child);
            case SqlBaseLexer.PLUS:
                return ArithmeticUnaryExpression.positive(getLocation(context), child);
            default:
                throw new UnsupportedOperationException("Unsupported sign: " + context.operator.getText());
        }
    }

    @Override
    public Node visitArithmeticBinary(SqlBaseParser.ArithmeticBinaryContext context)
    {
        return new ArithmeticBinaryExpression(
                getLocation(context.operator),
                getArithmeticBinaryOperator(context.operator),
                (Expression) visit(context.left),
                (Expression) visit(context.right));
    }

    @Override
    public Node visitConcatenation(SqlBaseParser.ConcatenationContext context)
    {
        return new FunctionCall(
                getLocation(context.CONCAT()),
                QualifiedName.of("concat"), ImmutableList.of(
                (Expression) visit(context.left),
                (Expression) visit(context.right)));
    }

    @Override
    public Node visitAtTimeZone(SqlBaseParser.AtTimeZoneContext context)
    {
        return new AtTimeZone(
                getLocation(context.AT()),
                (Expression) visit(context.valueExpression()),
                (Expression) visit(context.timeZoneSpecifier()));
    }

    @Override
    public Node visitTimeZoneInterval(SqlBaseParser.TimeZoneIntervalContext context)
    {
        return visit(context.interval());
    }

    @Override
    public Node visitTimeZoneString(SqlBaseParser.TimeZoneStringContext context)
    {
        return visit(context.string());
    }

    @Override
    public Node visitParenthesizedExpression(SqlBaseParser.ParenthesizedExpressionContext context)
    {
        return visit(context.expression());
    }

    @Override
    public Node visitRowConstructor(SqlBaseParser.RowConstructorContext context)
    {
        return new Row(getLocation(context), visit(context.expression(), Expression.class));
    }

    @Override
    public Node visitArrayConstructor(SqlBaseParser.ArrayConstructorContext context)
    {
        return new ArrayConstructor(getLocation(context), visit(context.expression(), Expression.class));
    }

    @Override
    public Node visitCast(SqlBaseParser.CastContext context)
    {
        boolean isTryCast = context.TRY_CAST() != null;
        return new Cast(getLocation(context), (Expression) visit(context.expression()), (DataType) visit(context.type()), isTryCast);
    }

    @Override
    public Node visitSpecialDateTimeFunction(SqlBaseParser.SpecialDateTimeFunctionContext context)
    {
        CurrentTime.Function function = getDateTimeFunctionType(context.name);

        if (context.precision != null) {
            return new CurrentTime(getLocation(context), function, Integer.parseInt(context.precision.getText()));
        }

        return new CurrentTime(getLocation(context), function);
    }

    @Override
    public Node visitCurrentUser(SqlBaseParser.CurrentUserContext context)
    {
        return new CurrentUser(getLocation(context.CURRENT_USER()));
    }

    // ************** literals **************

    @Override
    public Node visitCurrentPath(SqlBaseParser.CurrentPathContext context)
    {
        return new CurrentPath(getLocation(context.CURRENT_PATH()));
    }

    @Override
    public Node visitExtract(SqlBaseParser.ExtractContext context)
    {
        String fieldString = context.identifier().getText();
        Extract.Field field;
        try {
            field = Extract.Field.valueOf(fieldString.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            throw parseError("Invalid EXTRACT field: " + fieldString, context);
        }
        return new Extract(getLocation(context), (Expression) visit(context.valueExpression()), field);
    }

    @Override
    public Node visitSubstring(SqlBaseParser.SubstringContext context)
    {
        return new FunctionCall(getLocation(context), QualifiedName.of("substr"), visit(context.valueExpression(), Expression.class));
    }

    @Override
    public Node visitPosition(SqlBaseParser.PositionContext context)
    {
        List<Expression> arguments = Lists.reverse(visit(context.valueExpression(), Expression.class));
        return new FunctionCall(getLocation(context), QualifiedName.of("strpos"), arguments);
    }

    @Override
    public Node visitNormalize(SqlBaseParser.NormalizeContext context)
    {
        Expression str = (Expression) visit(context.valueExpression());
        String normalForm = Optional.ofNullable(context.normalForm()).map(ParserRuleContext::getText).orElse("NFC");
        return new FunctionCall(
                getLocation(context),
                QualifiedName.of(ImmutableList.of(new Identifier("normalize", true))), // delimited to avoid ambiguity with NORMALIZE SQL construct
                ImmutableList.of(str, new StringLiteral(getLocation(context), normalForm)));
    }

    @Override
    public Node visitSubscript(SqlBaseParser.SubscriptContext context)
    {
        return new SubscriptExpression(getLocation(context), (Expression) visit(context.value), (Expression) visit(context.index));
    }

    @Override
    public Node visitSubqueryExpression(SqlBaseParser.SubqueryExpressionContext context)
    {
        return new SubqueryExpression(getLocation(context), (Query) visit(context.query()));
    }

    @Override
    public Node visitDereference(SqlBaseParser.DereferenceContext context)
    {
        return new DereferenceExpression(
                getLocation(context),
                (Expression) visit(context.base),
                (Identifier) visit(context.fieldName));
    }

    @Override
    public Node visitColumnReference(SqlBaseParser.ColumnReferenceContext context)
    {
        return visit(context.identifier());
    }

    @Override
    public Node visitSimpleCase(SqlBaseParser.SimpleCaseContext context)
    {
        return new SimpleCaseExpression(
                getLocation(context),
                (Expression) visit(context.operand),
                visit(context.whenClause(), WhenClause.class),
                visitIfPresent(context.elseExpression, Expression.class));
    }

    @Override
    public Node visitSearchedCase(SqlBaseParser.SearchedCaseContext context)
    {
        return new SearchedCaseExpression(
                getLocation(context),
                visit(context.whenClause(), WhenClause.class),
                visitIfPresent(context.elseExpression, Expression.class));
    }

    // ***************** arguments *****************

    @Override
    public Node visitWhenClause(SqlBaseParser.WhenClauseContext context)
    {
        return new WhenClause(getLocation(context), (Expression) visit(context.condition), (Expression) visit(context.result));
    }

    @Override
    public Node visitFunctionCall(SqlBaseParser.FunctionCallContext context)
    {
        Optional<Expression> filter = visitIfPresent(context.filter(), Expression.class);
        Optional<Window> window = visitIfPresent(context.over(), Window.class);

        Optional<OrderBy> orderBy = Optional.empty();
        if (context.ORDER() != null) {
            orderBy = Optional.of(new OrderBy(visit(context.sortItem(), SortItem.class)));
        }

        QualifiedName name = getQualifiedName(context.qualifiedName());

        boolean distinct = isDistinct(context.setQuantifier());

        SqlBaseParser.NullTreatmentContext nullTreatment = context.nullTreatment();

        if (name.toString().equalsIgnoreCase("if")) {
            check(context.expression().size() == 2 || context.expression().size() == 3, "Invalid number of arguments for 'if' function", context);
            check(!window.isPresent(), "OVER clause not valid for 'if' function", context);
            check(!distinct, "DISTINCT not valid for 'if' function", context);
            check(nullTreatment == null, "Null treatment clause not valid for 'if' function", context);
            check(!filter.isPresent(), "FILTER not valid for 'if' function", context);

            Expression elseExpression = null;
            if (context.expression().size() == 3) {
                elseExpression = (Expression) visit(context.expression(2));
            }

            return new IfExpression(
                    getLocation(context),
                    (Expression) visit(context.expression(0)),
                    (Expression) visit(context.expression(1)),
                    elseExpression);
        }

        if (name.toString().equalsIgnoreCase("nullif")) {
            check(context.expression().size() == 2, "Invalid number of arguments for 'nullif' function", context);
            check(!window.isPresent(), "OVER clause not valid for 'nullif' function", context);
            check(!distinct, "DISTINCT not valid for 'nullif' function", context);
            check(nullTreatment == null, "Null treatment clause not valid for 'nullif' function", context);
            check(!filter.isPresent(), "FILTER not valid for 'nullif' function", context);

            return new NullIfExpression(
                    getLocation(context),
                    (Expression) visit(context.expression(0)),
                    (Expression) visit(context.expression(1)));
        }

        if (name.toString().equalsIgnoreCase("coalesce")) {
            check(context.expression().size() >= 2, "The 'coalesce' function must have at least two arguments", context);
            check(!window.isPresent(), "OVER clause not valid for 'coalesce' function", context);
            check(!distinct, "DISTINCT not valid for 'coalesce' function", context);
            check(nullTreatment == null, "Null treatment clause not valid for 'coalesce' function", context);
            check(!filter.isPresent(), "FILTER not valid for 'coalesce' function", context);

            return new CoalesceExpression(getLocation(context), visit(context.expression(), Expression.class));
        }

        if (name.toString().equalsIgnoreCase("try")) {
            check(context.expression().size() == 1, "The 'try' function must have exactly one argument", context);
            check(!window.isPresent(), "OVER clause not valid for 'try' function", context);
            check(!distinct, "DISTINCT not valid for 'try' function", context);
            check(nullTreatment == null, "Null treatment clause not valid for 'try' function", context);
            check(!filter.isPresent(), "FILTER not valid for 'try' function", context);

            return new TryExpression(getLocation(context), (Expression) visit(getOnlyElement(context.expression())));
        }

        if (name.toString().equalsIgnoreCase("format")) {
            check(context.expression().size() >= 2, "The 'format' function must have at least two arguments", context);
            check(!window.isPresent(), "OVER clause not valid for 'format' function", context);
            check(!distinct, "DISTINCT not valid for 'format' function", context);
            check(nullTreatment == null, "Null treatment clause not valid for 'format' function", context);
            check(!filter.isPresent(), "FILTER not valid for 'format' function", context);

            return new Format(getLocation(context), visit(context.expression(), Expression.class));
        }

        if (name.toString().equalsIgnoreCase("$internal$bind")) {
            check(context.expression().size() >= 1, "The '$internal$bind' function must have at least one arguments", context);
            check(!window.isPresent(), "OVER clause not valid for '$internal$bind' function", context);
            check(!distinct, "DISTINCT not valid for '$internal$bind' function", context);
            check(nullTreatment == null, "Null treatment clause not valid for '$internal$bind' function", context);
            check(!filter.isPresent(), "FILTER not valid for '$internal$bind' function", context);

            int numValues = context.expression().size() - 1;
            List<Expression> arguments = context.expression().stream()
                    .map(this::visit)
                    .map(Expression.class::cast)
                    .collect(toImmutableList());

            return new BindExpression(
                    getLocation(context),
                    arguments.subList(0, numValues),
                    arguments.get(numValues));
        }

        Optional<NullTreatment> nulls = Optional.empty();
        if (nullTreatment != null) {
            if (nullTreatment.IGNORE() != null) {
                nulls = Optional.of(NullTreatment.IGNORE);
            }
            else if (nullTreatment.RESPECT() != null) {
                nulls = Optional.of(NullTreatment.RESPECT);
            }
        }

        return new FunctionCall(
                Optional.of(getLocation(context)),
                name,
                window,
                filter,
                orderBy,
                distinct,
                nulls,
                visit(context.expression(), Expression.class));
    }

    @Override
    public Node visitLambda(SqlBaseParser.LambdaContext context)
    {
        List<LambdaArgumentDeclaration> arguments = visit(context.identifier(), Identifier.class).stream()
                .map(LambdaArgumentDeclaration::new)
                .collect(toList());

        Expression body = (Expression) visit(context.expression());

        return new LambdaExpression(getLocation(context), arguments, body);
    }

    @Override
    public Node visitFilter(SqlBaseParser.FilterContext context)
    {
        return visit(context.booleanExpression());
    }

    @Override
    public Node visitOver(SqlBaseParser.OverContext context)
    {
        Optional<OrderBy> orderBy = Optional.empty();
        if (context.windowSpecification().ORDER() != null) {
            orderBy = Optional.of(new OrderBy(getLocation(context.windowSpecification().ORDER()), visit(context.windowSpecification().sortItem(), SortItem.class)));
        }

        return new Window(
                getLocation(context),
                visit(context.windowSpecification().partition, Expression.class),
                orderBy,
                visitIfPresent(context.windowSpecification().windowFrame(), WindowFrame.class));
    }

    @Override
    public Node visitColumnDefinition(SqlBaseParser.ColumnDefinitionContext context)
    {
        Optional<String> comment = Optional.empty();
        if (context.COMMENT() != null) {
            comment = Optional.of(((StringLiteral) visit(context.string())).getValue());
        }

        List<Property> properties = ImmutableList.of();
        if (context.properties() != null) {
            properties = visit(context.properties().propertyAssignments().property(), Property.class);
        }

        boolean nullable = context.NOT() == null;

        return new ColumnDefinition(
                getLocation(context),
                (Identifier) visit(context.identifier()),
                (DataType) visit(context.type()),
                nullable,
                properties,
                comment);
    }

    @Override
    public Node visitLikeClause(SqlBaseParser.LikeClauseContext context)
    {
        return new LikeClause(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                Optional.ofNullable(context.optionType)
                        .map(AstVisitorBuilder::getPropertiesOption));
    }

    @Override
    public Node visitSortItem(SqlBaseParser.SortItemContext context)
    {
        return new SortItem(
                getLocation(context),
                (Expression) visit(context.expression()),
                Optional.ofNullable(context.ordering)
                        .map(AstVisitorBuilder::getOrderingType)
                        .orElse(SortItem.Ordering.ASCENDING),
                Optional.ofNullable(context.nullOrdering)
                        .map(AstVisitorBuilder::getNullOrderingType)
                        .orElse(SortItem.NullOrdering.UNDEFINED));
    }

    @Override
    public Node visitWindowFrame(SqlBaseParser.WindowFrameContext context)
    {
        return new WindowFrame(
                getLocation(context),
                getFrameType(context.frameExtent().frameType),
                (FrameBound) visit(context.frameExtent().start),
                visitIfPresent(context.frameExtent().end, FrameBound.class));
    }

    @Override
    public Node visitUnboundedFrame(SqlBaseParser.UnboundedFrameContext context)
    {
        return new FrameBound(getLocation(context), getUnboundedFrameBoundType(context.boundType));
    }

    @Override
    public Node visitBoundedFrame(SqlBaseParser.BoundedFrameContext context)
    {
        return new FrameBound(getLocation(context), getBoundedFrameBoundType(context.boundType), (Expression) visit(context.expression()));
    }

    @Override
    public Node visitCurrentRowBound(SqlBaseParser.CurrentRowBoundContext context)
    {
        return new FrameBound(getLocation(context), FrameBound.Type.CURRENT_ROW);
    }

    @Override
    public Node visitGroupingOperation(SqlBaseParser.GroupingOperationContext context)
    {
        List<QualifiedName> arguments = context.qualifiedName().stream()
                .map(this::getQualifiedName)
                .collect(toList());

        return new GroupingOperation(Optional.of(getLocation(context)), arguments);
    }

    @Override
    public Node visitUnquotedIdentifier(SqlBaseParser.UnquotedIdentifierContext context)
    {
        return new Identifier(getLocation(context), context.getText(), false);
    }

    @Override
    public Node visitQuotedIdentifier(SqlBaseParser.QuotedIdentifierContext context)
    {
        String token = context.getText();
        String identifier = token.substring(1, token.length() - 1)
                .replace("\"\"", "\"");

        return new Identifier(getLocation(context), identifier, true);
    }

    // ***************** helpers *****************

    @Override
    public Node visitNullLiteral(SqlBaseParser.NullLiteralContext context)
    {
        return new NullLiteral(getLocation(context));
    }

    @Override
    public Node visitBasicStringLiteral(SqlBaseParser.BasicStringLiteralContext context)
    {
        return new StringLiteral(getLocation(context), unquote(context.STRING().getText()));
    }

    @Override
    public Node visitUnicodeStringLiteral(SqlBaseParser.UnicodeStringLiteralContext context)
    {
        return new StringLiteral(getLocation(context), decodeUnicodeLiteral(context));
    }

    @Override
    public Node visitBinaryLiteral(SqlBaseParser.BinaryLiteralContext context)
    {
        String raw = context.BINARY_LITERAL().getText();
        return new BinaryLiteral(getLocation(context), unquote(raw.substring(1)));
    }

    @Override
    public Node visitTypeConstructor(SqlBaseParser.TypeConstructorContext context)
    {
        String value = ((StringLiteral) visit(context.string())).getValue();

        if (context.DOUBLE() != null) {
            // TODO: Temporary hack that should be removed with new planner.
            return new GenericLiteral(getLocation(context), "DOUBLE", value);
        }

        String type = context.identifier().getText();
        if (type.equalsIgnoreCase("time")) {
            return new TimeLiteral(getLocation(context), value);
        }
        if (type.equalsIgnoreCase("timestamp")) {
            return new TimestampLiteral(getLocation(context), value);
        }
        if (type.equalsIgnoreCase("decimal")) {
            return new DecimalLiteral(getLocation(context), value);
        }
        if (type.equalsIgnoreCase("char")) {
            return new CharLiteral(getLocation(context), value);
        }

        return new GenericLiteral(getLocation(context), type, value);
    }

    @Override
    public Node visitIntegerLiteral(SqlBaseParser.IntegerLiteralContext context)
    {
        return new LongLiteral(getLocation(context), context.getText());
    }

    @Override
    public Node visitDecimalLiteral(SqlBaseParser.DecimalLiteralContext context)
    {
        switch (parserOptions.getDecimalLiteralTreatment()) {
            case AS_DOUBLE:
                return new DoubleLiteral(getLocation(context), context.getText());
            case AS_DECIMAL:
                return new DecimalLiteral(getLocation(context), context.getText());
            case REJECT:
                throw new ParsingException("Unexpected decimal literal: " + context.getText());
        }
        throw new AssertionError("Unreachable");
    }

    @Override
    public Node visitDoubleLiteral(SqlBaseParser.DoubleLiteralContext context)
    {
        return new DoubleLiteral(getLocation(context), context.getText());
    }

    @Override
    public Node visitBooleanValue(SqlBaseParser.BooleanValueContext context)
    {
        return new BooleanLiteral(getLocation(context), context.getText());
    }

    @Override
    public Node visitInterval(SqlBaseParser.IntervalContext context)
    {
        return new IntervalLiteral(
                getLocation(context),
                ((StringLiteral) visit(context.string())).getValue(),
                Optional.ofNullable(context.sign)
                        .map(AstVisitorBuilder::getIntervalSign)
                        .orElse(IntervalLiteral.Sign.POSITIVE),
                getIntervalFieldType((Token) context.from.getChild(0).getPayload()),
                Optional.ofNullable(context.to)
                        .map((x) -> x.getChild(0).getPayload())
                        .map(Token.class::cast)
                        .map(AstVisitorBuilder::getIntervalFieldType));
    }

    @Override
    public Node visitParameter(SqlBaseParser.ParameterContext context)
    {
        Parameter parameter = new Parameter(getLocation(context), parameterPosition);
        parameterPosition++;
        return parameter;
    }

    @Override
    public Node visitPositionalArgument(SqlBaseParser.PositionalArgumentContext context)
    {
        return new CallArgument(getLocation(context), (Expression) visit(context.expression()));
    }

    @Override
    public Node visitNamedArgument(SqlBaseParser.NamedArgumentContext context)
    {
        return new CallArgument(getLocation(context), context.identifier().getText(), (Expression) visit(context.expression()));
    }

    @Override
    public Node visitQualifiedArgument(SqlBaseParser.QualifiedArgumentContext context)
    {
        return new PathElement(getLocation(context), (Identifier) visit(context.identifier(0)), (Identifier) visit(context.identifier(1)));
    }

    @Override
    public Node visitUnqualifiedArgument(SqlBaseParser.UnqualifiedArgumentContext context)
    {
        return new PathElement(getLocation(context), (Identifier) visit(context.identifier()));
    }

    @Override
    public Node visitPathSpecification(SqlBaseParser.PathSpecificationContext context)
    {
        return new PathSpecification(getLocation(context), visit(context.pathElement(), PathElement.class));
    }

    @Override
    public Node visitRowType(SqlBaseParser.RowTypeContext context)
    {
        List<RowDataType.Field> fields = context.rowField().stream()
                .map(this::visit)
                .map(RowDataType.Field.class::cast)
                .collect(toImmutableList());

        return new RowDataType(getLocation(context), fields);
    }

    @Override
    public Node visitRowField(SqlBaseParser.RowFieldContext context)
    {
        return new RowDataType.Field(
                getLocation(context),
                visitIfPresent(context.identifier(), Identifier.class),
                (DataType) visit(context.type()));
    }

    @Override
    public Node visitGenericType(SqlBaseParser.GenericTypeContext context)
    {
        List<DataTypeParameter> parameters = context.typeParameter().stream()
                .map(this::visit)
                .map(DataTypeParameter.class::cast)
                .collect(toImmutableList());

        return new GenericDataType(getLocation(context), (Identifier) visit(context.identifier()), parameters);
    }

    @Override
    public Node visitTypeParameter(SqlBaseParser.TypeParameterContext context)
    {
        if (context.INTEGER_VALUE() != null) {
            return new NumericParameter(getLocation(context), context.getText());
        }

        return new TypeParameter((DataType) visit(context.type()));
    }

    @Override
    public Node visitIntervalType(SqlBaseParser.IntervalTypeContext context)
    {
        String from = context.from.getText();
        String to = getTextIfPresent(context.to)
                .orElse(from);

        return new IntervalDayTimeDataType(
                getLocation(context),
                IntervalDayTimeDataType.Field.valueOf(from.toUpperCase(ENGLISH)),
                IntervalDayTimeDataType.Field.valueOf(to.toUpperCase(ENGLISH)));
    }

    @Override
    public Node visitDateTimeType(SqlBaseParser.DateTimeTypeContext context)
    {
        DateTimeDataType.Type type;

        if (context.base.getType() == SqlBaseParser.TIME) {
            type = DateTimeDataType.Type.TIME;
        }
        else if (context.base.getType() == SqlBaseParser.TIMESTAMP) {
            type = DateTimeDataType.Type.TIMESTAMP;
        }
        else {
            throw new ParsingException("Unexpected datetime type: " + context.getText());
        }

        return new DateTimeDataType(
                getLocation(context),
                type,
                context.WITH() != null,
                visitIfPresent(context.precision, DataTypeParameter.class));
    }

    @Override
    public Node visitDoublePrecisionType(SqlBaseParser.DoublePrecisionTypeContext context)
    {
        return new GenericDataType(
                getLocation(context),
                new Identifier(getLocation(context.DOUBLE()), context.DOUBLE().getText(), false),
                ImmutableList.of());
    }

    @Override
    public Node visitLegacyArrayType(SqlBaseParser.LegacyArrayTypeContext context)
    {
        return new GenericDataType(
                getLocation(context),
                new Identifier(getLocation(context.ARRAY()), context.ARRAY().getText(), false),
                ImmutableList.of(new TypeParameter((DataType) visit(context.type()))));
    }

    @Override
    public Node visitLegacyMapType(SqlBaseParser.LegacyMapTypeContext context)
    {
        return new GenericDataType(
                getLocation(context),
                new Identifier(getLocation(context.MAP()), context.MAP().getText(), false),
                ImmutableList.of(
                        new TypeParameter((DataType) visit(context.keyType)),
                        new TypeParameter((DataType) visit(context.valueType))));
    }

    @Override
    public Node visitArrayType(SqlBaseParser.ArrayTypeContext context)
    {
        if (context.INTEGER_VALUE() != null) {
            throw new UnsupportedOperationException("Explicit array size not supported");
        }

        return new GenericDataType(
                getLocation(context),
                new Identifier(getLocation(context.ARRAY()), context.ARRAY().getText(), false),
                ImmutableList.of(new TypeParameter((DataType) visit(context.type()))));
    }

    @Override
    protected Node defaultResult()
    {
        return null;
    }

    @Override
    protected Node aggregateResult(Node aggregate, Node nextResult)
    {
        if (nextResult == null) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        if (aggregate == null) {
            return nextResult;
        }

        throw new UnsupportedOperationException("not yet implemented");
    }

    private <T> Optional<T> visitIfPresent(ParserRuleContext context, Class<T> clazz)
    {
        return Optional.ofNullable(context)
                .map(this::visit)
                .map(clazz::cast);
    }

    private <T> List<T> visit(List<? extends ParserRuleContext> contexts, Class<T> clazz)
    {
        return contexts.stream()
                .map(this::visit)
                .map(clazz::cast)
                .collect(toList());
    }

    private QualifiedName getQualifiedName(SqlBaseParser.QualifiedNameContext context)
    {
        return QualifiedName.of(visit(context.identifier(), Identifier.class));
    }

    private Optional<Identifier> getIdentifierIfPresent(ParserRuleContext context)
    {
        return Optional.ofNullable(context).map(c -> (Identifier) visit(c));
    }

    private List<Identifier> getIdentifiers(List<SqlBaseParser.IdentifierContext> identifiers)
    {
        return identifiers.stream().map(context -> (Identifier) visit(context)).collect(toList());
    }

    private List<PrincipalSpecification> getPrincipalSpecifications(List<SqlBaseParser.PrincipalContext> principals)
    {
        return principals.stream().map(this::getPrincipalSpecification).collect(toList());
    }

    private Optional<GrantorSpecification> getGrantorSpecificationIfPresent(SqlBaseParser.GrantorContext context)
    {
        return Optional.ofNullable(context).map(this::getGrantorSpecification);
    }

    private GrantorSpecification getGrantorSpecification(SqlBaseParser.GrantorContext context)
    {
        if (context instanceof SqlBaseParser.SpecifiedPrincipalContext) {
            return new GrantorSpecification(GrantorSpecification.Type.PRINCIPAL, Optional.of(getPrincipalSpecification(((SqlBaseParser.SpecifiedPrincipalContext) context).principal())));
        }
        else if (context instanceof SqlBaseParser.CurrentUserGrantorContext) {
            return new GrantorSpecification(GrantorSpecification.Type.CURRENT_USER, Optional.empty());
        }
        else if (context instanceof SqlBaseParser.CurrentRoleGrantorContext) {
            return new GrantorSpecification(GrantorSpecification.Type.CURRENT_ROLE, Optional.empty());
        }
        else {
            throw new IllegalArgumentException("Unsupported grantor: " + context);
        }
    }

    private PrincipalSpecification getPrincipalSpecification(SqlBaseParser.PrincipalContext context)
    {
        if (context instanceof SqlBaseParser.UnspecifiedPrincipalContext) {
            return new PrincipalSpecification(PrincipalSpecification.Type.UNSPECIFIED, (Identifier) visit(((SqlBaseParser.UnspecifiedPrincipalContext) context).identifier()));
        }
        else if (context instanceof SqlBaseParser.UserPrincipalContext) {
            return new PrincipalSpecification(PrincipalSpecification.Type.USER, (Identifier) visit(((SqlBaseParser.UserPrincipalContext) context).identifier()));
        }
        else if (context instanceof SqlBaseParser.RolePrincipalContext) {
            return new PrincipalSpecification(PrincipalSpecification.Type.ROLE, (Identifier) visit(((SqlBaseParser.RolePrincipalContext) context).identifier()));
        }
        else {
            throw new IllegalArgumentException("Unsupported principal: " + context);
        }
    }

    private enum UnicodeDecodeState
    {
        EMPTY,
        ESCAPED,
        UNICODE_SEQUENCE
    }
}
