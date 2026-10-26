package org.devlive.metaforge.parser.mysql

import org.devlive.metaforge.parser.ParserResponse
import org.devlive.metaforge.parser.SqlParser

class MySQLSqlParser : SqlParser {
    override fun parse(sql: String): ParserResponse {
        return MySQLHelper.parse(sql)
    }
}
