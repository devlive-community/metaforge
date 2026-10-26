package org.devlive.metaforge.parser

interface SqlParser {
    fun name(): String {
        return this.javaClass
                .simpleName
                .removeSuffix("SqlParser")
    }

    fun description(): String {
        return "Integrate ${name()} sql parser"
    }

    /**
     * Parses the given SQL string and returns a ParserInfo object.
     *
     * @param sql the SQL string to parse
     * @return    the ParserInfo object containing the parsed information
     */
    fun parse(sql: String): ParserResponse
}
