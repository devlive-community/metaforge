package org.devlive.metaforge.parser.tree;

import org.devlive.metaforge.parser.ast.AstVisitor;
import org.devlive.metaforge.parser.tree.join.Join;
import org.devlive.metaforge.parser.tree.join.JoinOn;
import org.devlive.metaforge.parser.tree.query.Query;
import org.devlive.metaforge.parser.tree.query.QuerySpecification;
import org.devlive.metaforge.parser.tree.table.TableElement;
import org.devlive.metaforge.parser.tree.table.TableSubQuery;

public abstract class DefaultTraversalVisitor<C>
        extends AstVisitor<Void, C>
{
    @Override
    public Void visitExtract(Extract node, C context)
    {
        process(node.getExpression(), context);
        return null;
    }

    @Override
    public Void visitCast(Cast node, C context)
    {
        process(node.getExpression(), context);
        return null;
    }

    @Override
    public Void visitArithmeticBinary(ArithmeticBinaryExpression node, C context)
    {
        process(node.getLeft(), context);
        process(node.getRight(), context);

        return null;
    }

    @Override
    public Void visitBetweenPredicate(BetweenPredicate node, C context)
    {
        process(node.getValue(), context);
        process(node.getMin(), context);
        process(node.getMax(), context);

        return null;
    }

    @Override
    public Void visitCoalesceExpression(CoalesceExpression node, C context)
    {
        for (Expression operand : node.getOperands()) {
            process(operand, context);
        }

        return null;
    }

    @Override
    public Void visitAtTimeZone(AtTimeZone node, C context)
    {
        process(node.getValue(), context);
        process(node.getTimeZone(), context);

        return null;
    }

    @Override
    public Void visitArrayConstructor(ArrayConstructor node, C context)
    {
        for (Expression expression : node.getValues()) {
            process(expression, context);
        }

        return null;
    }

    @Override
    public Void visitSubscriptExpression(SubscriptExpression node, C context)
    {
        process(node.getBase(), context);
        process(node.getIndex(), context);

        return null;
    }

    @Override
    public Void visitComparisonExpression(ComparisonExpression node, C context)
    {
        process(node.getLeft(), context);
        process(node.getRight(), context);

        return null;
    }

    @Override
    public Void visitFormat(Format node, C context)
    {
        for (Expression argument : node.getArguments()) {
            process(argument, context);
        }

        return null;
    }

    @Override
    public Void visitQuery(Query node, C context)
    {
        if (node.getWith().isPresent()) {
            process(node.getWith().get(), context);
        }
        process(node.getQueryBody(), context);
        if (node.getOrderBy().isPresent()) {
            process(node.getOrderBy().get(), context);
        }
        if (node.getLimit().isPresent()) {
            process(node.getLimit().get(), context);
        }

        return null;
    }

    @Override
    public Void visitWith(With node, C context)
    {
        for (WithQuery query : node.getQueries()) {
            process(query, context);
        }

        return null;
    }

    @Override
    public Void visitWithQuery(WithQuery node, C context)
    {
        process(node.getQuery(), context);
        return null;
    }

    @Override
    public Void visitSelect(Select node, C context)
    {
        for (SelectItem item : node.getSelectItems()) {
            process(item, context);
        }

        return null;
    }

    @Override
    public Void visitSingleColumn(SingleColumn node, C context)
    {
        process(node.getExpression(), context);

        return null;
    }

    @Override
    public Void visitAllColumns(AllColumns node, C context)
    {
        node.getTarget().ifPresent(value -> process(value, context));

        return null;
    }

    @Override
    public Void visitWhenClause(WhenClause node, C context)
    {
        process(node.getOperand(), context);
        process(node.getResult(), context);

        return null;
    }

    @Override
    public Void visitInPredicate(InPredicate node, C context)
    {
        process(node.getValue(), context);
        process(node.getValueList(), context);

        return null;
    }

    @Override
    public Void visitFunctionCall(FunctionCall node, C context)
    {
        for (Expression argument : node.getArguments()) {
            process(argument, context);
        }

        if (node.getOrderBy().isPresent()) {
            process(node.getOrderBy().get(), context);
        }

        if (node.getWindow().isPresent()) {
            process(node.getWindow().get(), context);
        }

        if (node.getFilter().isPresent()) {
            process(node.getFilter().get(), context);
        }

        return null;
    }

    @Override
    public Void visitGroupingOperation(GroupingOperation node, C context)
    {
        for (Expression columnArgument : node.getGroupingColumns()) {
            process(columnArgument, context);
        }

        return null;
    }

    @Override
    public Void visitDereferenceExpression(DereferenceExpression node, C context)
    {
        process(node.getBase(), context);
        return null;
    }

    @Override
    public Void visitWindow(Window node, C context)
    {
        for (Expression expression : node.getPartitionBy()) {
            process(expression, context);
        }

        if (node.getOrderBy().isPresent()) {
            process(node.getOrderBy().get(), context);
        }

        if (node.getFrame().isPresent()) {
            process(node.getFrame().get(), context);
        }

        return null;
    }

    @Override
    public Void visitWindowFrame(WindowFrame node, C context)
    {
        process(node.getStart(), context);
        if (node.getEnd().isPresent()) {
            process(node.getEnd().get(), context);
        }

        return null;
    }

    @Override
    public Void visitFrameBound(FrameBound node, C context)
    {
        if (node.getValue().isPresent()) {
            process(node.getValue().get(), context);
        }

        return null;
    }

    @Override
    public Void visitLimit(Limit node, C context)
    {
        process(node.getRowCount());

        return null;
    }

    @Override
    public Void visitSimpleCaseExpression(SimpleCaseExpression node, C context)
    {
        process(node.getOperand(), context);
        for (WhenClause clause : node.getWhenClauses()) {
            process(clause, context);
        }

        node.getDefaultValue()
                .ifPresent(value -> process(value, context));

        return null;
    }

    @Override
    public Void visitInListExpression(InListExpression node, C context)
    {
        for (Expression value : node.getValues()) {
            process(value, context);
        }

        return null;
    }

    @Override
    public Void visitNullIfExpression(NullIfExpression node, C context)
    {
        process(node.getFirst(), context);
        process(node.getSecond(), context);

        return null;
    }

    @Override
    public Void visitIfExpression(IfExpression node, C context)
    {
        process(node.getCondition(), context);
        process(node.getTrueValue(), context);
        if (node.getFalseValue().isPresent()) {
            process(node.getFalseValue().get(), context);
        }

        return null;
    }

    @Override
    public Void visitTryExpression(TryExpression node, C context)
    {
        process(node.getInnerExpression(), context);
        return null;
    }

    @Override
    public Void visitBindExpression(BindExpression node, C context)
    {
        for (Expression value : node.getValues()) {
            process(value, context);
        }
        process(node.getFunction(), context);

        return null;
    }

    @Override
    public Void visitArithmeticUnary(ArithmeticUnaryExpression node, C context)
    {
        process(node.getValue(), context);
        return null;
    }

    @Override
    public Void visitNotExpression(NotExpression node, C context)
    {
        process(node.getValue(), context);
        return null;
    }

    @Override
    public Void visitSearchedCaseExpression(SearchedCaseExpression node, C context)
    {
        for (WhenClause clause : node.getWhenClauses()) {
            process(clause, context);
        }
        node.getDefaultValue()
                .ifPresent(value -> process(value, context));

        return null;
    }

    @Override
    public Void visitLikePredicate(LikePredicate node, C context)
    {
        process(node.getValue(), context);
        process(node.getPattern(), context);
        node.getEscape().ifPresent(value -> process(value, context));

        return null;
    }

    @Override
    public Void visitIsNotNullPredicate(IsNotNullPredicate node, C context)
    {
        process(node.getValue(), context);
        return null;
    }

    @Override
    public Void visitIsNullPredicate(IsNullPredicate node, C context)
    {
        process(node.getValue(), context);
        return null;
    }

    @Override
    public Void visitLogicalBinaryExpression(LogicalBinaryExpression node, C context)
    {
        process(node.getLeft(), context);
        process(node.getRight(), context);

        return null;
    }

    @Override
    public Void visitSubqueryExpression(SubqueryExpression node, C context)
    {
        process(node.getQuery(), context);
        return null;
    }

    @Override
    public Void visitOrderBy(OrderBy node, C context)
    {
        for (SortItem sortItem : node.getSortItems()) {
            process(sortItem, context);
        }
        return null;
    }

    @Override
    public Void visitSortItem(SortItem node, C context)
    {
        process(node.getSortKey(), context);
        return null;
    }

    @Override
    public Void visitQuerySpecification(QuerySpecification node, C context)
    {
        process(node.getSelect(), context);
        if (node.getFrom().isPresent()) {
            process(node.getFrom().get(), context);
        }
        if (node.getWhere().isPresent()) {
            process(node.getWhere().get(), context);
        }
        if (node.getGroupBy().isPresent()) {
            process(node.getGroupBy().get(), context);
        }
        if (node.getHaving().isPresent()) {
            process(node.getHaving().get(), context);
        }
        if (node.getOrderBy().isPresent()) {
            process(node.getOrderBy().get(), context);
        }
        if (node.getLimit().isPresent()) {
            process(node.getLimit().get(), context);
        }
        return null;
    }

    @Override
    public Void visitSetOperation(SetOperation node, C context)
    {
        for (Relation relation : node.getRelations()) {
            process(relation, context);
        }
        return null;
    }

    @Override
    public Void visitValues(Values node, C context)
    {
        for (Expression row : node.getRows()) {
            process(row, context);
        }
        return null;
    }

    @Override
    public Void visitRow(Row node, C context)
    {
        for (Expression expression : node.getItems()) {
            process(expression, context);
        }
        return null;
    }

    @Override
    public Void visitTableSubquery(TableSubQuery node, C context)
    {
        process(node.getQuery(), context);
        return null;
    }

    @Override
    public Void visitAliasedRelation(AliasedRelation node, C context)
    {
        process(node.getRelation(), context);
        return null;
    }

    @Override
    public Void visitSampledRelation(SampledRelation node, C context)
    {
        process(node.getRelation(), context);
        process(node.getSamplePercentage(), context);
        return null;
    }

    @Override
    public Void visitJoin(Join node, C context)
    {
        process(node.getLeft(), context);
        process(node.getRight(), context);

        node.getCriteria()
                .filter(criteria -> criteria instanceof JoinOn)
                .map(criteria -> process(((JoinOn) criteria).getExpression(), context));

        return null;
    }

    @Override
    public Void visitUnnest(Unnest node, C context)
    {
        for (Expression expression : node.getExpressions()) {
            process(expression, context);
        }

        return null;
    }

    @Override
    public Void visitGroupBy(GroupBy node, C context)
    {
        for (GroupingElement groupingElement : node.getGroupingElements()) {
            process(groupingElement, context);
        }

        return null;
    }

    @Override
    public Void visitCube(Cube node, C context)
    {
        return null;
    }

    @Override
    public Void visitRollup(Rollup node, C context)
    {
        return null;
    }

    @Override
    public Void visitSimpleGroupBy(SimpleGroupBy node, C context)
    {
        for (Expression expression : node.getExpressions()) {
            process(expression, context);
        }

        return null;
    }

    @Override
    public Void visitGroupingSets(GroupingSets node, C context)
    {
        return null;
    }

    @Override
    public Void visitInsert(Insert node, C context)
    {
        process(node.getQuery(), context);

        return null;
    }

    @Override
    public Void visitDelete(Delete node, C context)
    {
        process(node.getTable(), context);
        node.getWhere().ifPresent(where -> process(where, context));

        return null;
    }

    @Override
    public Void visitCreateTableAsSelect(CreateTableAsSelect node, C context)
    {
        process(node.getQuery(), context);
        for (Property property : node.getProperties()) {
            process(property, context);
        }

        return null;
    }

    @Override
    public Void visitProperty(Property node, C context)
    {
        process(node.getName(), context);
        process(node.getValue(), context);

        return null;
    }

    @Override
    public Void visitAnalyze(Analyze node, C context)
    {
        for (Property property : node.getProperties()) {
            process(property, context);
        }
        return null;
    }

    @Override
    public Void visitCreateView(CreateView node, C context)
    {
        process(node.getQuery(), context);

        return null;
    }

    @Override
    public Void visitSetSession(SetSession node, C context)
    {
        process(node.getValue(), context);

        return null;
    }

    @Override
    public Void visitAddColumn(AddColumn node, C context)
    {
        process(node.getColumn(), context);

        return null;
    }

    @Override
    public Void visitCreateTable(CreateTable node, C context)
    {
        for (TableElement tableElement : node.getElements()) {
            process(tableElement, context);
        }
        for (Property property : node.getProperties()) {
            process(property, context);
        }

        return null;
    }

    @Override
    public Void visitStartTransaction(StartTransaction node, C context)
    {
        for (TransactionMode transactionMode : node.getTransactionModes()) {
            process(transactionMode, context);
        }

        return null;
    }

    @Override
    public Void visitExplain(Explain node, C context)
    {
        process(node.getStatement(), context);

        for (ExplainOption option : node.getOptions()) {
            process(option, context);
        }

        return null;
    }

    @Override
    public Void visitQuantifiedComparisonExpression(QuantifiedComparisonExpression node, C context)
    {
        process(node.getValue(), context);
        process(node.getSubquery(), context);

        return null;
    }

    @Override
    public Void visitExists(ExistsPredicate node, C context)
    {
        process(node.getSubquery(), context);

        return null;
    }

    @Override
    public Void visitLateral(Lateral node, C context)
    {
        process(node.getQuery(), context);

        return null;
    }
}
