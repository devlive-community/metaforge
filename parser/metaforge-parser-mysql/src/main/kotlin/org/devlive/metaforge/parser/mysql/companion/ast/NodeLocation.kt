package org.devlive.metaforge.parser.mysql.companion.ast

import com.google.common.base.Preconditions.checkArgument
import java.util.*

data class NodeLocation(val line: Int, val charPositionInLine: Int) {
    init {
        checkArgument(line >= 1, "line must be at least one, got: $line")
        checkArgument(charPositionInLine >= 0, "charPositionInLine must be at least zero, got: $charPositionInLine")
    }

    fun getLineNumber(): Int {
        return line
    }

    fun getColumnNumber(): Int {
        // Since the index is 0, perform +1 operation here
        return charPositionInLine + 1
    }

    override fun toString(): String {
        return "$line:$charPositionInLine"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as NodeLocation
        return line == that.line && charPositionInLine == that.charPositionInLine
    }

    override fun hashCode(): Int {
        return Objects.hash(line, charPositionInLine)
    }
}
