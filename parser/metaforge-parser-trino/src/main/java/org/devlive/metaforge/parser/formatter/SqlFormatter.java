package org.devlive.metaforge.parser.formatter;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.devlive.metaforge.parser.ast.AstVisitor;
import org.devlive.metaforge.parser.node.Node;
import org.devlive.metaforge.parser.tree.AddColumn;
import org.devlive.metaforge.parser.tree.AliasedRelation;
import org.devlive.metaforge.parser.tree.AllColumns;
import org.devlive.metaforge.parser.tree.Analyze;
import org.devlive.metaforge.parser.tree.Call;
import org.devlive.metaforge.parser.tree.CallArgument;
import org.devlive.metaforge.parser.tree.ColumnDefinition;
import org.devlive.metaforge.parser.tree.Comment;
import org.devlive.metaforge.parser.tree.Commit;
import org.devlive.metaforge.parser.tree.CreateRole;
import org.devlive.metaforge.parser.tree.CreateSchema;
import org.devlive.metaforge.parser.tree.CreateTable;
import org.devlive.metaforge.parser.tree.CreateTableAsSelect;
import org.devlive.metaforge.parser.tree.CreateView;
import org.devlive.metaforge.parser.tree.Deallocate;
import org.devlive.metaforge.parser.tree.Delete;
import org.devlive.metaforge.parser.tree.DescribeInput;
import org.devlive.metaforge.parser.tree.DescribeOutput;
import org.devlive.metaforge.parser.tree.DropColumn;
import org.devlive.metaforge.parser.tree.DropRole;
import org.devlive.metaforge.parser.tree.DropSchema;
import org.devlive.metaforge.parser.tree.DropTable;
import org.devlive.metaforge.parser.tree.DropView;
import org.devlive.metaforge.parser.tree.Except;
import org.devlive.metaforge.parser.tree.Execute;
import org.devlive.metaforge.parser.tree.Explain;
import org.devlive.metaforge.parser.tree.ExplainFormat;
import org.devlive.metaforge.parser.tree.ExplainOption;
import org.devlive.metaforge.parser.tree.ExplainType;
import org.devlive.metaforge.parser.tree.Expression;
import org.devlive.metaforge.parser.tree.FetchFirst;
import org.devlive.metaforge.parser.tree.Grant;
import org.devlive.metaforge.parser.tree.GrantRoles;
import org.devlive.metaforge.parser.tree.GrantorSpecification;
import org.devlive.metaforge.parser.tree.Identifier;
import org.devlive.metaforge.parser.tree.Insert;
import org.devlive.metaforge.parser.tree.Intersect;
import org.devlive.metaforge.parser.tree.Isolation;
import org.devlive.metaforge.parser.tree.Lateral;
import org.devlive.metaforge.parser.tree.LikeClause;
import org.devlive.metaforge.parser.tree.Limit;
import org.devlive.metaforge.parser.tree.NaturalJoin;
import org.devlive.metaforge.parser.tree.Offset;
import org.devlive.metaforge.parser.tree.OrderBy;
import org.devlive.metaforge.parser.tree.Prepare;
import org.devlive.metaforge.parser.tree.PrincipalSpecification;
import org.devlive.metaforge.parser.tree.Property;
import org.devlive.metaforge.parser.tree.QualifiedName;
import org.devlive.metaforge.parser.tree.Relation;
import org.devlive.metaforge.parser.tree.RenameColumn;
import org.devlive.metaforge.parser.tree.RenameSchema;
import org.devlive.metaforge.parser.tree.RenameTable;
import org.devlive.metaforge.parser.tree.RenameView;
import org.devlive.metaforge.parser.tree.ResetSession;
import org.devlive.metaforge.parser.tree.Revoke;
import org.devlive.metaforge.parser.tree.RevokeRoles;
import org.devlive.metaforge.parser.tree.Rollback;
import org.devlive.metaforge.parser.tree.Row;
import org.devlive.metaforge.parser.tree.SampledRelation;
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
import org.devlive.metaforge.parser.tree.SingleColumn;
import org.devlive.metaforge.parser.tree.StartTransaction;
import org.devlive.metaforge.parser.tree.TransactionAccessMode;
import org.devlive.metaforge.parser.tree.TransactionMode;
import org.devlive.metaforge.parser.tree.Union;
import org.devlive.metaforge.parser.tree.Unnest;
import org.devlive.metaforge.parser.tree.Values;
import org.devlive.metaforge.parser.tree.With;
import org.devlive.metaforge.parser.tree.WithQuery;
import org.devlive.metaforge.parser.tree.join.Join;
import org.devlive.metaforge.parser.tree.join.JoinCriteria;
import org.devlive.metaforge.parser.tree.join.JoinOn;
import org.devlive.metaforge.parser.tree.join.JoinUsing;
import org.devlive.metaforge.parser.tree.query.Query;
import org.devlive.metaforge.parser.tree.query.QuerySpecification;
import org.devlive.metaforge.parser.tree.table.Table;
import org.devlive.metaforge.parser.tree.table.TableSubQuery;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.devlive.metaforge.parser.formatter.ExpressionFormatter.formatExpression;
import static org.devlive.metaforge.parser.formatter.ExpressionFormatter.formatStringLiteral;

public final class SqlFormatter
{
    private static final String INDENT = "   ";

    private SqlFormatter()
    {
    }

    public static String formatSql(Node root)
    {
        StringBuilder builder = new StringBuilder();
        new Formatter(builder).process(root, 0);
        return builder.toString();
    }

    static String formatName(QualifiedName name)
    {
        return name.getOriginalParts().stream()
                .map(ExpressionFormatter::formatExpression)
                .collect(joining("."));
    }

    private static void appendAliasColumns(StringBuilder builder, List<Identifier> columns)
    {
        if ((columns != null) && (!columns.isEmpty())) {
            String formattedColumns = columns.stream()
                    .map(ExpressionFormatter::formatExpression)
                    .collect(Collectors.joining(", "));

            builder.append(" (")
                    .append(formattedColumns)
                    .append(')');
        }
    }

    private static class Formatter
            extends AstVisitor<Void, Integer>
    {
        private final StringBuilder builder;

        public Formatter(StringBuilder builder)
        {
            this.builder = builder;
        }

        private static String formatGrantor(GrantorSpecification grantor)
        {
            GrantorSpecification.Type type = grantor.getType();
            switch (type) {
                case CURRENT_ROLE:
                case CURRENT_USER:
                    return type.name();
                case PRINCIPAL:
                    return formatPrincipal(grantor.getPrincipal().get());
                default:
                    throw new IllegalArgumentException("Unsupported principal type: " + type);
            }
        }

        private static String formatPrincipal(PrincipalSpecification principal)
        {
            PrincipalSpecification.Type type = principal.getType();
            switch (type) {
                case UNSPECIFIED:
                    return principal.getName().toString();
                case USER:
                case ROLE:
                    return format("%s %s", type.name(), principal.getName().toString());
                default:
                    throw new IllegalArgumentException("Unsupported principal type: " + type);
            }
        }

        private static String indentString(int indent)
        {
            return Strings.repeat(INDENT, indent);
        }

        @Override
        public Void visitNode(Node node, Integer indent)
        {
            throw new UnsupportedOperationException("not yet implemented: " + node);
        }

        @Override
        public Void visitExpression(Expression node, Integer indent)
        {
            checkArgument(indent == 0, "visitExpression should only be called at root");
            builder.append(ExpressionFormatter.formatExpression(node));
            return null;
        }

        @Override
        public Void visitUnnest(Unnest node, Integer indent)
        {
            builder.append("UNNEST(")
                    .append(node.getExpressions().stream()
                            .map(ExpressionFormatter::formatExpression)
                            .collect(joining(", ")))
                    .append(")");
            if (node.isWithOrdinality()) {
                builder.append(" WITH ORDINALITY");
            }
            return null;
        }

        @Override
        public Void visitLateral(Lateral node, Integer indent)
        {
            append(indent, "LATERAL (");
            process(node.getQuery(), indent + 1);
            append(indent, ")");
            return null;
        }

        @Override
        public Void visitPrepare(Prepare node, Integer indent)
        {
            append(indent, "PREPARE ");
            builder.append(node.getName());
            builder.append(" FROM");
            builder.append("\n");
            process(node.getStatement(), indent + 1);
            return null;
        }

        @Override
        public Void visitDeallocate(Deallocate node, Integer indent)
        {
            append(indent, "DEALLOCATE PREPARE ");
            builder.append(node.getName());
            return null;
        }

        @Override
        public Void visitExecute(Execute node, Integer indent)
        {
            append(indent, "EXECUTE ");
            builder.append(node.getName());
            List<Expression> parameters = node.getParameters();
            if (!parameters.isEmpty()) {
                builder.append(" USING ");
                Joiner.on(", ").appendTo(builder, parameters);
            }
            return null;
        }

        @Override
        public Void visitDescribeOutput(DescribeOutput node, Integer indent)
        {
            append(indent, "DESCRIBE OUTPUT ");
            builder.append(node.getName());
            return null;
        }

        @Override
        public Void visitDescribeInput(DescribeInput node, Integer indent)
        {
            append(indent, "DESCRIBE INPUT ");
            builder.append(node.getName());
            return null;
        }

        @Override
        public Void visitQuery(Query node, Integer indent)
        {
            if (node.getWith().isPresent()) {
                With with = node.getWith().get();
                append(indent, "WITH");
                if (with.isRecursive()) {
                    builder.append(" RECURSIVE");
                }
                builder.append("\n  ");
                Iterator<WithQuery> queries = with.getQueries().iterator();
                while (queries.hasNext()) {
                    WithQuery query = queries.next();
                    append(indent, ExpressionFormatter.formatExpression(query.getName()));
                    query.getColumnNames().ifPresent(columnNames -> appendAliasColumns(builder, columnNames));
                    builder.append(" AS ");
                    process(new TableSubQuery(query.getQuery()), indent);
                    builder.append('\n');
                    if (queries.hasNext()) {
                        builder.append(", ");
                    }
                }
            }
            processRelation(node.getQueryBody(), indent);
            if (node.getOrderBy().isPresent()) {
                process(node.getOrderBy().get(), indent);
            }
            if (node.getOffset().isPresent()) {
                process(node.getOffset().get(), indent);
            }
            if (node.getLimit().isPresent()) {
                process(node.getLimit().get(), indent);
            }
            return null;
        }

        @Override
        public Void visitQuerySpecification(QuerySpecification node, Integer indent)
        {
            process(node.getSelect(), indent);
            if (node.getFrom().isPresent()) {
                append(indent, "FROM");
                builder.append('\n');
                append(indent, "  ");
                process(node.getFrom().get(), indent);
            }

            builder.append('\n');

            if (node.getWhere().isPresent()) {
                append(indent, "WHERE " + ExpressionFormatter.formatExpression(node.getWhere().get()))
                        .append('\n');
            }

            if (node.getGroupBy().isPresent()) {
                append(indent, "GROUP BY " + (node.getGroupBy().get().isDistinct() ? " DISTINCT " : "") + ExpressionFormatter.formatGroupBy(node.getGroupBy().get().getGroupingElements())).append('\n');
            }

            if (node.getHaving().isPresent()) {
                append(indent, "HAVING " + ExpressionFormatter.formatExpression(node.getHaving().get()))
                        .append('\n');
            }

            if (node.getOrderBy().isPresent()) {
                process(node.getOrderBy().get(), indent);
            }

            if (node.getOffset().isPresent()) {
                process(node.getOffset().get(), indent);
            }

            if (node.getLimit().isPresent()) {
                process(node.getLimit().get(), indent);
            }
            return null;
        }

        @Override
        public Void visitOrderBy(OrderBy node, Integer indent)
        {
            append(indent, ExpressionFormatter.formatOrderBy(node))
                    .append('\n');
            return null;
        }

        @Override
        public Void visitOffset(Offset node, Integer indent)
        {
            append(indent, "OFFSET " + node.getRowCount() + " ROWS")
                    .append('\n');
            return null;
        }

        @Override
        public Void visitFetchFirst(FetchFirst node, Integer indent)
        {
            append(indent, "FETCH FIRST " + node.getRowCount().map(c -> c + " ROWS ").orElse("ROW "))
                    .append(node.isWithTies() ? "WITH TIES" : "ONLY")
                    .append('\n');
            return null;
        }

        @Override
        public Void visitLimit(Limit node, Integer indent)
        {
            append(indent, "LIMIT ")
                    .append(ExpressionFormatter.formatExpression(node.getRowCount()))
                    .append('\n');
            return null;
        }

        @Override
        public Void visitSelect(Select node, Integer indent)
        {
            append(indent, "SELECT");
            if (node.isDistinct()) {
                builder.append(" DISTINCT");
            }

            if (node.getSelectItems().size() > 1) {
                boolean first = true;
                for (SelectItem item : node.getSelectItems()) {
                    builder.append("\n")
                            .append(indentString(indent))
                            .append(first ? "  " : ", ");

                    process(item, indent);
                    first = false;
                }
            }
            else {
                builder.append(' ');
                process(getOnlyElement(node.getSelectItems()), indent);
            }

            builder.append('\n');

            return null;
        }

        @Override
        public Void visitSingleColumn(SingleColumn node, Integer indent)
        {
            builder.append(ExpressionFormatter.formatExpression(node.getExpression()));
            if (node.getAlias().isPresent()) {
                builder.append(' ')
                        .append(ExpressionFormatter.formatExpression(node.getAlias().get()));
            }

            return null;
        }

        @Override
        public Void visitAllColumns(AllColumns node, Integer context)
        {
            node.getTarget().ifPresent(value -> builder
                    .append(ExpressionFormatter.formatExpression(value))
                    .append("."));
            builder.append("*");

            if (!node.getAliases().isEmpty()) {
                builder.append(" AS (")
                        .append(Joiner.on(", ").join(node.getAliases().stream()
                                .map(ExpressionFormatter::formatExpression)
                                .collect(toImmutableList())))
                        .append(")");
            }

            return null;
        }

        @Override
        public Void visitTable(Table node, Integer indent)
        {
            builder.append(formatName(node.getName()));
            return null;
        }

        @Override
        public Void visitJoin(Join node, Integer indent)
        {
            JoinCriteria criteria = node.getCriteria().orElse(null);
            String type = node.getType().toString();
            if (criteria instanceof NaturalJoin) {
                type = "NATURAL " + type;
            }

            if (node.getType() != Join.Type.IMPLICIT) {
                builder.append('(');
            }
            process(node.getLeft(), indent);

            builder.append('\n');
            if (node.getType() == Join.Type.IMPLICIT) {
                append(indent, ", ");
            }
            else {
                append(indent, type).append(" JOIN ");
            }

            process(node.getRight(), indent);

            if (node.getType() != Join.Type.CROSS && node.getType() != Join.Type.IMPLICIT) {
                if (criteria instanceof JoinUsing) {
                    JoinUsing using = (JoinUsing) criteria;
                    builder.append(" USING (")
                            .append(Joiner.on(", ").join(using.getColumns()))
                            .append(")");
                }
                else if (criteria instanceof JoinOn) {
                    JoinOn on = (JoinOn) criteria;
                    builder.append(" ON ")
                            .append(ExpressionFormatter.formatExpression(on.getExpression()));
                }
                else if (!(criteria instanceof NaturalJoin)) {
                    throw new UnsupportedOperationException("unknown join criteria: " + criteria);
                }
            }

            if (node.getType() != Join.Type.IMPLICIT) {
                builder.append(")");
            }

            return null;
        }

        @Override
        public Void visitAliasedRelation(AliasedRelation node, Integer indent)
        {
            processRelationSuffix(node.getRelation(), indent);

            builder.append(' ')
                    .append(ExpressionFormatter.formatExpression(node.getAlias()));
            appendAliasColumns(builder, node.getColumnNames());

            return null;
        }

        @Override
        public Void visitSampledRelation(SampledRelation node, Integer indent)
        {
            processRelationSuffix(node.getRelation(), indent);

            builder.append(" TABLESAMPLE ")
                    .append(node.getType())
                    .append(" (")
                    .append(node.getSamplePercentage())
                    .append(')');

            return null;
        }

        private void processRelationSuffix(Relation relation, Integer indent)
        {
            if ((relation instanceof AliasedRelation) || (relation instanceof SampledRelation)) {
                builder.append("( ");
                process(relation, indent + 1);
                append(indent, ")");
            }
            else {
                process(relation, indent);
            }
        }

        @Override
        public Void visitValues(Values node, Integer indent)
        {
            builder.append(" VALUES ");

            boolean first = true;
            for (Expression row : node.getRows()) {
                builder.append("\n")
                        .append(indentString(indent))
                        .append(first ? "  " : ", ");

                builder.append(ExpressionFormatter.formatExpression(row));
                first = false;
            }
            builder.append('\n');

            return null;
        }

        @Override
        public Void visitTableSubquery(TableSubQuery node, Integer indent)
        {
            builder.append('(')
                    .append('\n');

            process(node.getQuery(), indent + 1);

            append(indent, ") ");

            return null;
        }

        @Override
        public Void visitUnion(Union node, Integer indent)
        {
            Iterator<Relation> relations = node.getRelations().iterator();

            while (relations.hasNext()) {
                processRelation(relations.next(), indent);

                if (relations.hasNext()) {
                    builder.append("UNION ");
                    if (!node.isDistinct()) {
                        builder.append("ALL ");
                    }
                }
            }

            return null;
        }

        @Override
        public Void visitExcept(Except node, Integer indent)
        {
            processRelation(node.getLeft(), indent);

            builder.append("EXCEPT ");
            if (!node.isDistinct()) {
                builder.append("ALL ");
            }

            processRelation(node.getRight(), indent);

            return null;
        }

        @Override
        public Void visitIntersect(Intersect node, Integer indent)
        {
            Iterator<Relation> relations = node.getRelations().iterator();

            while (relations.hasNext()) {
                processRelation(relations.next(), indent);

                if (relations.hasNext()) {
                    builder.append("INTERSECT ");
                    if (!node.isDistinct()) {
                        builder.append("ALL ");
                    }
                }
            }

            return null;
        }

        @Override
        public Void visitCreateView(CreateView node, Integer indent)
        {
            builder.append("CREATE ");
            if (node.isReplace()) {
                builder.append("OR REPLACE ");
            }
            builder.append("VIEW ")
                    .append(formatName(node.getName()));

            node.getComment().ifPresent(comment ->
                    builder.append(" COMMENT ")
                            .append(ExpressionFormatter.formatStringLiteral(comment)));

            node.getSecurity().ifPresent(security ->
                    builder.append(" SECURITY ")
                            .append(security.toString()));

            builder.append(" AS\n");

            process(node.getQuery(), indent);

            return null;
        }

        @Override
        public Void visitRenameView(RenameView node, Integer context)
        {
            builder.append("ALTER VIEW ")
                    .append(node.getSource())
                    .append(" RENAME TO ")
                    .append(node.getTarget());

            return null;
        }

        @Override
        public Void visitDropView(DropView node, Integer context)
        {
            builder.append("DROP VIEW ");
            if (node.isExists()) {
                builder.append("IF EXISTS ");
            }
            builder.append(node.getName());

            return null;
        }

        @Override
        public Void visitExplain(Explain node, Integer indent)
        {
            builder.append("EXPLAIN ");
            if (node.isAnalyze()) {
                builder.append("ANALYZE ");
            }

            List<String> options = new ArrayList<>();

            for (ExplainOption option : node.getOptions()) {
                if (option instanceof ExplainType) {
                    options.add("TYPE " + ((ExplainType) option).getType());
                }
                else if (option instanceof ExplainFormat) {
                    options.add("FORMAT " + ((ExplainFormat) option).getType());
                }
                else {
                    throw new UnsupportedOperationException("unhandled explain option: " + option);
                }
            }

            if (!options.isEmpty()) {
                builder.append("(");
                Joiner.on(", ").appendTo(builder, options);
                builder.append(")");
            }

            builder.append("\n");

            process(node.getStatement(), indent);

            return null;
        }

        @Override
        public Void visitShowCatalogs(ShowCatalogs node, Integer context)
        {
            builder.append("SHOW CATALOGS");

            node.getLikePattern().ifPresent((value) ->
                    builder.append(" LIKE ")
                            .append(ExpressionFormatter.formatStringLiteral(value)));

            node.getEscape().ifPresent((value) ->
                    builder.append(" ESCAPE ")
                            .append(ExpressionFormatter.formatStringLiteral(value)));

            return null;
        }

        @Override
        public Void visitShowSchemas(ShowSchemas node, Integer context)
        {
            builder.append("SHOW SCHEMAS");

            if (node.getCatalog().isPresent()) {
                builder.append(" FROM ")
                        .append(node.getCatalog().get());
            }

            node.getLikePattern().ifPresent((value) ->
                    builder.append(" LIKE ")
                            .append(ExpressionFormatter.formatStringLiteral(value)));

            node.getEscape().ifPresent((value) ->
                    builder.append(" ESCAPE ")
                            .append(ExpressionFormatter.formatStringLiteral(value)));

            return null;
        }

        @Override
        public Void visitShowTables(ShowTables node, Integer context)
        {
            builder.append("SHOW TABLES");

            node.getSchema().ifPresent(value ->
                    builder.append(" FROM ")
                            .append(formatName(value)));

            node.getLikePattern().ifPresent(value ->
                    builder.append(" LIKE ")
                            .append(formatStringLiteral(value)));

            node.getEscape().ifPresent(value ->
                    builder.append(" ESCAPE ")
                            .append(formatStringLiteral(value)));

            return null;
        }

        @Override
        public Void visitShowCreate(ShowCreate node, Integer context)
        {
            if (node.getType() == ShowCreate.Type.TABLE) {
                builder.append("SHOW CREATE TABLE ")
                        .append(formatName(node.getName()));
            }
            else if (node.getType() == ShowCreate.Type.VIEW) {
                builder.append("SHOW CREATE VIEW ")
                        .append(formatName(node.getName()));
            }

            return null;
        }

        @Override
        public Void visitShowColumns(ShowColumns node, Integer context)
        {
            builder.append("SHOW COLUMNS FROM ")
                    .append(formatName(node.getTable()));

            node.getLikePattern().ifPresent(value ->
                    builder.append(" LIKE ")
                            .append(formatStringLiteral(value)));

            node.getEscape().ifPresent(value ->
                    builder.append(" ESCAPE ")
                            .append(formatStringLiteral(value)));

            return null;
        }

        @Override
        public Void visitShowStats(ShowStats node, Integer context)
        {
            builder.append("SHOW STATS FOR ");
            process(node.getRelation(), 0);

            return null;
        }

        @Override
        public Void visitShowFunctions(ShowFunctions node, Integer context)
        {
            builder.append("SHOW FUNCTIONS");

            node.getLikePattern().ifPresent((value) ->
                    builder.append(" LIKE ")
                            .append(formatStringLiteral(value)));

            node.getEscape().ifPresent((value) ->
                    builder.append(" ESCAPE ")
                            .append(formatStringLiteral(value)));

            return null;
        }

        @Override
        public Void visitShowSession(ShowSession node, Integer context)
        {
            builder.append("SHOW SESSION");

            node.getLikePattern().ifPresent((value) ->
                    builder.append(" LIKE ")
                            .append(formatStringLiteral(value)));

            node.getEscape().ifPresent((value) ->
                    builder.append(" ESCAPE ")
                            .append(formatStringLiteral(value)));

            return null;
        }

        @Override
        public Void visitDelete(Delete node, Integer context)
        {
            builder.append("DELETE FROM ")
                    .append(formatName(node.getTable().getName()));
            if (node.getWhere().isPresent()) {
                builder.append(" WHERE ")
                        .append(formatExpression(node.getWhere().get()));
            }
            return null;
        }

        @Override
        public Void visitCreateSchema(CreateSchema node, Integer context)
        {
            builder.append("CREATE SCHEMA ");
            if (node.isNotExists()) {
                builder.append("IF NOT EXISTS ");
            }
            builder.append(formatName(node.getSchemaName()));
            if (node.getPrincipal().isPresent()) {
                builder.append("\nAUTHORIZATION ")
                        .append(formatPrincipal(node.getPrincipal().get()));
            }
            builder.append(formatPropertiesMultiLine(node.getProperties()));

            return null;
        }

        @Override
        public Void visitDropSchema(DropSchema node, Integer context)
        {
            builder.append("DROP SCHEMA ");
            if (node.isExists()) {
                builder.append("IF EXISTS ");
            }
            builder.append(formatName(node.getSchemaName()))
                    .append(" ")
                    .append(node.isCascade() ? "CASCADE" : "RESTRICT");

            return null;
        }

        @Override
        public Void visitRenameSchema(RenameSchema node, Integer context)
        {
            builder.append("ALTER SCHEMA ")
                    .append(formatName(node.getSource()))
                    .append(" RENAME TO ")
                    .append(formatExpression(node.getTarget()));

            return null;
        }

        @Override
        public Void visitSetSchemaAuthorization(SetSchemaAuthorization node, Integer context)
        {
            builder.append("ALTER SCHEMA ")
                    .append(formatName(node.getSource()))
                    .append(" SET AUTHORIZATION ")
                    .append(formatPrincipal(node.getPrincipal()));

            return null;
        }

        @Override
        public Void visitCreateTableAsSelect(CreateTableAsSelect node, Integer indent)
        {
            builder.append("CREATE TABLE ");
            if (node.isNotExists()) {
                builder.append("IF NOT EXISTS ");
            }
            builder.append(formatName(node.getName()));

            if (node.getColumnAliases().isPresent()) {
                String columnList = node.getColumnAliases().get().stream()
                        .map(ExpressionFormatter::formatExpression)
                        .collect(joining(", "));
                builder.append(format("( %s )", columnList));
            }

            if (node.getComment().isPresent()) {
                builder.append("\nCOMMENT ")
                        .append(formatStringLiteral(node.getComment().get()));
            }

            builder.append(formatPropertiesMultiLine(node.getProperties()));

            builder.append(" AS ");
            process(node.getQuery(), indent);

            if (!node.isWithData()) {
                builder.append(" WITH NO DATA");
            }

            return null;
        }

        @Override
        public Void visitCreateTable(CreateTable node, Integer indent)
        {
            builder.append("CREATE TABLE ");
            if (node.isNotExists()) {
                builder.append("IF NOT EXISTS ");
            }
            String tableName = formatName(node.getName());
            builder.append(tableName).append(" (\n");

            String elementIndent = indentString(indent + 1);
            String columnList = node.getElements().stream()
                    .map(element -> {
                        if (element instanceof ColumnDefinition) {
                            ColumnDefinition column = (ColumnDefinition) element;
                            return elementIndent + formatColumnDefinition(column);
                        }
                        if (element instanceof LikeClause) {
                            LikeClause likeClause = (LikeClause) element;
                            StringBuilder builder = new StringBuilder(elementIndent);
                            builder.append("LIKE ")
                                    .append(formatName(likeClause.getTableName()));
                            if (likeClause.getPropertiesOption().isPresent()) {
                                builder.append(" ")
                                        .append(likeClause.getPropertiesOption().get().name())
                                        .append(" PROPERTIES");
                            }
                            return builder.toString();
                        }
                        throw new UnsupportedOperationException("unknown table element: " + element);
                    })
                    .collect(joining(",\n"));
            builder.append(columnList);
            builder.append("\n").append(")");

            if (node.getComment().isPresent()) {
                builder.append("\nCOMMENT ")
                        .append(formatStringLiteral(node.getComment().get()));
            }

            builder.append(formatPropertiesMultiLine(node.getProperties()));

            return null;
        }

        private String formatPropertiesMultiLine(List<Property> properties)
        {
            if (properties.isEmpty()) {
                return "";
            }

            String propertyList = properties.stream()
                    .map(element -> INDENT +
                            formatExpression(element.getName()) + " = " +
                            formatExpression(element.getValue()))
                    .collect(joining(",\n"));

            return "\nWITH (\n" + propertyList + "\n)";
        }

        private String formatPropertiesSingleLine(List<Property> properties)
        {
            if (properties.isEmpty()) {
                return "";
            }

            String propertyList = properties.stream()
                    .map(element -> formatExpression(element.getName()) + " = " +
                            formatExpression(element.getValue()))
                    .collect(joining(", "));

            return " WITH ( " + propertyList + " )";
        }

        private String formatColumnDefinition(ColumnDefinition column)
        {
            StringBuilder sb = new StringBuilder()
                    .append(formatExpression(column.getName()))
                    .append(" ").append(column.getType());
            if (!column.isNullable()) {
                sb.append(" NOT NULL");
            }
            column.getComment().ifPresent(comment ->
                    sb.append(" COMMENT ").append(formatStringLiteral(comment)));
            sb.append(formatPropertiesSingleLine(column.getProperties()));
            return sb.toString();
        }

        @Override
        public Void visitDropTable(DropTable node, Integer context)
        {
            builder.append("DROP TABLE ");
            if (node.isExists()) {
                builder.append("IF EXISTS ");
            }
            builder.append(node.getTableName());

            return null;
        }

        @Override
        public Void visitRenameTable(RenameTable node, Integer context)
        {
            builder.append("ALTER TABLE ")
                    .append(node.getSource())
                    .append(" RENAME TO ")
                    .append(node.getTarget());

            return null;
        }

        @Override
        public Void visitComment(Comment node, Integer context)
        {
            String comment = node.getComment().isPresent() ? formatStringLiteral(node.getComment().get()) : "NULL";

            switch (node.getType()) {
                case TABLE:
                    builder.append("COMMENT ON TABLE ")
                            .append(node.getName())
                            .append(" IS ")
                            .append(comment);
                    break;
                case COLUMN:
                    builder.append("COMMENT ON COLUMN ")
                            .append(node.getName())
                            .append(" IS ")
                            .append(comment);
                    break;
            }

            return null;
        }

        @Override
        public Void visitRenameColumn(RenameColumn node, Integer context)
        {
            builder.append("ALTER TABLE ")
                    .append(node.getTable())
                    .append(" RENAME COLUMN ")
                    .append(node.getSource())
                    .append(" TO ")
                    .append(node.getTarget());

            return null;
        }

        @Override
        public Void visitDropColumn(DropColumn node, Integer context)
        {
            builder.append("ALTER TABLE ")
                    .append(formatName(node.getTable()))
                    .append(" DROP COLUMN ")
                    .append(formatExpression(node.getColumn()));

            return null;
        }

        @Override
        public Void visitAnalyze(Analyze node, Integer context)
        {
            builder.append("ANALYZE ")
                    .append(formatName(node.getTableName()));
            builder.append(formatPropertiesMultiLine(node.getProperties()));
            return null;
        }

        @Override
        public Void visitAddColumn(AddColumn node, Integer indent)
        {
            builder.append("ALTER TABLE ")
                    .append(node.getName())
                    .append(" ADD COLUMN ")
                    .append(formatColumnDefinition(node.getColumn()));

            return null;
        }

        @Override
        public Void visitInsert(Insert node, Integer indent)
        {
            builder.append("INSERT INTO ")
                    .append(node.getTarget());

            if (node.getColumns().isPresent()) {
                builder.append(" (")
                        .append(Joiner.on(", ").join(node.getColumns().get()))
                        .append(")");
            }

            builder.append("\n");

            process(node.getQuery(), indent);

            return null;
        }

        @Override
        public Void visitSetSession(SetSession node, Integer context)
        {
            builder.append("SET SESSION ")
                    .append(node.getName())
                    .append(" = ")
                    .append(formatExpression(node.getValue()));

            return null;
        }

        @Override
        public Void visitResetSession(ResetSession node, Integer context)
        {
            builder.append("RESET SESSION ")
                    .append(node.getName());

            return null;
        }

        @Override
        public Void visitCallArgument(CallArgument node, Integer indent)
        {
            if (node.getName().isPresent()) {
                builder.append(node.getName().get())
                        .append(" => ");
            }
            builder.append(formatExpression(node.getValue()));

            return null;
        }

        @Override
        public Void visitCall(Call node, Integer indent)
        {
            builder.append("CALL ")
                    .append(node.getName())
                    .append("(");

            Iterator<CallArgument> arguments = node.getArguments().iterator();
            while (arguments.hasNext()) {
                process(arguments.next(), indent);
                if (arguments.hasNext()) {
                    builder.append(", ");
                }
            }

            builder.append(")");

            return null;
        }

        @Override
        public Void visitRow(Row node, Integer indent)
        {
            builder.append("ROW(");
            boolean firstItem = true;
            for (Expression item : node.getItems()) {
                if (!firstItem) {
                    builder.append(", ");
                }
                process(item, indent);
                firstItem = false;
            }
            builder.append(")");
            return null;
        }

        @Override
        public Void visitStartTransaction(StartTransaction node, Integer indent)
        {
            builder.append("START TRANSACTION");

            Iterator<TransactionMode> iterator = node.getTransactionModes().iterator();
            while (iterator.hasNext()) {
                builder.append(" ");
                process(iterator.next(), indent);
                if (iterator.hasNext()) {
                    builder.append(",");
                }
            }
            return null;
        }

        @Override
        public Void visitIsolationLevel(Isolation node, Integer indent)
        {
            builder.append("ISOLATION LEVEL ").append(node.getLevel().getText());
            return null;
        }

        @Override
        public Void visitTransactionAccessMode(TransactionAccessMode node, Integer context)
        {
            builder.append(node.isReadOnly() ? "READ ONLY" : "READ WRITE");
            return null;
        }

        @Override
        public Void visitCommit(Commit node, Integer context)
        {
            builder.append("COMMIT");
            return null;
        }

        @Override
        public Void visitRollback(Rollback node, Integer context)
        {
            builder.append("ROLLBACK");
            return null;
        }

        @Override
        public Void visitCreateRole(CreateRole node, Integer context)
        {
            builder.append("CREATE ROLE ").append(node.getName());
            if (node.getGrantor().isPresent()) {
                builder.append(" WITH ADMIN ").append(formatGrantor(node.getGrantor().get()));
            }
            return null;
        }

        @Override
        public Void visitDropRole(DropRole node, Integer context)
        {
            builder.append("DROP ROLE ").append(node.getName());
            return null;
        }

        @Override
        public Void visitGrantRoles(GrantRoles node, Integer context)
        {
            builder.append("GRANT ");
            builder.append(node.getRoles().stream()
                    .map(Identifier::toString)
                    .collect(joining(", ")));
            builder.append(" TO ");
            builder.append(node.getGrantees().stream()
                    .map(Formatter::formatPrincipal)
                    .collect(joining(", ")));
            if (node.isAdminOption()) {
                builder.append(" WITH ADMIN OPTION");
            }
            if (node.getGrantor().isPresent()) {
                builder.append(" GRANTED BY ").append(formatGrantor(node.getGrantor().get()));
            }
            return null;
        }

        @Override
        public Void visitRevokeRoles(RevokeRoles node, Integer context)
        {
            builder.append("REVOKE ");
            if (node.isAdminOption()) {
                builder.append("ADMIN OPTION FOR ");
            }
            builder.append(node.getRoles().stream()
                    .map(Identifier::toString)
                    .collect(joining(", ")));
            builder.append(" FROM ");
            builder.append(node.getGrantees().stream()
                    .map(Formatter::formatPrincipal)
                    .collect(joining(", ")));
            if (node.getGrantor().isPresent()) {
                builder.append(" GRANTED BY ").append(formatGrantor(node.getGrantor().get()));
            }
            return null;
        }

        @Override
        public Void visitSetRole(SetRole node, Integer context)
        {
            builder.append("SET ROLE ");
            SetRole.Type type = node.getType();
            switch (type) {
                case ALL:
                case NONE:
                    builder.append(type.toString());
                    break;
                case ROLE:
                    builder.append(node.getRole().get());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type: " + type);
            }
            return null;
        }

        @Override
        public Void visitGrant(Grant node, Integer indent)
        {
            builder.append("GRANT ");

            if (node.getPrivileges().isPresent()) {
                builder.append(node.getPrivileges().get().stream()
                        .collect(joining(", ")));
            }
            else {
                builder.append("ALL PRIVILEGES");
            }

            builder.append(" ON ");
            if (node.isTable()) {
                builder.append("TABLE ");
            }
            builder.append(node.getTableName())
                    .append(" TO ")
                    .append(formatPrincipal(node.getGrantee()));
            if (node.isWithGrantOption()) {
                builder.append(" WITH GRANT OPTION");
            }

            return null;
        }

        @Override
        public Void visitRevoke(Revoke node, Integer indent)
        {
            builder.append("REVOKE ");

            if (node.isGrantOptionFor()) {
                builder.append("GRANT OPTION FOR ");
            }

            if (node.getPrivileges().isPresent()) {
                builder.append(node.getPrivileges().get().stream()
                        .collect(joining(", ")));
            }
            else {
                builder.append("ALL PRIVILEGES");
            }

            builder.append(" ON ");
            if (node.isTable()) {
                builder.append("TABLE ");
            }
            builder.append(node.getTableName())
                    .append(" FROM ")
                    .append(formatPrincipal(node.getGrantee()));

            return null;
        }

        @Override
        public Void visitShowGrants(ShowGrants node, Integer indent)
        {
            builder.append("SHOW GRANTS ");

            if (node.getTableName().isPresent()) {
                builder.append("ON ");

                if (node.getTable()) {
                    builder.append("TABLE ");
                }
                builder.append(node.getTableName().get());
            }

            return null;
        }

        @Override
        public Void visitShowRoles(ShowRoles node, Integer context)
        {
            builder.append("SHOW ");
            if (node.isCurrent()) {
                builder.append("CURRENT ");
            }
            builder.append("ROLES");

            if (node.getCatalog().isPresent()) {
                builder.append(" FROM ")
                        .append(node.getCatalog().get());
            }

            return null;
        }

        @Override
        public Void visitShowRoleGrants(ShowRoleGrants node, Integer context)
        {
            builder.append("SHOW ROLE GRANTS");

            if (node.getCatalog().isPresent()) {
                builder.append(" FROM ")
                        .append(node.getCatalog().get());
            }

            return null;
        }

        @Override
        public Void visitSetPath(SetPath node, Integer indent)
        {
            builder.append("SET PATH ");
            builder.append(Joiner.on(", ").join(node.getPathSpecification().getPath()));
            return null;
        }

        private void processRelation(Relation relation, Integer indent)
        {
            // TODO: handle this properly
            if (relation instanceof Table) {
                builder.append("TABLE ")
                        .append(((Table) relation).getName())
                        .append('\n');
            }
            else {
                process(relation, indent);
            }
        }

        private StringBuilder append(int indent, String value)
        {
            return builder.append(indentString(indent))
                    .append(value);
        }
    }
}
