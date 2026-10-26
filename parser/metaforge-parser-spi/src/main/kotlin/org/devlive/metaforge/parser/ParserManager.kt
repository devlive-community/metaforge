package org.devlive.metaforge.parser

import com.google.inject.AbstractModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.util.*

class ParserManager : AbstractModule() {
    private val log: Logger = getLogger(ParserManager::class.java)
    private var externalModules: Iterable<ParserModule> = ServiceLoader.load(ParserModule::class.java)

    override fun configure() {
        log.info("========== Loading parsers start ==========")
        externalModules.forEach { module ->
            log.info("Installing parser [ {} ] join time [ {} ]", module.name(), System.currentTimeMillis())
            install(module)
        }
        log.info("========== Loading parsers end ==========")
    }
}
