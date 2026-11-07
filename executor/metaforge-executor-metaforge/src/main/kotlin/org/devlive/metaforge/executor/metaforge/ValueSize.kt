package org.devlive.metaforge.executor.metaforge

import java.math.BigDecimal
import java.math.BigInteger
import java.time.temporal.Temporal

/**
 * 估算一个 JDBC 值的字节数，用于字节吞吐指标。只求量级正确、开销低：字符串按 UTF-8 计，
 * 数字按其定长类型计，未知类型按 toString() 长度计。
 *
 * Cheap byte-size estimate of a JDBC value for throughput metrics: strings as UTF-8, numbers by
 * their fixed width, anything else by its toString() length.
 */
object ValueSize
{
    @JvmStatic
    fun of(value: Any?): Long = when (value)
    {
        null -> 0L
        is String -> utf8(value)
        is ByteArray -> value.size.toLong()
        is Boolean, is Byte -> 1L
        is Short -> 2L
        is Int, is Float -> 4L
        is Long, is Double -> 8L
        is BigDecimal -> value.unscaledValue().bitLength() / 8L + 1L + 4L
        is BigInteger -> value.bitLength() / 8L + 1L
        is java.util.Date, is Temporal -> 8L
        is CharSequence -> utf8(value)
        else -> value.toString().length.toLong()
    }

    @JvmStatic
    fun ofRow(row: List<Any?>): Long
    {
        var total = 0L
        for (value in row)
        {
            total += of(value)
        }
        return total
    }

    private fun utf8(text: CharSequence): Long
    {
        var bytes = 0L
        var i = 0
        while (i < text.length)
        {
            val c = text[i]
            bytes += when
            {
                c.code < 0x80 -> 1
                c.code < 0x800 -> 2
                Character.isHighSurrogate(c) -> { i ++; 4 }
                else -> 3
            }
            i ++
        }
        return bytes
    }
}
