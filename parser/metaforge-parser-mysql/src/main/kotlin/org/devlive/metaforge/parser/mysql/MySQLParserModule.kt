package org.devlive.metaforge.parser.mysql

import com.google.inject.multibindings.Multibinder
import org.devlive.metaforge.parser.ParserModule
import org.devlive.metaforge.parser.SqlParser

class MySQLParserModule : ParserModule() {
    override fun configure() {
        Multibinder.newSetBinder(binder(), SqlParser::class.java)
                .addBinding()
                .to(MySQLSqlParser::class.java)
    }
}
