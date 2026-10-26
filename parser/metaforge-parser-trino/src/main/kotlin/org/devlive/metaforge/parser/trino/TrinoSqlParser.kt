package org.devlive.metaforge.parser.trino

import org.devlive.metaforge.parser.SqlParser
import org.devlive.metaforge.parser.ParserResponse

class TrinoSqlParser : SqlParser {
    override fun parse(sql: String): ParserResponse {
        return TrinoHelper.parse(sql)
    }
}
