package org.devlive.metaforge.parser.trino

import com.google.inject.multibindings.Multibinder
import org.devlive.metaforge.parser.SqlParser
import org.devlive.metaforge.parser.ParserModule

class TrinoParserModule : ParserModule() {
    override fun configure() {
        Multibinder.newSetBinder(binder(), SqlParser::class.java)
                .addBinding()
                .to(TrinoSqlParser::class.java)
    }
}
