package org.devlive.metaforge.parser.trino

import org.devlive.metaforge.parser.ParserResponse
import org.devlive.metaforge.parser.type.StatementType
import org.junit.Assert.assertTrue
import org.junit.Test
import org.slf4j.LoggerFactory.getLogger

class TrinoHelperTest {
    private val log = getLogger(TrinoHelperTest::class.java)

    private fun printLog(response: ParserResponse) {
        log.info("engine: [ {} ]", response.engine)
        log.info("type: [ {} ]", response.type)
        log.info("isParser: [ {} ]", response.isParser)
        log.info("database name: [ {} ]", response.table.database)
        log.info("table name: [ {} ]", response.table.name)
        log.info("table limit: [ {} ]", response.table.limit)
        log.info("message: [ {} ]", response.message)
        response.table.columns.forEach {
            log.info("column name: [ {} ], alias: [ {} ], type: [ {} ], expression: [ {} ], functions: [ {} ]", it.name, it.alias, it.type, it.expression, it.functions)
        }
    }

    @Test
    fun test() {
        val response = TrinoHelper.parse("SELECT 1")
        printLog(response)
        assertTrue(response.type == StatementType.SELECT)
    }

    @Test
    fun testFromTable() {
        val response = TrinoHelper.parse("SELECT \"name\", \"age\" FROM \"a_table_name\"")
        printLog(response)
        assertTrue(response.isParser)
    }

    @Test
    fun testFromTableWithAlias() {
        val response = TrinoHelper.parse("SELECT \"name\" AS \"name\", \"age\" AS \"age\", \"summer\" FROM \"a_table_name\"")
        printLog(response)
        assertTrue(response.table.columns.find { it.alias != null } != null)
    }

    @Test
    fun testFromTableWithAliasAndExpression() {
        val response = TrinoHelper.parse("SELECT \"name\" AS \"name\", \"age\" AS \"age\", \"summer\" + 1 AS \"summer\" FROM \"a_table_name\"")
        printLog(response)
        assertTrue(response.table.columns.find { it.expression != null }?.expression == "(\"summer\" + 1)")
    }

    @Test
    fun testFromTableWithAliasAndExpressionAndFunction() {
        val response = TrinoHelper.parse("SELECT \"name\" AS \"name\", \"age\" AS \"age\", \"summer\" + 1 AS \"summer\", SUM(\"summer\") FROM \"a_table_name\"")
        printLog(response)
        assertTrue(response.table.columns.find { it.expression != null }?.expression == "(\"summer\" + 1)")
    }

    @Test
    fun testLimit() {
        val response = TrinoHelper.parse("SELECT * FROM \"a_table_name\" LIMIT 10")
        printLog(response)
        assertTrue(response.table.limit == 10L)
    }
}
