package org.devlive.metaforge.parser.trino

import org.devlive.metaforge.parser.ParserResponse
import org.devlive.metaforge.parser.ParsingException
import org.devlive.metaforge.parser.model.Column
import org.devlive.metaforge.parser.node.Node
import org.devlive.metaforge.parser.parser.ParserOptions
import org.devlive.metaforge.parser.parser.SqlParser
import org.devlive.metaforge.parser.tree.*
import org.devlive.metaforge.parser.tree.query.Query
import org.devlive.metaforge.parser.tree.query.QuerySpecification
import org.devlive.metaforge.parser.tree.table.Table
import org.devlive.metaforge.parser.type.EngineType
import org.devlive.metaforge.parser.type.StatementType
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger

object TrinoHelper {
    private val log: Logger = getLogger(TrinoHelper::class.java)

    @JvmStatic
    fun parse(sql: String): ParserResponse {
        val response = ParserResponse()
        response.engine = EngineType.TRINO
        try {
            response.isParser = true
            check(SqlParser().createStatement(sql, ParserOptions(ParserOptions.DecimalLiteralTreatment.AS_DECIMAL)), response)
        }
        catch (e: ParsingException) {
            response.isParser = false
            response.message = e.message
            log.error("Parse error", e)
        }
        return response
    }

    @Throws(ParsingException::class)
    private fun check(statement: Statement, response: ParserResponse) {
        if (statement is Query) {
            response.type = StatementType.SELECT
            val query: Query = statement
            val children: List<Node> = query.getChildren()
            for (child in children) {
                eachNode(child, response)
            }
        }
    }

    @Throws(ParsingException::class)
    private fun eachNode(node: Node, response: ParserResponse) {
        when (node) {
            // Parse the query
            is Query -> {
                loopNode(node.children, response)
            }
            // Parse the query specification
            is QuerySpecification -> {
                loopNode(node.children, response)
                node.limit.ifPresent {
                    val limit = it as Limit
                    val rowCount = limit.rowCount as LongLiteral
                    response.table.limit = rowCount.value
                }
            }
            // Parse the select
            is Select -> {
                node.selectItems.forEach {
                    when (it) {
                        is SingleColumn -> {
                            getColumn(it, response)
                        }
                        // If the query is all columns, add them directly to the collection
                        is AllColumns -> {
                            val column = Column()
                            column.name = it.toString()
                            response.table.columns += column
                        }
                    }
                }
            }
            // Parse the table
            is Table -> {
                val (database, name) = node.name.toString().split(".").let {
                    if (it.size > 1) Pair(it[0], it[1]) else Pair(null, it[0])
                }

                response.table.apply {
                    this.database = database
                    this.name = name
                }

                loopNode(node.children, response)
            }
        }
    }

    @Throws(ParsingException::class)
    private fun loopNode(children: List<Node>, response: ParserResponse) {
        for (node in children) {
            eachNode(node, response)
        }
    }

    @Throws(ParsingException::class)
    private fun getColumn(node: Node, response: ParserResponse) {
        when (node) {
            is SingleColumn -> {
                val column = Column()
                getNameByExpression(node.expression, column)
                node.alias.ifPresent { column.alias = it.value }
                response.table.columns += column
            }

            else -> {
                throw ParsingException("Invalid query")
            }
        }
    }

    @Throws(ParsingException::class)
    private fun getNameByExpression(expression: Expression, column: Column) {
        when (expression) {
            // Get the column name
            is Identifier -> {
                column.name = expression.value
            }
            // Get the arithmetic binary expression
            is ArithmeticBinaryExpression -> {
                column.expression = expression.toString()
                val left = expression.left as Identifier
                column.name = left.value
            }
            // Get the function call
            is FunctionCall -> {
                formatFunctionCall(expression, column)
            }
        }
    }

    @Throws(ParsingException::class)
    private fun formatFunctionCall(call: FunctionCall, column: Column) {
        val functions = call.name.parts.toSet()
        column.functions = functions
        column.name = call.arguments[0].toString()
    }
}
