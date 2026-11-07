package org.devlive.metaforge.executor.metaforge

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.configure.TotalSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * 一次未完成任务的断点。分片沿用当时的规划，已完成的跳过，未完成的从水位之后继续。
 * The checkpoint of an unfinished run: the planned splits, which are done, and each one's watermark.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
data class SyncCheckpoint(
    var query: String = "",
    var column: String? = null,
    var rowsWritten: Long = 0,
    var rowsSkipped: Long = 0,
    var bytesWritten: Long = 0,
    var elapsedMs: Long = 0,
    var totalRows: Long = -1,
    var totalSource: TotalSource = TotalSource.UNKNOWN,
    var splits: MutableList<SplitCheckpoint> = mutableListOf()
)
{
    fun toJson(): String = JSON.writeValueAsString(this)

    companion object
    {
        private val JSON = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        @JvmStatic
        fun fromJson(raw: String): SyncCheckpoint = JSON.readValue(raw, SyncCheckpoint::class.java)
    }
}

data class SplitCheckpoint(
    var id: Int = 0,
    var query: String = "",
    var range: String = "",
    var nullOnly: Boolean = false,
    var done: Boolean = false,
    var watermark: String? = null,
    var rowsWritten: Long = 0
)

/**
 * 把分片键的值写成 SQL 字面量，用于续传条件 `key > 水位`。值来自源端查询结果，字符串按 SQL 规则转义。
 * Renders a split key value as a SQL literal for the resume condition; strings are escaped.
 */
internal object KeyLiteral
{
    private val SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun of(value: Any?): String? = when (value)
    {
        null -> null
        is BigDecimal -> value.toPlainString()
        is Number -> value.toString()
        is java.sql.Timestamp -> time(value.toLocalDateTime())
        is java.sql.Date -> "'$value'"
        is LocalDate -> "'$value'"
        is LocalDateTime -> time(value)
        is OffsetDateTime -> time(value.toLocalDateTime())
        is ZonedDateTime -> time(value.toLocalDateTime())
        else -> "'" + value.toString().replace("'", "''") + "'"
    }

    /** 没有小数秒时不带小数，有些引擎的秒级类型不接受 / no fraction unless needed: some engines' second-precision types reject it */
    private fun time(value: LocalDateTime): String
    {
        val seconds = value.format(SECONDS)
        if (value.nano == 0)
        {
            return "'$seconds'"
        }
        val fraction = String.format("%09d", value.nano).trimEnd('0')
        return "'$seconds.$fraction'"
    }
}
