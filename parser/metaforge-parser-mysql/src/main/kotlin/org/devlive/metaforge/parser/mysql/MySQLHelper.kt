package org.devlive.metaforge.parser.mysql

import org.devlive.metaforge.parser.ParserResponse
import org.devlive.metaforge.parser.model.Column
import org.devlive.metaforge.parser.model.Table
import org.devlive.metaforge.parser.mysql.companion.MySQLEndpoint
import org.devlive.metaforge.parser.mysql.companion.ParserException
import org.devlive.metaforge.parser.mysql.companion.ast.ParserOptions
import org.devlive.metaforge.parser.mysql.companion.tree.QuerySpecification
import org.devlive.metaforge.parser.mysql.companion.tree.Statement
import org.devlive.metaforge.parser.type.EngineType
import org.devlive.metaforge.parser.type.StatementType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object MySQLHelper {
    private val log: Logger = LoggerFactory.getLogger(MySQLHelper::class.java)

    @JvmStatic
    fun parse(sql: String): ParserResponse {
        val response = ParserResponse()
        response.engine = EngineType.MYSQL
        try {
            response.isParser = true
            check(MySQLEndpoint().createStatement(sql, ParserOptions.DecimalLiteralTreatment.AS_DECIMAL), response)
        }
        catch (e: ParserException) {
            response.isParser = false
            response.message = e.message
            log.error("Parse error", e)
        }
        return response
    }

    @Throws(ParserException::class)
    private fun check(statement: Statement, response: ParserResponse) {
        when (statement) {
            is QuerySpecification -> {
                response.type = StatementType.SELECT

                val table = Table()
                statement.from.table.children.forEach {
                    table.name = it.name
                    table.alias = it.alias
                }

                statement.select.children.forEach {
                    val column = Column()
                    column.name = it.name
                    table.columns.plus(column)
                }

                statement.limit.children.forEach {
                    table.limit = it.limit
                }
                response.table = table
            }

            else -> throw ParserException("Unsupported statement: " + statement.javaClass)
        }
    }
}
