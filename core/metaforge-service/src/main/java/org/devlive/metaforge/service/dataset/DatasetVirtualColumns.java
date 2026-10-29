package org.devlive.metaforge.service.dataset;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.devlive.metaforge.common.sql.SqlBuilder;
import org.devlive.metaforge.common.sql.configure.SqlBody;
import org.devlive.metaforge.common.sql.configure.SqlColumn;
import org.devlive.metaforge.common.sql.configure.SqlType;
import org.devlive.metaforge.service.entity.DataSetColumnEntity;
import org.devlive.metaforge.sql.parser.SqlBaseLexer;
import org.devlive.metaforge.sql.parser.SqlBaseParser;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Persistent row-level expressions are evaluated by the dataset storage engine. */
public final class DatasetVirtualColumns
{
    private static final Set<String> AGGREGATES = Set.of("sum", "avg", "count", "min", "max", "uniq", "uniqexact", "any", "anylast",
            "argmin", "argmax", "grouparray", "groupuniqarray", "median", "quantile", "stddevpop", "stddevsamp", "varpop", "varsamp");

    private DatasetVirtualColumns() {}

    public static void validate(Collection<DataSetColumnEntity> columns)
    {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Dataset columns are required");
        }
        Set<String> names = new HashSet<>();
        for (DataSetColumnEntity column : columns) {
            if (column.getName() == null || column.getName().trim().isEmpty() || !names.add(column.getName())) {
                throw new IllegalArgumentException("Column names must be non-empty and unique");
            }
        }
        Set<String> physical = columns.stream().filter(column -> !column.isVirtualColumn()).map(DataSetColumnEntity::getName).collect(Collectors.toSet());
        for (DataSetColumnEntity column : columns) {
            if (column.isVirtualColumn()) {
                normalize(column.getFormula(), physical);
            }
        }
    }

    public static String normalize(String expression, Set<String> physicalColumns)
    {
        if (expression == null || expression.trim().isEmpty() || expression.length() > 4000) {
            throw new IllegalArgumentException("Virtual column expression is required and must not exceed 4000 characters");
        }
        BaseErrorListener errors = new BaseErrorListener()
        {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object symbol, int line, int position, String message, RecognitionException exception)
            {
                throw new IllegalArgumentException("Invalid virtual column expression at " + line + ":" + position + ": " + message);
            }
        };
        SqlBaseLexer lexer = new SqlBaseLexer(CharStreams.fromString(expression));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errors);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SqlBaseParser parser = new SqlBaseParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errors);
        ParseTree tree = parser.expression();
        if (parser.getCurrentToken().getType() != Token.EOF) {
            throw new IllegalArgumentException("A virtual column must contain one expression, without SQL statements or aliases");
        }
        checkReferences(tree, physicalColumns);
        // Rebuild from parsed tokens so skipped comments cannot swallow the generated SQL.
        tokens.fill();
        return tokens.getTokens().stream().filter(token -> token.getType() != Token.EOF && token.getChannel() == Token.DEFAULT_CHANNEL).map(Token::getText).collect(Collectors.joining(" "));
    }

    private static void checkReferences(ParseTree tree, Set<String> physical)
    {
        if (tree instanceof SqlBaseParser.ColumnReferenceContext) {
            SqlBaseParser.ColumnReferenceContext reference = (SqlBaseParser.ColumnReferenceContext) tree;
            String name = reference.columnName().getText();
            if (name.startsWith("`") && name.endsWith("`")) {
                name = name.substring(1, name.length() - 1).replace("``", "`");
            }
            if (reference.tableName() != null || !physical.contains(name)) {
                throw new IllegalArgumentException("Unknown physical column in virtual expression: " + name);
            }
        }
        if (tree instanceof SqlBaseParser.WindowFunctionContext) {
            throw new IllegalArgumentException("Window functions cannot be used in a row-level virtual column");
        }
        if (tree instanceof SqlBaseParser.FunctionCallContext) {
            SqlBaseParser.FunctionCallContext call = (SqlBaseParser.FunctionCallContext) tree;
            if (call.functionName() != null) {
                if (isAggregateFunction(call.functionName().getText())) {
                    throw new IllegalArgumentException("Configure aggregation in the adhoc metric settings; virtual columns use row-level expressions");
                }
            }
        }
        for (int index = 0; index < tree.getChildCount(); index++) {
            checkReferences(tree.getChild(index), physical);
        }
    }

    public static boolean isAggregateFunction(String name)
    {
        String function = name == null ? "" : name.replace("`", "").toLowerCase(Locale.ROOT);
        return AGGREGATES.contains(function) || function.matches("(sum|avg|count|uniq|uniqexact|min|max|any|argmin|argmax)(if|ornull|state|merge|distinct)+");
    }

    public static String table(String database, String table, Collection<DataSetColumnEntity> columns, Set<Long> selected)
    {
        List<DataSetColumnEntity> virtual = columns.stream().filter(column -> column.isVirtualColumn() && selected.contains(column.getId())
                && column.getFormula() != null && !column.getFormula().trim().isEmpty()).collect(Collectors.toList());
        if (virtual.isEmpty()) {
            return null;
        }
        Set<String> physical = columns.stream().filter(column -> !column.isVirtualColumn()).map(DataSetColumnEntity::getName).collect(Collectors.toSet());
        String projections = virtual.stream().map(column -> "(" + normalize(column.getFormula(), physical) + ") AS " + quote(column.getName())).collect(Collectors.joining(", "));
        return "(SELECT *, " + projections + " FROM " + quote(database) + "." + quote(table) + ") AS dataset_rows";
    }

    public static String previewSql(String database, String table, Collection<DataSetColumnEntity> columns)
    {
        List<DataSetColumnEntity> available = columns.stream().filter(column -> !column.isVirtualColumn()
                || (column.getFormula() != null && !column.getFormula().isBlank())).collect(Collectors.toList());
        if (available.isEmpty()) {
            throw new IllegalArgumentException("No previewable dataset columns");
        }
        String derived = table(database, table, available, available.stream().map(DataSetColumnEntity::getId).collect(Collectors.toSet()));
        return new SqlBuilder(SqlBody.builder().type(SqlType.SELECT)
                .database(derived == null ? database : null).table(derived == null ? table : derived)
                .columns(available.stream().map(column -> SqlColumn.builder().column(quote(column.getName()))
                        .alias((column.getAliasName() == null || column.getAliasName().isBlank() ? column.getName() : column.getAliasName()).replace("`", "``"))
                        .build()).collect(Collectors.toList())).limit(100).build()).getSql();
    }

    private static String quote(String value)
    {
        return "`" + value.replace("`", "``") + "`";
    }
}
