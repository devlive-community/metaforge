package org.devlive.metaforge.parser.mysql.companion

import org.devlive.metaforge.parser.mysql.MySqlParser
import org.devlive.metaforge.parser.mysql.MySqlParser.*
import org.devlive.metaforge.parser.mysql.MySqlParserVisitor
import org.devlive.metaforge.parser.mysql.companion.ast.Node
import org.devlive.metaforge.parser.mysql.companion.ast.NodeLocation
import org.devlive.metaforge.parser.mysql.companion.ast.ParserOptions
import org.devlive.metaforge.parser.mysql.companion.tree.*
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.RuleNode
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.*
import java.util.stream.Collectors.toList

class AstVisitorBuilder(options: ParserOptions.DecimalLiteralTreatment) : MySqlParserVisitor<Node>
{
    private fun <T> visitSelectElements(context: SelectElementsContext, clazz: Class<T>): List<T>
    {
        return context.selectElement()
            .stream()
            .map<Any>(this::visitChildren)
            .map { o: Any? -> clazz.cast(o) }
            .collect(toList())
    }

    private fun getLocation(parserRuleContext: ParserRuleContext): Optional<NodeLocation>
    {
        Objects.requireNonNull(parserRuleContext, "parserRuleContext is null")
        return Optional.ofNullable(getLocation(parserRuleContext.getStart()))
    }

    private fun getLocation(token: Token): NodeLocation
    {
        Objects.requireNonNull(token, "token is null")
        return NodeLocation(token.line, token.charPositionInLine)
    }

    override fun visit(tree: ParseTree): Node
    {
        return when (tree)
        {
            is MySqlParser.RootContext -> visitRoot(tree)
            is MySqlParser.SqlStatementsContext -> visitSqlStatements(tree)
            is MySqlParser.SqlStatementContext -> visitSqlStatement(tree)
            else -> throw UnsupportedOperationException("Unsupported ParseTree type: ${tree?.javaClass?.simpleName}")
        }
    }

    override fun visitChildren(p0: RuleNode?): Node
    {
        return when (p0)
        {
            is SelectExpressionElementContext -> visitSelectExpressionElement(p0)
            is SelectFunctionElementContext -> visitSelectFunctionElement(p0)
            is SelectColumnElementContext -> visitSelectColumnElement(p0)
            else -> throw UnsupportedOperationException("Unsupported RuleNode type: ${p0?.javaClass?.simpleName}")
        }
    }

    override fun visitTerminal(ctx: TerminalNode?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitErrorNode(ctx: ErrorNode?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRoot(ctx: MySqlParser.RootContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSqlStatements(ctx: MySqlParser.SqlStatementsContext): Node
    {
        // TODO: In the later stage, multiple SQL statements can be parsed
        return visit(ctx.sqlStatement()[0])
    }

    override fun visitSqlStatement(ctx: MySqlParser.SqlStatementContext): Node
    {
        if (ctx.dmlStatement() != null)
        {
            return visitDmlStatement(ctx.dmlStatement())
        }
        else if (ctx.ddlStatement() != null)
        {
            return visitDdlStatement(ctx.ddlStatement())
        }
        else if (ctx.transactionStatement() != null)
        {
            return visitTransactionStatement(ctx.transactionStatement())
        }
        else if (ctx.replicationStatement() != null)
        {
            return visitReplicationStatement(ctx.replicationStatement())
        }
        else if (ctx.utilityStatement() != null)
        {
            return visitUtilityStatement(ctx.utilityStatement())
        }
        else if (ctx.preparedStatement() != null)
        {
            return visitPreparedStatement(ctx.preparedStatement())
        }
        else if (ctx.administrationStatement() != null)
        {
            return visitAdministrationStatement(ctx.administrationStatement())
        }
        else
        {
            throw UnsupportedOperationException("Unsupported SqlStatement type: ${ctx?.javaClass?.simpleName}")
        }
    }

    override fun visitEmptyStatement_(ctx: MySqlParser.EmptyStatement_Context?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDdlStatement(ctx: MySqlParser.DdlStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported DdlStatement type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDmlStatement(ctx: MySqlParser.DmlStatementContext): Node
    {
        ctx.selectStatement()?.let {
            return visitSimpleSelect(ctx.selectStatement() as SimpleSelectContext)
        }
        throw UnsupportedOperationException("Unsupported DmlStatement type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTransactionStatement(ctx: MySqlParser.TransactionStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported TransactionStatement type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitReplicationStatement(ctx: MySqlParser.ReplicationStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported ReplicationStatement type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPreparedStatement(ctx: MySqlParser.PreparedStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported PreparedStatement type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCompoundStatement(ctx: MySqlParser.CompoundStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported CompoundStatement type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAdministrationStatement(ctx: MySqlParser.AdministrationStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported AdministrationStatement type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUtilityStatement(ctx: MySqlParser.UtilityStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported UtilityStatement type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateDatabase(ctx: MySqlParser.CreateDatabaseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported CreateDatabase type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateEvent(ctx: MySqlParser.CreateEventContext?): Node
    {
        throw UnsupportedOperationException("Unsupported CreateEvent type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateIndex(ctx: MySqlParser.CreateIndexContext?): Node
    {
        throw UnsupportedOperationException("Unsupported CreateIndex type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateLogfileGroup(ctx: MySqlParser.CreateLogfileGroupContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateProcedure(ctx: MySqlParser.CreateProcedureContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateFunction(ctx: MySqlParser.CreateFunctionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateRole(ctx: MySqlParser.CreateRoleContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateServer(ctx: MySqlParser.CreateServerContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCopyCreateTable(ctx: MySqlParser.CopyCreateTableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitQueryCreateTable(ctx: MySqlParser.QueryCreateTableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitColumnCreateTable(ctx: MySqlParser.ColumnCreateTableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateTablespaceInnodb(ctx: MySqlParser.CreateTablespaceInnodbContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateTablespaceNdb(ctx: MySqlParser.CreateTablespaceNdbContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateTrigger(ctx: MySqlParser.CreateTriggerContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitWithClause(ctx: MySqlParser.WithClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCommonTableExpressions(ctx: MySqlParser.CommonTableExpressionsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCteName(ctx: MySqlParser.CteNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCteColumnName(ctx: MySqlParser.CteColumnNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateView(ctx: MySqlParser.CreateViewContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateDatabaseOption(ctx: MySqlParser.CreateDatabaseOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCharSet(ctx: MySqlParser.CharSetContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCurrentUserExpression(ctx: MySqlParser.CurrentUserExpressionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitOwnerStatement(ctx: MySqlParser.OwnerStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPreciseSchedule(ctx: MySqlParser.PreciseScheduleContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIntervalSchedule(ctx: MySqlParser.IntervalScheduleContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTimestampValue(ctx: MySqlParser.TimestampValueContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIntervalExpr(ctx: MySqlParser.IntervalExprContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIntervalType(ctx: MySqlParser.IntervalTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitEnableType(ctx: MySqlParser.EnableTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIndexType(ctx: MySqlParser.IndexTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIndexOption(ctx: MySqlParser.IndexOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitProcedureParameter(ctx: MySqlParser.ProcedureParameterContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFunctionParameter(ctx: MySqlParser.FunctionParameterContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRoutineComment(ctx: MySqlParser.RoutineCommentContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRoutineLanguage(ctx: MySqlParser.RoutineLanguageContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRoutineBehavior(ctx: MySqlParser.RoutineBehaviorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRoutineData(ctx: MySqlParser.RoutineDataContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRoutineSecurity(ctx: MySqlParser.RoutineSecurityContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitServerOption(ctx: MySqlParser.ServerOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateDefinitions(ctx: MySqlParser.CreateDefinitionsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitColumnDeclaration(ctx: MySqlParser.ColumnDeclarationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitConstraintDeclaration(ctx: MySqlParser.ConstraintDeclarationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIndexDeclaration(ctx: MySqlParser.IndexDeclarationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitColumnDefinition(ctx: MySqlParser.ColumnDefinitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitNullColumnConstraint(ctx: MySqlParser.NullColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDefaultColumnConstraint(ctx: MySqlParser.DefaultColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitVisibilityColumnConstraint(ctx: MySqlParser.VisibilityColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitInvisibilityColumnConstraint(ctx: MySqlParser.InvisibilityColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAutoIncrementColumnConstraint(ctx: MySqlParser.AutoIncrementColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPrimaryKeyColumnConstraint(ctx: MySqlParser.PrimaryKeyColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUniqueKeyColumnConstraint(ctx: MySqlParser.UniqueKeyColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCommentColumnConstraint(ctx: MySqlParser.CommentColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFormatColumnConstraint(ctx: MySqlParser.FormatColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitStorageColumnConstraint(ctx: MySqlParser.StorageColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitReferenceColumnConstraint(ctx: MySqlParser.ReferenceColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCollateColumnConstraint(ctx: MySqlParser.CollateColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitGeneratedColumnConstraint(ctx: MySqlParser.GeneratedColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSerialDefaultColumnConstraint(ctx: MySqlParser.SerialDefaultColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCheckColumnConstraint(ctx: MySqlParser.CheckColumnConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPrimaryKeyTableConstraint(ctx: MySqlParser.PrimaryKeyTableConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUniqueKeyTableConstraint(ctx: MySqlParser.UniqueKeyTableConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitForeignKeyTableConstraint(ctx: MySqlParser.ForeignKeyTableConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCheckTableConstraint(ctx: MySqlParser.CheckTableConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitReferenceDefinition(ctx: MySqlParser.ReferenceDefinitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitReferenceAction(ctx: MySqlParser.ReferenceActionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitReferenceControlType(ctx: MySqlParser.ReferenceControlTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSimpleIndexDeclaration(ctx: MySqlParser.SimpleIndexDeclarationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSpecialIndexDeclaration(ctx: MySqlParser.SpecialIndexDeclarationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionEngine(ctx: MySqlParser.TableOptionEngineContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionEngineAttribute(ctx: MySqlParser.TableOptionEngineAttributeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionAutoextendSize(ctx: MySqlParser.TableOptionAutoextendSizeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionAutoIncrement(ctx: MySqlParser.TableOptionAutoIncrementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionAverage(ctx: MySqlParser.TableOptionAverageContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionCharset(ctx: MySqlParser.TableOptionCharsetContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionChecksum(ctx: MySqlParser.TableOptionChecksumContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionCollate(ctx: MySqlParser.TableOptionCollateContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionComment(ctx: MySqlParser.TableOptionCommentContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionCompression(ctx: MySqlParser.TableOptionCompressionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionConnection(ctx: MySqlParser.TableOptionConnectionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionDataDirectory(ctx: MySqlParser.TableOptionDataDirectoryContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionDelay(ctx: MySqlParser.TableOptionDelayContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionEncryption(ctx: MySqlParser.TableOptionEncryptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionPageCompressed(ctx: MySqlParser.TableOptionPageCompressedContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionPageCompressionLevel(ctx: MySqlParser.TableOptionPageCompressionLevelContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionEncryptionKeyId(ctx: MySqlParser.TableOptionEncryptionKeyIdContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionIndexDirectory(ctx: MySqlParser.TableOptionIndexDirectoryContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionInsertMethod(ctx: MySqlParser.TableOptionInsertMethodContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionKeyBlockSize(ctx: MySqlParser.TableOptionKeyBlockSizeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionMaxRows(ctx: MySqlParser.TableOptionMaxRowsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionMinRows(ctx: MySqlParser.TableOptionMinRowsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionPackKeys(ctx: MySqlParser.TableOptionPackKeysContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionPassword(ctx: MySqlParser.TableOptionPasswordContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionRowFormat(ctx: MySqlParser.TableOptionRowFormatContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionStartTransaction(ctx: MySqlParser.TableOptionStartTransactionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionSecondaryEngineAttribute(ctx: MySqlParser.TableOptionSecondaryEngineAttributeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionRecalculation(ctx: MySqlParser.TableOptionRecalculationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionPersistent(ctx: MySqlParser.TableOptionPersistentContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionSamplePage(ctx: MySqlParser.TableOptionSamplePageContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionTablespace(ctx: MySqlParser.TableOptionTablespaceContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionTableType(ctx: MySqlParser.TableOptionTableTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionTransactional(ctx: MySqlParser.TableOptionTransactionalContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableOptionUnion(ctx: MySqlParser.TableOptionUnionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableType(ctx: MySqlParser.TableTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTablespaceStorage(ctx: MySqlParser.TablespaceStorageContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionDefinitions(ctx: MySqlParser.PartitionDefinitionsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionFunctionHash(ctx: MySqlParser.PartitionFunctionHashContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionFunctionKey(ctx: MySqlParser.PartitionFunctionKeyContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionFunctionRange(ctx: MySqlParser.PartitionFunctionRangeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionFunctionList(ctx: MySqlParser.PartitionFunctionListContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSubPartitionFunctionHash(ctx: MySqlParser.SubPartitionFunctionHashContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSubPartitionFunctionKey(ctx: MySqlParser.SubPartitionFunctionKeyContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionComparison(ctx: MySqlParser.PartitionComparisonContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionListAtom(ctx: MySqlParser.PartitionListAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionListVector(ctx: MySqlParser.PartitionListVectorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionSimple(ctx: MySqlParser.PartitionSimpleContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionDefinerAtom(ctx: MySqlParser.PartitionDefinerAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionDefinerVector(ctx: MySqlParser.PartitionDefinerVectorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSubpartitionDefinition(ctx: MySqlParser.SubpartitionDefinitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionOptionEngine(ctx: MySqlParser.PartitionOptionEngineContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionOptionComment(ctx: MySqlParser.PartitionOptionCommentContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionOptionDataDirectory(ctx: MySqlParser.PartitionOptionDataDirectoryContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionOptionIndexDirectory(ctx: MySqlParser.PartitionOptionIndexDirectoryContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionOptionMaxRows(ctx: MySqlParser.PartitionOptionMaxRowsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionOptionMinRows(ctx: MySqlParser.PartitionOptionMinRowsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionOptionTablespace(ctx: MySqlParser.PartitionOptionTablespaceContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionOptionNodeGroup(ctx: MySqlParser.PartitionOptionNodeGroupContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterSimpleDatabase(ctx: MySqlParser.AlterSimpleDatabaseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterUpgradeName(ctx: MySqlParser.AlterUpgradeNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterEvent(ctx: MySqlParser.AlterEventContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterFunction(ctx: MySqlParser.AlterFunctionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterInstance(ctx: MySqlParser.AlterInstanceContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterLogfileGroup(ctx: MySqlParser.AlterLogfileGroupContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterProcedure(ctx: MySqlParser.AlterProcedureContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterServer(ctx: MySqlParser.AlterServerContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterTable(ctx: MySqlParser.AlterTableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterTablespace(ctx: MySqlParser.AlterTablespaceContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterView(ctx: MySqlParser.AlterViewContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByTableOption(ctx: MySqlParser.AlterByTableOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByAddColumn(ctx: MySqlParser.AlterByAddColumnContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByAddColumns(ctx: MySqlParser.AlterByAddColumnsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByAddIndex(ctx: MySqlParser.AlterByAddIndexContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByAddPrimaryKey(ctx: MySqlParser.AlterByAddPrimaryKeyContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByAddUniqueKey(ctx: MySqlParser.AlterByAddUniqueKeyContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByAddSpecialIndex(ctx: MySqlParser.AlterByAddSpecialIndexContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByAddForeignKey(ctx: MySqlParser.AlterByAddForeignKeyContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByAddCheckTableConstraint(ctx: MySqlParser.AlterByAddCheckTableConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByAlterCheckTableConstraint(ctx: MySqlParser.AlterByAlterCheckTableConstraintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterBySetAlgorithm(ctx: MySqlParser.AlterBySetAlgorithmContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByChangeDefault(ctx: MySqlParser.AlterByChangeDefaultContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByChangeColumn(ctx: MySqlParser.AlterByChangeColumnContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByRenameColumn(ctx: MySqlParser.AlterByRenameColumnContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByLock(ctx: MySqlParser.AlterByLockContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByModifyColumn(ctx: MySqlParser.AlterByModifyColumnContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByDropColumn(ctx: MySqlParser.AlterByDropColumnContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByDropConstraintCheck(ctx: MySqlParser.AlterByDropConstraintCheckContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByDropPrimaryKey(ctx: MySqlParser.AlterByDropPrimaryKeyContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByDropIndex(ctx: MySqlParser.AlterByDropIndexContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByRenameIndex(ctx: MySqlParser.AlterByRenameIndexContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByAlterColumnDefault(ctx: MySqlParser.AlterByAlterColumnDefaultContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByAlterIndexVisibility(ctx: MySqlParser.AlterByAlterIndexVisibilityContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByDropForeignKey(ctx: MySqlParser.AlterByDropForeignKeyContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByDisableKeys(ctx: MySqlParser.AlterByDisableKeysContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByEnableKeys(ctx: MySqlParser.AlterByEnableKeysContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByRename(ctx: MySqlParser.AlterByRenameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByOrder(ctx: MySqlParser.AlterByOrderContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByConvertCharset(ctx: MySqlParser.AlterByConvertCharsetContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByDefaultCharset(ctx: MySqlParser.AlterByDefaultCharsetContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByDiscardTablespace(ctx: MySqlParser.AlterByDiscardTablespaceContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByImportTablespace(ctx: MySqlParser.AlterByImportTablespaceContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByForce(ctx: MySqlParser.AlterByForceContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByValidate(ctx: MySqlParser.AlterByValidateContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByAddDefinitions(ctx: MySqlParser.AlterByAddDefinitionsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterPartition(ctx: MySqlParser.AlterPartitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByAddPartition(ctx: MySqlParser.AlterByAddPartitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByDropPartition(ctx: MySqlParser.AlterByDropPartitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByDiscardPartition(ctx: MySqlParser.AlterByDiscardPartitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByImportPartition(ctx: MySqlParser.AlterByImportPartitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByTruncatePartition(ctx: MySqlParser.AlterByTruncatePartitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByCoalescePartition(ctx: MySqlParser.AlterByCoalescePartitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByReorganizePartition(ctx: MySqlParser.AlterByReorganizePartitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByExchangePartition(ctx: MySqlParser.AlterByExchangePartitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByAnalyzePartition(ctx: MySqlParser.AlterByAnalyzePartitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByCheckPartition(ctx: MySqlParser.AlterByCheckPartitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByOptimizePartition(ctx: MySqlParser.AlterByOptimizePartitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByRebuildPartition(ctx: MySqlParser.AlterByRebuildPartitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByRepairPartition(ctx: MySqlParser.AlterByRepairPartitionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByRemovePartitioning(ctx: MySqlParser.AlterByRemovePartitioningContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterByUpgradePartitioning(ctx: MySqlParser.AlterByUpgradePartitioningContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDropDatabase(ctx: MySqlParser.DropDatabaseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDropEvent(ctx: MySqlParser.DropEventContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDropIndex(ctx: MySqlParser.DropIndexContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDropLogfileGroup(ctx: MySqlParser.DropLogfileGroupContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDropProcedure(ctx: MySqlParser.DropProcedureContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDropFunction(ctx: MySqlParser.DropFunctionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDropServer(ctx: MySqlParser.DropServerContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDropTable(ctx: MySqlParser.DropTableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDropTablespace(ctx: MySqlParser.DropTablespaceContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDropTrigger(ctx: MySqlParser.DropTriggerContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDropView(ctx: MySqlParser.DropViewContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDropRole(ctx: MySqlParser.DropRoleContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSetRole(ctx: MySqlParser.SetRoleContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRenameTable(ctx: MySqlParser.RenameTableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRenameTableClause(ctx: MySqlParser.RenameTableClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTruncateTable(ctx: MySqlParser.TruncateTableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCallStatement(ctx: MySqlParser.CallStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDeleteStatement(ctx: MySqlParser.DeleteStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDoStatement(ctx: MySqlParser.DoStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHandlerStatement(ctx: MySqlParser.HandlerStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitInsertStatement(ctx: MySqlParser.InsertStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLoadDataStatement(ctx: MySqlParser.LoadDataStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLoadXmlStatement(ctx: MySqlParser.LoadXmlStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitReplaceStatement(ctx: MySqlParser.ReplaceStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSimpleSelect(ctx: SimpleSelectContext): Node
    {
        ctx.querySpecification()?.let {
            return visitQuerySpecification(it)
        }
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitParenthesisSelect(ctx: MySqlParser.ParenthesisSelectContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUnionSelect(ctx: MySqlParser.UnionSelectContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUnionParenthesisSelect(ctx: MySqlParser.UnionParenthesisSelectContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitWithLateralStatement(ctx: MySqlParser.WithLateralStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUpdateStatement(ctx: MySqlParser.UpdateStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitValuesStatement(ctx: MySqlParser.ValuesStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitInsertStatementValue(ctx: MySqlParser.InsertStatementValueContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUpdatedElement(ctx: MySqlParser.UpdatedElementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAssignmentField(ctx: MySqlParser.AssignmentFieldContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLockClause(ctx: MySqlParser.LockClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSingleDeleteStatement(ctx: MySqlParser.SingleDeleteStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitMultipleDeleteStatement(ctx: MySqlParser.MultipleDeleteStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHandlerOpenStatement(ctx: MySqlParser.HandlerOpenStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHandlerReadIndexStatement(ctx: MySqlParser.HandlerReadIndexStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHandlerReadStatement(ctx: MySqlParser.HandlerReadStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHandlerCloseStatement(ctx: MySqlParser.HandlerCloseStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSingleUpdateStatement(ctx: MySqlParser.SingleUpdateStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitMultipleUpdateStatement(ctx: MySqlParser.MultipleUpdateStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitOrderByClause(ctx: MySqlParser.OrderByClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitOrderByExpression(ctx: MySqlParser.OrderByExpressionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableSources(ctx: TableSourcesContext): Table
    {
        val children: MutableSet<TableItem> = HashSet<TableItem>().toMutableSet()
        ctx.tableSource().forEach { children += visitTableSourceBase(it as TableSourceBaseContext) }
        return Table(getLocation(ctx), children)
    }

    override fun visitTableSourceBase(ctx: TableSourceBaseContext): TableItem
    {
        return visitAtomTableItem(ctx.tableSourceItem() as AtomTableItemContext)
    }

    override fun visitTableSourceNested(ctx: MySqlParser.TableSourceNestedContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableJson(ctx: MySqlParser.TableJsonContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAtomTableItem(ctx: AtomTableItemContext): TableItem
    {
        var alias = ""
        if (ctx.alias != null)
        {
            alias = ctx.alias.text
        }
        return TableItem(getLocation(ctx), ctx.tableName().text, alias)
    }

    override fun visitSubqueryTableItem(ctx: MySqlParser.SubqueryTableItemContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableSourcesItem(ctx: MySqlParser.TableSourcesItemContext): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIndexHint(ctx: MySqlParser.IndexHintContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIndexHintType(ctx: MySqlParser.IndexHintTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitInnerJoin(ctx: MySqlParser.InnerJoinContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitStraightJoin(ctx: MySqlParser.StraightJoinContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitOuterJoin(ctx: MySqlParser.OuterJoinContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitNaturalJoin(ctx: MySqlParser.NaturalJoinContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitJoinSpec(ctx: MySqlParser.JoinSpecContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitQueryExpression(ctx: MySqlParser.QueryExpressionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitQueryExpressionNointo(ctx: MySqlParser.QueryExpressionNointoContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitQuerySpecification(ctx: QuerySpecificationContext): Node
    {
        val selectItems: List<SelectItem> = visitSelectElements(ctx.selectElements(), SelectItem::class.java)
        val fromClause: Relation = visitFromClause(ctx.fromClause())

        var limitClause: Limit = Limit(Optional.empty(), emptySet())
        if (ctx.limitClause() != null)
        {
            limitClause = visitLimitClause(ctx.limitClause())
        }

        if (ctx.groupByClause() != null) {
            visitGroupByClause(ctx.groupByClause())
        }

        return QuerySpecification(
            getLocation(ctx),
            Select(getLocation(ctx.selectElements()), false, selectItems),
            fromClause,
            limitClause
        )
    }

    override fun visitQuerySpecificationNointo(ctx: MySqlParser.QuerySpecificationNointoContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUnionParenthesis(ctx: MySqlParser.UnionParenthesisContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUnionStatement(ctx: MySqlParser.UnionStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLateralStatement(ctx: MySqlParser.LateralStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitJsonTable(ctx: MySqlParser.JsonTableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitJsonColumnList(ctx: MySqlParser.JsonColumnListContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitJsonColumn(ctx: MySqlParser.JsonColumnContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitJsonOnEmpty(ctx: MySqlParser.JsonOnEmptyContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitJsonOnError(ctx: MySqlParser.JsonOnErrorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSelectSpec(ctx: MySqlParser.SelectSpecContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSelectElements(ctx: MySqlParser.SelectElementsContext): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSelectStarElement(ctx: MySqlParser.SelectStarElementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSelectColumnElement(ctx: SelectColumnElementContext): Node
    {
        var columnName = ctx.fullColumnName().text
        return SelectItem(getLocation(ctx), columnName)
    }

    override fun visitSelectFunctionElement(ctx: MySqlParser.SelectFunctionElementContext?): Node
    {
//        val functionName = ctx.functionCall().functionName().text
//        val arguments = ctx.functionCall().expressionList()?.let { visitExpressions(it) } ?: emptyList()
//        return when (functionName.toUpperCase()) {
//            "COUNT" -> AggregateFunction(getLocation(ctx), functionName, arguments)
//            // 添加其他聚合函数的支持,如 SUM, AVG, MIN, MAX 等
//            else -> throw UnsupportedOperationException("Unsupported function: $functionName")
//        }

        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSelectExpressionElement(ctx: SelectExpressionElementContext): Node
    {
        return SelectItem(getLocation(ctx.expression()), ctx.expression().text)
    }

    override fun visitSelectIntoVariables(ctx: MySqlParser.SelectIntoVariablesContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSelectIntoDumpFile(ctx: MySqlParser.SelectIntoDumpFileContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSelectIntoTextFile(ctx: MySqlParser.SelectIntoTextFileContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSelectFieldsInto(ctx: MySqlParser.SelectFieldsIntoContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSelectLinesInto(ctx: MySqlParser.SelectLinesIntoContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFromClause(ctx: FromClauseContext): Relation
    {
        if (ctx.tableSources() != null)
        {
            return Relation(getLocation(ctx), visitTableSources(ctx.tableSources()))
        }
        return Relation(getLocation(ctx), Table(getLocation(ctx), emptySet()))
    }

    override fun visitGroupByClause(ctx: MySqlParser.GroupByClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHavingClause(ctx: MySqlParser.HavingClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitWindowClause(ctx: MySqlParser.WindowClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitGroupByItem(ctx: MySqlParser.GroupByItemContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLimitClause(ctx: LimitClauseContext): Limit
    {
        val children: MutableSet<LimitItem> = HashSet<LimitItem>().toMutableSet()
        ctx?.limitClauseAtom()?.forEach { children += visitLimitClauseAtom(it) }
        return Limit(getLocation(ctx), children)
    }

    override fun visitLimitClauseAtom(ctx: LimitClauseAtomContext): LimitItem
    {
        return LimitItem(getLocation(ctx), ctx.text.toLong())
    }

    override fun visitStartTransaction(ctx: MySqlParser.StartTransactionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitBeginWork(ctx: MySqlParser.BeginWorkContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCommitWork(ctx: MySqlParser.CommitWorkContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRollbackWork(ctx: MySqlParser.RollbackWorkContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSavepointStatement(ctx: MySqlParser.SavepointStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRollbackStatement(ctx: MySqlParser.RollbackStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitReleaseStatement(ctx: MySqlParser.ReleaseStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLockTables(ctx: MySqlParser.LockTablesContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUnlockTables(ctx: MySqlParser.UnlockTablesContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSetAutocommitStatement(ctx: MySqlParser.SetAutocommitStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSetTransactionStatement(ctx: MySqlParser.SetTransactionStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTransactionMode(ctx: MySqlParser.TransactionModeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLockTableElement(ctx: MySqlParser.LockTableElementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLockAction(ctx: MySqlParser.LockActionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTransactionOption(ctx: MySqlParser.TransactionOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTransactionLevel(ctx: MySqlParser.TransactionLevelContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitChangeMaster(ctx: MySqlParser.ChangeMasterContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitChangeReplicationFilter(ctx: MySqlParser.ChangeReplicationFilterContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPurgeBinaryLogs(ctx: MySqlParser.PurgeBinaryLogsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitResetMaster(ctx: MySqlParser.ResetMasterContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitResetSlave(ctx: MySqlParser.ResetSlaveContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitStartSlave(ctx: MySqlParser.StartSlaveContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitStopSlave(ctx: MySqlParser.StopSlaveContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitStartGroupReplication(ctx: MySqlParser.StartGroupReplicationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitStopGroupReplication(ctx: MySqlParser.StopGroupReplicationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitMasterStringOption(ctx: MySqlParser.MasterStringOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitMasterDecimalOption(ctx: MySqlParser.MasterDecimalOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitMasterBoolOption(ctx: MySqlParser.MasterBoolOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitMasterRealOption(ctx: MySqlParser.MasterRealOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitMasterUidListOption(ctx: MySqlParser.MasterUidListOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitStringMasterOption(ctx: MySqlParser.StringMasterOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDecimalMasterOption(ctx: MySqlParser.DecimalMasterOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitBoolMasterOption(ctx: MySqlParser.BoolMasterOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitChannelOption(ctx: MySqlParser.ChannelOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDoDbReplication(ctx: MySqlParser.DoDbReplicationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIgnoreDbReplication(ctx: MySqlParser.IgnoreDbReplicationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDoTableReplication(ctx: MySqlParser.DoTableReplicationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIgnoreTableReplication(ctx: MySqlParser.IgnoreTableReplicationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitWildDoTableReplication(ctx: MySqlParser.WildDoTableReplicationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitWildIgnoreTableReplication(ctx: MySqlParser.WildIgnoreTableReplicationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRewriteDbReplication(ctx: MySqlParser.RewriteDbReplicationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTablePair(ctx: MySqlParser.TablePairContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitThreadType(ctx: MySqlParser.ThreadTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitGtidsUntilOption(ctx: MySqlParser.GtidsUntilOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitMasterLogUntilOption(ctx: MySqlParser.MasterLogUntilOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRelayLogUntilOption(ctx: MySqlParser.RelayLogUntilOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSqlGapsUntilOption(ctx: MySqlParser.SqlGapsUntilOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUserConnectionOption(ctx: MySqlParser.UserConnectionOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPasswordConnectionOption(ctx: MySqlParser.PasswordConnectionOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDefaultAuthConnectionOption(ctx: MySqlParser.DefaultAuthConnectionOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPluginDirConnectionOption(ctx: MySqlParser.PluginDirConnectionOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitGtuidSet(ctx: MySqlParser.GtuidSetContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitXaStartTransaction(ctx: MySqlParser.XaStartTransactionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitXaEndTransaction(ctx: MySqlParser.XaEndTransactionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitXaPrepareStatement(ctx: MySqlParser.XaPrepareStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitXaCommitWork(ctx: MySqlParser.XaCommitWorkContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitXaRollbackWork(ctx: MySqlParser.XaRollbackWorkContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitXaRecoverWork(ctx: MySqlParser.XaRecoverWorkContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPrepareStatement(ctx: MySqlParser.PrepareStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitExecuteStatement(ctx: MySqlParser.ExecuteStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDeallocatePrepare(ctx: MySqlParser.DeallocatePrepareContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRoutineBody(ctx: MySqlParser.RoutineBodyContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitBlockStatement(ctx: MySqlParser.BlockStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCaseStatement(ctx: MySqlParser.CaseStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIfStatement(ctx: MySqlParser.IfStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIterateStatement(ctx: MySqlParser.IterateStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLeaveStatement(ctx: MySqlParser.LeaveStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLoopStatement(ctx: MySqlParser.LoopStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRepeatStatement(ctx: MySqlParser.RepeatStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitReturnStatement(ctx: MySqlParser.ReturnStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitWhileStatement(ctx: MySqlParser.WhileStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCloseCursor(ctx: MySqlParser.CloseCursorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFetchCursor(ctx: MySqlParser.FetchCursorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitOpenCursor(ctx: MySqlParser.OpenCursorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDeclareVariable(ctx: MySqlParser.DeclareVariableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDeclareCondition(ctx: MySqlParser.DeclareConditionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDeclareCursor(ctx: MySqlParser.DeclareCursorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDeclareHandler(ctx: MySqlParser.DeclareHandlerContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHandlerConditionCode(ctx: MySqlParser.HandlerConditionCodeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHandlerConditionState(ctx: MySqlParser.HandlerConditionStateContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHandlerConditionName(ctx: MySqlParser.HandlerConditionNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHandlerConditionWarning(ctx: MySqlParser.HandlerConditionWarningContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHandlerConditionNotfound(ctx: MySqlParser.HandlerConditionNotfoundContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHandlerConditionException(ctx: MySqlParser.HandlerConditionExceptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitProcedureSqlStatement(ctx: MySqlParser.ProcedureSqlStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCaseAlternative(ctx: MySqlParser.CaseAlternativeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitElifAlternative(ctx: MySqlParser.ElifAlternativeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterUserMysqlV56(ctx: MySqlParser.AlterUserMysqlV56Context?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAlterUserMysqlV80(ctx: MySqlParser.AlterUserMysqlV80Context?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateUserMysqlV56(ctx: MySqlParser.CreateUserMysqlV56Context?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateUserMysqlV80(ctx: MySqlParser.CreateUserMysqlV80Context?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDropUser(ctx: MySqlParser.DropUserContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitGrantStatement(ctx: MySqlParser.GrantStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRoleOption(ctx: MySqlParser.RoleOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitGrantProxy(ctx: MySqlParser.GrantProxyContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRenameUser(ctx: MySqlParser.RenameUserContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDetailRevoke(ctx: MySqlParser.DetailRevokeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShortRevoke(ctx: MySqlParser.ShortRevokeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRoleRevoke(ctx: MySqlParser.RoleRevokeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRevokeProxy(ctx: MySqlParser.RevokeProxyContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSetPasswordStatement(ctx: MySqlParser.SetPasswordStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUserSpecification(ctx: MySqlParser.UserSpecificationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHashAuthOption(ctx: MySqlParser.HashAuthOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRandomAuthOption(ctx: MySqlParser.RandomAuthOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitStringAuthOption(ctx: MySqlParser.StringAuthOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitModuleAuthOption(ctx: MySqlParser.ModuleAuthOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSimpleAuthOption(ctx: MySqlParser.SimpleAuthOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAuthOptionClause(ctx: MySqlParser.AuthOptionClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitModule(ctx: MySqlParser.ModuleContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPasswordModuleOption(ctx: MySqlParser.PasswordModuleOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTlsOption(ctx: MySqlParser.TlsOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUserResourceOption(ctx: MySqlParser.UserResourceOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUserPasswordOption(ctx: MySqlParser.UserPasswordOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUserLockOption(ctx: MySqlParser.UserLockOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPrivelegeClause(ctx: MySqlParser.PrivelegeClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPrivilege(ctx: MySqlParser.PrivilegeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCurrentSchemaPriviLevel(ctx: MySqlParser.CurrentSchemaPriviLevelContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitGlobalPrivLevel(ctx: MySqlParser.GlobalPrivLevelContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDefiniteSchemaPrivLevel(ctx: MySqlParser.DefiniteSchemaPrivLevelContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDefiniteFullTablePrivLevel(ctx: MySqlParser.DefiniteFullTablePrivLevelContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDefiniteFullTablePrivLevel2(ctx: MySqlParser.DefiniteFullTablePrivLevel2Context?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDefiniteTablePrivLevel(ctx: MySqlParser.DefiniteTablePrivLevelContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRenameUserClause(ctx: MySqlParser.RenameUserClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAnalyzeTable(ctx: MySqlParser.AnalyzeTableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCheckTable(ctx: MySqlParser.CheckTableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitChecksumTable(ctx: MySqlParser.ChecksumTableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitOptimizeTable(ctx: MySqlParser.OptimizeTableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRepairTable(ctx: MySqlParser.RepairTableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCheckTableOption(ctx: MySqlParser.CheckTableOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCreateUdfunction(ctx: MySqlParser.CreateUdfunctionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitInstallPlugin(ctx: MySqlParser.InstallPluginContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUninstallPlugin(ctx: MySqlParser.UninstallPluginContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSetVariable(ctx: MySqlParser.SetVariableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSetCharset(ctx: MySqlParser.SetCharsetContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSetNames(ctx: MySqlParser.SetNamesContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSetPassword(ctx: MySqlParser.SetPasswordContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSetTransaction(ctx: MySqlParser.SetTransactionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSetAutocommit(ctx: MySqlParser.SetAutocommitContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSetNewValueInsideTrigger(ctx: MySqlParser.SetNewValueInsideTriggerContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowMasterLogs(ctx: MySqlParser.ShowMasterLogsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowLogEvents(ctx: MySqlParser.ShowLogEventsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowObjectFilter(ctx: MySqlParser.ShowObjectFilterContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowColumns(ctx: MySqlParser.ShowColumnsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowCreateDb(ctx: MySqlParser.ShowCreateDbContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowCreateFullIdObject(ctx: MySqlParser.ShowCreateFullIdObjectContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowCreateUser(ctx: MySqlParser.ShowCreateUserContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowEngine(ctx: MySqlParser.ShowEngineContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowGlobalInfo(ctx: MySqlParser.ShowGlobalInfoContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowErrors(ctx: MySqlParser.ShowErrorsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowCountErrors(ctx: MySqlParser.ShowCountErrorsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowSchemaFilter(ctx: MySqlParser.ShowSchemaFilterContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowRoutine(ctx: MySqlParser.ShowRoutineContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowGrants(ctx: MySqlParser.ShowGrantsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowIndexes(ctx: MySqlParser.ShowIndexesContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowOpenTables(ctx: MySqlParser.ShowOpenTablesContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowProfile(ctx: MySqlParser.ShowProfileContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowSlaveStatus(ctx: MySqlParser.ShowSlaveStatusContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitVariableClause(ctx: MySqlParser.VariableClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowCommonEntity(ctx: MySqlParser.ShowCommonEntityContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowFilter(ctx: MySqlParser.ShowFilterContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowGlobalInfoClause(ctx: MySqlParser.ShowGlobalInfoClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowSchemaEntity(ctx: MySqlParser.ShowSchemaEntityContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShowProfileType(ctx: MySqlParser.ShowProfileTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitBinlogStatement(ctx: MySqlParser.BinlogStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCacheIndexStatement(ctx: MySqlParser.CacheIndexStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFlushStatement(ctx: MySqlParser.FlushStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitKillStatement(ctx: MySqlParser.KillStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLoadIndexIntoCache(ctx: MySqlParser.LoadIndexIntoCacheContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitResetStatement(ctx: MySqlParser.ResetStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitShutdownStatement(ctx: MySqlParser.ShutdownStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableIndexes(ctx: MySqlParser.TableIndexesContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSimpleFlushOption(ctx: MySqlParser.SimpleFlushOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitChannelFlushOption(ctx: MySqlParser.ChannelFlushOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableFlushOption(ctx: MySqlParser.TableFlushOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFlushTableOption(ctx: MySqlParser.FlushTableOptionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLoadedTableIndexes(ctx: MySqlParser.LoadedTableIndexesContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSimpleDescribeStatement(ctx: MySqlParser.SimpleDescribeStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFullDescribeStatement(ctx: MySqlParser.FullDescribeStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHelpStatement(ctx: MySqlParser.HelpStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUseStatement(ctx: MySqlParser.UseStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSignalStatement(ctx: MySqlParser.SignalStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitResignalStatement(ctx: MySqlParser.ResignalStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSignalConditionInformation(ctx: MySqlParser.SignalConditionInformationContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitWithStatement(ctx: MySqlParser.WithStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableStatement(ctx: MySqlParser.TableStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDiagnosticsStatement(ctx: MySqlParser.DiagnosticsStatementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDiagnosticsConditionInformationName(ctx: MySqlParser.DiagnosticsConditionInformationNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDescribeStatements(ctx: MySqlParser.DescribeStatementsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDescribeConnection(ctx: MySqlParser.DescribeConnectionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFullId(ctx: MySqlParser.FullIdContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTableName(ctx: MySqlParser.TableNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRoleName(ctx: MySqlParser.RoleNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFullColumnName(ctx: MySqlParser.FullColumnNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIndexColumnName(ctx: MySqlParser.IndexColumnNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSimpleUserName(ctx: MySqlParser.SimpleUserNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHostName(ctx: MySqlParser.HostNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUserName(ctx: MySqlParser.UserNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitMysqlVariable(ctx: MySqlParser.MysqlVariableContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCharsetName(ctx: MySqlParser.CharsetNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCollationName(ctx: MySqlParser.CollationNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitEngineName(ctx: MySqlParser.EngineNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitEngineNameBase(ctx: MySqlParser.EngineNameBaseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUuidSet(ctx: MySqlParser.UuidSetContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitXid(ctx: MySqlParser.XidContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitXuidStringId(ctx: MySqlParser.XuidStringIdContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAuthPlugin(ctx: MySqlParser.AuthPluginContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUid(ctx: MySqlParser.UidContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSimpleId(ctx: MySqlParser.SimpleIdContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDottedId(ctx: MySqlParser.DottedIdContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDecimalLiteral(ctx: MySqlParser.DecimalLiteralContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFileSizeLiteral(ctx: MySqlParser.FileSizeLiteralContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitStringLiteral(ctx: MySqlParser.StringLiteralContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitBooleanLiteral(ctx: MySqlParser.BooleanLiteralContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitHexadecimalLiteral(ctx: MySqlParser.HexadecimalLiteralContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitNullNotnull(ctx: MySqlParser.NullNotnullContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitConstant(ctx: MySqlParser.ConstantContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitStringDataType(ctx: MySqlParser.StringDataTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitNationalVaryingStringDataType(ctx: MySqlParser.NationalVaryingStringDataTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitNationalStringDataType(ctx: MySqlParser.NationalStringDataTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDimensionDataType(ctx: MySqlParser.DimensionDataTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSimpleDataType(ctx: MySqlParser.SimpleDataTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCollectionDataType(ctx: MySqlParser.CollectionDataTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSpatialDataType(ctx: MySqlParser.SpatialDataTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLongVarcharDataType(ctx: MySqlParser.LongVarcharDataTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLongVarbinaryDataType(ctx: MySqlParser.LongVarbinaryDataTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCollectionOptions(ctx: MySqlParser.CollectionOptionsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitConvertedDataType(ctx: MySqlParser.ConvertedDataTypeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLengthOneDimension(ctx: MySqlParser.LengthOneDimensionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLengthTwoDimension(ctx: MySqlParser.LengthTwoDimensionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLengthTwoOptionalDimension(ctx: MySqlParser.LengthTwoOptionalDimensionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUidList(ctx: MySqlParser.UidListContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFullColumnNameList(ctx: MySqlParser.FullColumnNameListContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTables(ctx: MySqlParser.TablesContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIndexColumnNames(ctx: MySqlParser.IndexColumnNamesContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitExpressions(ctx: MySqlParser.ExpressionsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitExpressionsWithDefaults(ctx: MySqlParser.ExpressionsWithDefaultsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitConstants(ctx: MySqlParser.ConstantsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSimpleStrings(ctx: MySqlParser.SimpleStringsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUserVariables(ctx: MySqlParser.UserVariablesContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDefaultValue(ctx: MySqlParser.DefaultValueContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCurrentTimestamp(ctx: MySqlParser.CurrentTimestampContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitExpressionOrDefault(ctx: MySqlParser.ExpressionOrDefaultContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIfExists(ctx: MySqlParser.IfExistsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIfNotExists(ctx: MySqlParser.IfNotExistsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitOrReplace(ctx: MySqlParser.OrReplaceContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitWaitNowaitClause(ctx: MySqlParser.WaitNowaitClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSpecificFunctionCall(ctx: MySqlParser.SpecificFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAggregateFunctionCall(ctx: MySqlParser.AggregateFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitNonAggregateFunctionCall(ctx: MySqlParser.NonAggregateFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitScalarFunctionCall(ctx: MySqlParser.ScalarFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUdfFunctionCall(ctx: MySqlParser.UdfFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPasswordFunctionCall(ctx: MySqlParser.PasswordFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSimpleFunctionCall(ctx: MySqlParser.SimpleFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCurrentUser(ctx: MySqlParser.CurrentUserContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDataTypeFunctionCall(ctx: MySqlParser.DataTypeFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitValuesFunctionCall(ctx: MySqlParser.ValuesFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCaseExpressionFunctionCall(ctx: MySqlParser.CaseExpressionFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCaseFunctionCall(ctx: MySqlParser.CaseFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCharFunctionCall(ctx: MySqlParser.CharFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPositionFunctionCall(ctx: MySqlParser.PositionFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSubstrFunctionCall(ctx: MySqlParser.SubstrFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTrimFunctionCall(ctx: MySqlParser.TrimFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitWeightFunctionCall(ctx: MySqlParser.WeightFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitExtractFunctionCall(ctx: MySqlParser.ExtractFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitGetFormatFunctionCall(ctx: MySqlParser.GetFormatFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitJsonValueFunctionCall(ctx: MySqlParser.JsonValueFunctionCallContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCaseFuncAlternative(ctx: MySqlParser.CaseFuncAlternativeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLevelWeightList(ctx: MySqlParser.LevelWeightListContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLevelWeightRange(ctx: MySqlParser.LevelWeightRangeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLevelInWeightListElement(ctx: MySqlParser.LevelInWeightListElementContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAggregateWindowedFunction(ctx: MySqlParser.AggregateWindowedFunctionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitNonAggregateWindowedFunction(ctx: MySqlParser.NonAggregateWindowedFunctionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitOverClause(ctx: MySqlParser.OverClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitWindowSpec(ctx: MySqlParser.WindowSpecContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitWindowName(ctx: MySqlParser.WindowNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFrameClause(ctx: MySqlParser.FrameClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFrameUnits(ctx: MySqlParser.FrameUnitsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFrameExtent(ctx: MySqlParser.FrameExtentContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFrameBetween(ctx: MySqlParser.FrameBetweenContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFrameRange(ctx: MySqlParser.FrameRangeContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPartitionClause(ctx: MySqlParser.PartitionClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitScalarFunctionName(ctx: MySqlParser.ScalarFunctionNameContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPasswordFunctionClause(ctx: MySqlParser.PasswordFunctionClauseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFunctionArgs(ctx: MySqlParser.FunctionArgsContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFunctionArg(ctx: MySqlParser.FunctionArgContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIsExpression(ctx: MySqlParser.IsExpressionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitNotExpression(ctx: MySqlParser.NotExpressionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLogicalExpression(ctx: MySqlParser.LogicalExpressionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPredicateExpression(ctx: MySqlParser.PredicateExpressionContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSoundsLikePredicate(ctx: MySqlParser.SoundsLikePredicateContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitExpressionAtomPredicate(ctx: MySqlParser.ExpressionAtomPredicateContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSubqueryComparisonPredicate(ctx: MySqlParser.SubqueryComparisonPredicateContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitJsonMemberOfPredicate(ctx: MySqlParser.JsonMemberOfPredicateContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitBinaryComparisonPredicate(ctx: MySqlParser.BinaryComparisonPredicateContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitInPredicate(ctx: MySqlParser.InPredicateContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitBetweenPredicate(ctx: MySqlParser.BetweenPredicateContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIsNullPredicate(ctx: MySqlParser.IsNullPredicateContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLikePredicate(ctx: MySqlParser.LikePredicateContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitRegexpPredicate(ctx: MySqlParser.RegexpPredicateContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUnaryExpressionAtom(ctx: MySqlParser.UnaryExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCollateExpressionAtom(ctx: MySqlParser.CollateExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitVariableAssignExpressionAtom(ctx: MySqlParser.VariableAssignExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitMysqlVariableExpressionAtom(ctx: MySqlParser.MysqlVariableExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitNestedExpressionAtom(ctx: MySqlParser.NestedExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitNestedRowExpressionAtom(ctx: MySqlParser.NestedRowExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitMathExpressionAtom(ctx: MySqlParser.MathExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitExistsExpressionAtom(ctx: MySqlParser.ExistsExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIntervalExpressionAtom(ctx: MySqlParser.IntervalExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitJsonExpressionAtom(ctx: MySqlParser.JsonExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitSubqueryExpressionAtom(ctx: MySqlParser.SubqueryExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitConstantExpressionAtom(ctx: MySqlParser.ConstantExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFunctionCallExpressionAtom(ctx: MySqlParser.FunctionCallExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitBinaryExpressionAtom(ctx: MySqlParser.BinaryExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFullColumnNameExpressionAtom(ctx: MySqlParser.FullColumnNameExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitBitExpressionAtom(ctx: MySqlParser.BitExpressionAtomContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitUnaryOperator(ctx: MySqlParser.UnaryOperatorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitComparisonOperator(ctx: MySqlParser.ComparisonOperatorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitLogicalOperator(ctx: MySqlParser.LogicalOperatorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitBitOperator(ctx: MySqlParser.BitOperatorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitMultOperator(ctx: MySqlParser.MultOperatorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitAddOperator(ctx: MySqlParser.AddOperatorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitJsonOperator(ctx: MySqlParser.JsonOperatorContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitCharsetNameBase(ctx: MySqlParser.CharsetNameBaseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitTransactionLevelBase(ctx: MySqlParser.TransactionLevelBaseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitPrivilegesBase(ctx: MySqlParser.PrivilegesBaseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitIntervalTypeBase(ctx: MySqlParser.IntervalTypeBaseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitDataTypeBase(ctx: MySqlParser.DataTypeBaseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitKeywordsCanBeId(ctx: MySqlParser.KeywordsCanBeIdContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }

    override fun visitFunctionNameBase(ctx: MySqlParser.FunctionNameBaseContext?): Node
    {
        throw UnsupportedOperationException("Unsupported type: ${ctx?.javaClass?.simpleName}")
    }
}
