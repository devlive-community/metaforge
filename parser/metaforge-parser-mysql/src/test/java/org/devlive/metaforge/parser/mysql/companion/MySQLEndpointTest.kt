package org.devlive.metaforge.parser.mysql.companion

import org.devlive.metaforge.parser.mysql.companion.ast.ParserOptions
import org.junit.Test

class MySQLEndpointTest
{
    @Test
    fun test()
    {
        val sql = "SELECT name AS name, age AS age, summer + 1 AS summer, SUM(summer) FROM a_table_name LIMIt 100"
        MySQLEndpoint().createStatement(sql, ParserOptions.DecimalLiteralTreatment.AS_DECIMAL)
    }

    @Test
    fun test1()
    {
        val sql = "select count(`id`), query_mode\n" +
                "from metaforge.metaforge_source_query\n" +
                "group by query_mode\n" +
                "LIMIT 10000"
        MySQLEndpoint().createStatement(sql, ParserOptions.DecimalLiteralTreatment.AS_DOUBLE)
    }
}
