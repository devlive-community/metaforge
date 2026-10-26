package org.devlive.metaforge.parser

import org.devlive.metaforge.parser.model.Table

class TestSqlParser : SqlParser
{
    override fun parse(sql: String): ParserResponse
    {
        val response = ParserResponse()
        val table = Table()
        table.name = "Test"
        response.table = table
        return response
    }
}
