package org.devlive.metaforge.parser

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.IntStream
import org.antlr.v4.runtime.misc.Interval

class CaseInsensitiveStream : CharStream {
    private val stream: CharStream

    constructor(stream: CharStream) {
        this.stream = stream
    }

    override fun consume() {
        stream.consume()
    }

    override fun LA(p0: Int): Int {
        return when (val result = stream.LA(p0)) {
            0, IntStream.EOF -> result
            else -> Character.toUpperCase(result)
        }
    }

    override fun mark(): Int {
        return stream.mark()
    }

    override fun release(p0: Int) {
        stream.release(p0)
    }

    override fun index(): Int {
        return stream.index()
    }

    override fun seek(p0: Int) {
        stream.seek(p0)
    }

    override fun size(): Int {
        return stream.size()
    }

    override fun getSourceName(): String {
        return stream.getSourceName()
    }

    override fun getText(p0: Interval?): String {
        return stream.getText(p0)
    }
}
