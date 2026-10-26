package org.devlive.metaforge.parser

import com.google.inject.multibindings.Multibinder

class TestParserModule : ParserModule() {
    override fun configure() {
        Multibinder.newSetBinder(this.binder(), SqlParser::class.java)
                .addBinding().to(TestSqlParser::class.java)
    }
}
