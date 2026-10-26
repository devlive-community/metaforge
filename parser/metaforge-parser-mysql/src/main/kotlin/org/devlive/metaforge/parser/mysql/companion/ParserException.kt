package org.devlive.metaforge.parser.mysql.companion

import org.devlive.metaforge.parser.mysql.companion.ast.NodeLocation
import org.antlr.v4.runtime.RecognitionException

class ParserException : RuntimeException {
    private val line: Int
    private val positionInLine: Int

    constructor(message: String, cause: RecognitionException?, line: Int, positionInLine: Int) : super(message, cause) {
        this.line = line
        this.positionInLine = positionInLine
    }

    constructor(message: String) : this(message, null, 1, 0)

    constructor(message: String, location: NodeLocation) : this(message, null, location.getLineNumber(), location.getColumnNumber())

    private fun getLineNumber(): Int {
        return line
    }

    private fun getColumnNumber(): Int {
        // Since the index is 0, a +1 operation is required here
        return positionInLine + 1
    }

    private fun getErrorMessage(): String {
        return super.message.orEmpty()
    }

    override fun getLocalizedMessage(): String {
        return String.format("line %s:%s: %s", getLineNumber(), getColumnNumber(), getErrorMessage())
    }
}
