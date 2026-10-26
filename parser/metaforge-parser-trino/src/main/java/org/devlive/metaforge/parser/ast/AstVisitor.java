package org.devlive.metaforge.parser.ast;

import org.devlive.metaforge.parser.node.Node;
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
import org.devlive.metaforge.parser.tree.ExplainOption;
import org.devlive.metaforge.parser.tree.Expression;
import org.devlive.metaforge.parser.tree.Extract;
import org.devlive.metaforge.parser.tree.FetchFirst;
import org.devlive.metaforge.parser.tree.FieldReference;
import org.devlive.metaforge.parser.tree.Format;
import org.devlive.metaforge.parser.tree.FrameBound;
import org.devlive.metaforge.parser.tree.FunctionCall;
import org.devlive.metaforge.parser.tree.GenericDataType;
import org.devlive.metaforge.parser.tree.GenericLiteral;
import org.devlive.metaforge.parser.tree.Grant;
import org.devlive.metaforge.parser.tree.GrantRoles;
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
import org.devlive.metaforge.parser.tree.Literal;
import org.devlive.metaforge.parser.tree.LogicalBinaryExpression;
import org.devlive.metaforge.parser.tree.LongLiteral;
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
import org.devlive.metaforge.parser.tree.Property;
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
import org.devlive.metaforge.parser.tree.SetOperation;
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
import org.devlive.metaforge.parser.tree.SymbolReference;
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
import org.devlive.metaforge.parser.tree.query.Query;
import org.devlive.metaforge.parser.tree.query.QueryBody;
import org.devlive.metaforge.parser.tree.query.QuerySpecification;
import org.devlive.metaforge.parser.tree.table.Table;
import org.devlive.metaforge.parser.tree.table.TableElement;
import org.devlive.metaforge.parser.tree.table.TableSubQuery;

import javax.annotation.Nullable;

public abstract class AstVisitor<R, C>
{
    public R process(Node node)
    {
        return process(node, null);
    }

    public R process(Node node, @Nullable C context)
    {
        return node.accept(this, context);
    }

    public R visitNode(Node node, C context)
    {
        return null;
    }

    public R visitExpression(Expression node, C context)
    {
        return visitNode(node, context);
    }

    public R visitCurrentTime(CurrentTime node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitExtract(Extract node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitArithmeticBinary(ArithmeticBinaryExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitBetweenPredicate(BetweenPredicate node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitCoalesceExpression(CoalesceExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitComparisonExpression(ComparisonExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitLiteral(Literal node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitDoubleLiteral(DoubleLiteral node, C context)
    {
        return visitLiteral(node, context);
    }

    public R visitDecimalLiteral(DecimalLiteral node, C context)
    {
        return visitLiteral(node, context);
    }

    public R visitStatement(Statement node, C context)
    {
        return visitNode(node, context);
    }

    public R visitPrepare(Prepare node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitDeallocate(Deallocate node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitExecute(Execute node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitDescribeOutput(DescribeOutput node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitDescribeInput(DescribeInput node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitQuery(Query node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitExplain(Explain node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitShowTables(ShowTables node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitShowSchemas(ShowSchemas node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitShowCatalogs(ShowCatalogs node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitShowColumns(ShowColumns node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitShowStats(ShowStats node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitShowCreate(ShowCreate node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitShowFunctions(ShowFunctions node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitUse(Use node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitShowSession(ShowSession node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitSetSession(SetSession node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitResetSession(ResetSession node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitGenericLiteral(GenericLiteral node, C context)
    {
        return visitLiteral(node, context);
    }

    public R visitTimeLiteral(TimeLiteral node, C context)
    {
        return visitLiteral(node, context);
    }

    public R visitExplainOption(ExplainOption node, C context)
    {
        return visitNode(node, context);
    }

    public R visitWith(With node, C context)
    {
        return visitNode(node, context);
    }

    public R visitWithQuery(WithQuery node, C context)
    {
        return visitNode(node, context);
    }

    public R visitSelect(Select node, C context)
    {
        return visitNode(node, context);
    }

    public R visitRelation(Relation node, C context)
    {
        return visitNode(node, context);
    }

    public R visitQueryBody(QueryBody node, C context)
    {
        return visitRelation(node, context);
    }

    public R visitOrderBy(OrderBy node, C context)
    {
        return visitNode(node, context);
    }

    public R visitOffset(Offset node, C context)
    {
        return visitNode(node, context);
    }

    public R visitFetchFirst(FetchFirst node, C context)
    {
        return visitNode(node, context);
    }

    public R visitLimit(Limit node, C context)
    {
        return visitNode(node, context);
    }

    public R visitAllRows(AllRows node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitQuerySpecification(QuerySpecification node, C context)
    {
        return visitQueryBody(node, context);
    }

    public R visitSetOperation(SetOperation node, C context)
    {
        return visitQueryBody(node, context);
    }

    public R visitUnion(Union node, C context)
    {
        return visitSetOperation(node, context);
    }

    public R visitIntersect(Intersect node, C context)
    {
        return visitSetOperation(node, context);
    }

    public R visitExcept(Except node, C context)
    {
        return visitSetOperation(node, context);
    }

    public R visitTimestampLiteral(TimestampLiteral node, C context)
    {
        return visitLiteral(node, context);
    }

    public R visitWhenClause(WhenClause node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitIntervalLiteral(IntervalLiteral node, C context)
    {
        return visitLiteral(node, context);
    }

    public R visitInPredicate(InPredicate node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitFunctionCall(FunctionCall node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitLambdaExpression(LambdaExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitSimpleCaseExpression(SimpleCaseExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitStringLiteral(StringLiteral node, C context)
    {
        return visitLiteral(node, context);
    }

    public R visitCharLiteral(CharLiteral node, C context)
    {
        return visitLiteral(node, context);
    }

    public R visitBinaryLiteral(BinaryLiteral node, C context)
    {
        return visitLiteral(node, context);
    }

    public R visitBooleanLiteral(BooleanLiteral node, C context)
    {
        return visitLiteral(node, context);
    }

    public R visitInListExpression(InListExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitIdentifier(Identifier node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitDereferenceExpression(DereferenceExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitNullIfExpression(NullIfExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitIfExpression(IfExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitNullLiteral(NullLiteral node, C context)
    {
        return visitLiteral(node, context);
    }

    public R visitArithmeticUnary(ArithmeticUnaryExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitNotExpression(NotExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitSelectItem(SelectItem node, C context)
    {
        return visitNode(node, context);
    }

    public R visitSingleColumn(SingleColumn node, C context)
    {
        return visitSelectItem(node, context);
    }

    public R visitAllColumns(AllColumns node, C context)
    {
        return visitSelectItem(node, context);
    }

    public R visitSearchedCaseExpression(SearchedCaseExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitLikePredicate(LikePredicate node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitIsNotNullPredicate(IsNotNullPredicate node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitIsNullPredicate(IsNullPredicate node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitArrayConstructor(ArrayConstructor node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitSubscriptExpression(SubscriptExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitLongLiteral(LongLiteral node, C context)
    {
        return visitLiteral(node, context);
    }

    public R visitParameter(Parameter node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitLogicalBinaryExpression(LogicalBinaryExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitSubqueryExpression(SubqueryExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitSortItem(SortItem node, C context)
    {
        return visitNode(node, context);
    }

    public R visitTable(Table node, C context)
    {
        return visitQueryBody(node, context);
    }

    public R visitUnnest(Unnest node, C context)
    {
        return visitRelation(node, context);
    }

    public R visitLateral(Lateral node, C context)
    {
        return visitRelation(node, context);
    }

    public R visitValues(Values node, C context)
    {
        return visitQueryBody(node, context);
    }

    public R visitRow(Row node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitTableSubquery(TableSubQuery node, C context)
    {
        return visitQueryBody(node, context);
    }

    public R visitAliasedRelation(AliasedRelation node, C context)
    {
        return visitRelation(node, context);
    }

    public R visitSampledRelation(SampledRelation node, C context)
    {
        return visitRelation(node, context);
    }

    public R visitJoin(Join node, C context)
    {
        return visitRelation(node, context);
    }

    public R visitExists(ExistsPredicate node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitTryExpression(TryExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitCast(Cast node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitFieldReference(FieldReference node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitWindow(Window node, C context)
    {
        return visitNode(node, context);
    }

    public R visitWindowFrame(WindowFrame node, C context)
    {
        return visitNode(node, context);
    }

    public R visitFrameBound(FrameBound node, C context)
    {
        return visitNode(node, context);
    }

    public R visitCallArgument(CallArgument node, C context)
    {
        return visitNode(node, context);
    }

    public R visitTableElement(TableElement node, C context)
    {
        return visitNode(node, context);
    }

    public R visitColumnDefinition(ColumnDefinition node, C context)
    {
        return visitTableElement(node, context);
    }

    public R visitLikeClause(LikeClause node, C context)
    {
        return visitTableElement(node, context);
    }

    public R visitCreateSchema(CreateSchema node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitDropSchema(DropSchema node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitRenameSchema(RenameSchema node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitSetSchemaAuthorization(SetSchemaAuthorization node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitCreateTable(CreateTable node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitCreateTableAsSelect(CreateTableAsSelect node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitProperty(Property node, C context)
    {
        return visitNode(node, context);
    }

    public R visitDropTable(DropTable node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitRenameTable(RenameTable node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitRenameView(RenameView node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitComment(Comment node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitRenameColumn(RenameColumn node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitDropColumn(DropColumn node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitAddColumn(AddColumn node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitAnalyze(Analyze node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitCreateView(CreateView node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitDropView(DropView node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitInsert(Insert node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitCall(Call node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitDelete(Delete node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitStartTransaction(StartTransaction node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitCreateRole(CreateRole node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitDropRole(DropRole node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitGrantRoles(GrantRoles node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitRevokeRoles(RevokeRoles node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitSetRole(SetRole node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitGrant(Grant node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitRevoke(Revoke node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitShowGrants(ShowGrants node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitShowRoles(ShowRoles node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitShowRoleGrants(ShowRoleGrants node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitSetPath(SetPath node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitPathSpecification(PathSpecification node, C context)
    {
        return visitNode(node, context);
    }

    public R visitPathElement(PathElement node, C context)
    {
        return visitNode(node, context);
    }

    public R visitTransactionMode(TransactionMode node, C context)
    {
        return visitNode(node, context);
    }

    public R visitIsolationLevel(Isolation node, C context)
    {
        return visitTransactionMode(node, context);
    }

    public R visitTransactionAccessMode(TransactionAccessMode node, C context)
    {
        return visitTransactionMode(node, context);
    }

    public R visitCommit(Commit node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitRollback(Rollback node, C context)
    {
        return visitStatement(node, context);
    }

    public R visitAtTimeZone(AtTimeZone node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitGroupBy(GroupBy node, C context)
    {
        return visitNode(node, context);
    }

    public R visitGroupingElement(GroupingElement node, C context)
    {
        return visitNode(node, context);
    }

    public R visitCube(Cube node, C context)
    {
        return visitGroupingElement(node, context);
    }

    public R visitGroupingSets(GroupingSets node, C context)
    {
        return visitGroupingElement(node, context);
    }

    public R visitRollup(Rollup node, C context)
    {
        return visitGroupingElement(node, context);
    }

    public R visitSimpleGroupBy(SimpleGroupBy node, C context)
    {
        return visitGroupingElement(node, context);
    }

    public R visitSymbolReference(SymbolReference node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitQuantifiedComparisonExpression(QuantifiedComparisonExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitLambdaArgumentDeclaration(LambdaArgumentDeclaration node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitBindExpression(BindExpression node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitGroupingOperation(GroupingOperation node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitCurrentUser(CurrentUser node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitCurrentPath(CurrentPath node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitFormat(Format node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitDataType(DataType node, C context)
    {
        return visitExpression(node, context);
    }

    public R visitRowDataType(RowDataType node, C context)
    {
        return visitDataType(node, context);
    }

    public R visitGenericDataType(GenericDataType node, C context)
    {
        return visitDataType(node, context);
    }

    public R visitRowField(RowDataType.Field node, C context)
    {
        return visitNode(node, context);
    }

    public R visitDataTypeParameter(DataTypeParameter node, C context)
    {
        return visitNode(node, context);
    }

    public R visitNumericTypeParameter(NumericParameter node, C context)
    {
        return visitDataTypeParameter(node, context);
    }

    public R visitTypeParameter(TypeParameter node, C context)
    {
        return visitDataTypeParameter(node, context);
    }

    public R visitIntervalDataType(IntervalDayTimeDataType node, C context)
    {
        return visitDataType(node, context);
    }

    public R visitDateTimeType(DateTimeDataType node, C context)
    {
        return visitDataType(node, context);
    }
}
