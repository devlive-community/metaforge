package org.devlive.metaforge.parser

import com.google.inject.AbstractModule

abstract class ParserModule : AbstractModule() {
    fun name(): String = this.javaClass
            .simpleName
            .removeSuffix("Parser")
            .removeSuffix("Module")
}
