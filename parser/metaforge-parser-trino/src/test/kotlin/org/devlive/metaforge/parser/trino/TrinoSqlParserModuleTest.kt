package org.devlive.metaforge.parser.trino

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import org.devlive.metaforge.parser.SqlParser
import org.devlive.metaforge.parser.ParserManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class TrinoSqlParserModuleTest {
    private val log: Logger = LoggerFactory.getLogger(TrinoSqlParserModuleTest::class.java)
    private val injector: Injector = Guice.createInjector(ParserManager())

    @Test
    fun test() {
        val optional: Optional<SqlParser> = injector.getInstance(Key.get(object : TypeLiteral<Set<SqlParser>>() {}))
                .stream()
                .findFirst()

        optional.ifPresent {
            log.info("Module name: [ {} ]", it.name())
            assertEquals(it.name(), "Trino")
        }
    }
}
