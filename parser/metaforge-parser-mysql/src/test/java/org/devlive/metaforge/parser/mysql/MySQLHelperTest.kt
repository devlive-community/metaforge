package org.devlive.metaforge.parser.mysql

import org.devlive.metaforge.parser.ParserResponse
import org.devlive.metaforge.parser.type.StatementType
import org.junit.Assert.assertTrue
import org.junit.Test
import org.slf4j.LoggerFactory.getLogger

class MySQLHelperTest
{
    private val log = getLogger(MySQLHelperTest::class.java)

    private fun printLog(response: ParserResponse)
    {
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
    fun test()
    {
        val response = MySQLHelper.parse("SELECT 1")
        printLog(response)
        assertTrue(response.type == StatementType.SELECT)
    }

    @Test
    fun testFromTable()
    {
        val response = MySQLHelper.parse("SELECT \"name\", \"age\" FROM \"a_table_name\"")
        printLog(response)
        assertTrue(response.isParser)
    }

    @Test
    fun testLimit()
    {
        val response = MySQLHelper.parse("SELECT * FROM \"a_table_name\" LIMIT 10")
        printLog(response)
        assertTrue(response.table.limit == 10L)
    }

    @Test
    fun testFull001()
    {
        val response = MySQLHelper.parse("select count(`id`), query_mode\n" +
                "from metaforge.metaforge_source_query\n" +
                "group by query_mode\n" +
                "LIMIT 10000")
        printLog(response)
    }
}
