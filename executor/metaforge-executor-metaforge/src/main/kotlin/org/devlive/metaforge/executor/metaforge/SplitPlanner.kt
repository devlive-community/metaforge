package org.devlive.metaforge.executor.metaforge

import com.fasterxml.jackson.databind.JsonNode
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.Response
import org.devlive.metaforge.spi.model.RuntimeConfigure
import org.slf4j.Logger
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** 一个分片：一条可独立执行的查询，以及它范围的可读描述 / one independently runnable query and its range */
class Split(val id: Int, val query: String, val range: String)

/** 分片规划结果。strategy 为实际采用的方式 / the plan; strategy is what was actually applied */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
class SplitPlan(val strategy: String, val column: String?, val splits: List<Split>)
{
    companion object
    {
        const val NONE = "NONE"
        const val AUTO = "AUTO"
        const val RANGE = "RANGE"
    }
}

/**
 * 把一条查询按分片列的取值范围切成多条互不重叠、合起来覆盖全部行的查询：
 *
 *     col < b1 / b1 <= col < b2 / ... / col >= bn / col IS NULL
 *
 * - 首尾区间开放，MIN/MAX 之后新增的行不会丢；NULL 单独一片，否则这些行不属于任何区间
 * - 只切整数与日期时间列；其他类型、读不出边界、方言不接受字面量时都退回单分片，只记日志，不让同步失败
 * - 带 LIMIT 等的查询每次执行结果不一定相同，分别执行后合起来可能丢行或重复；GROUP BY 等每片都要全量
 *   计算，切分没有好处。这些都退回单分片
 * - 分片列会拼进 SQL，只接受普通标识符，防止注入；因方言引号不同不加引号，大小写敏感列会在探测时失败并退回
 *
 * 注意：各分片是独立查询，不共享快照；同步期间源表变更时，结果不是同一时刻的一致视图。
 *
 * Splits a query by value ranges of a column into non-overlapping queries that together cover every
 * row, with open-ended outer ranges and a separate NULL split. Anything that cannot be split safely
 * falls back to one split. Splits are separate queries and do not share a snapshot.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP2", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
class SplitPlanner(
    private val plugin: PluginService,
    private val configure: RuntimeConfigure,
    private val taskLog: Logger
)
{
    fun plan(query: String, strategy: String, column: String?, concurrency: Int, splitCount: Int): SplitPlan
    {
        val whole = SplitPlan(SplitPlan.NONE, null, listOf(Split(1, query, "ALL")))
        val wanted = strategy.trim().uppercase()
        if (wanted != SplitPlan.RANGE && !(wanted == SplitPlan.AUTO && concurrency > 1))
        {
            return whole
        }
        val name = column?.trim().orEmpty()
        if (name.isEmpty())
        {
            taskLog.warn("Split skipped: no split column and the dataset has no primary key")
            return whole
        }
        if (!IDENTIFIER.matches(name))
        {
            taskLog.warn("Split skipped: split column '{}' is not a plain identifier", name)
            return whole
        }
        val trimmed = query.trim().trimEnd(';').trim()
        val blocker = unsplittableReason(trimmed)
        if (blocker != null)
        {
            taskLog.warn("Split skipped: the query cannot be split safely ({})", blocker)
            return whole
        }
        val count = (if (splitCount > 0) splitCount else concurrency * SPLITS_PER_CHANNEL).coerceIn(1, MAX_SPLITS)
        return try
        {
            planRange(trimmed, name, count) ?: whole
        }
        catch (ex: Exception)
        {
            taskLog.warn("Split skipped: planning failed: {}", ex.message)
            whole
        }
    }

    private fun planRange(query: String, column: String, count: Int): SplitPlan?
    {
        val derived = "FROM ($query) $DERIVED_ALIAS"
        val bounds = firstRow(plugin.execute(configure, "SELECT MIN($column) AS min_value, MAX($column) AS max_value $derived"))
        if (bounds == null || bounds.size < 2)
        {
            taskLog.warn("Split skipped: could not read the bounds of '{}'", column)
            return null
        }
        val low = bounds[0]
        val high = bounds[1]
        if (low == null || high == null)
        {
            taskLog.warn("Split skipped: '{}' has no values", column)
            return null
        }
        val cuts = integerCuts(low, high, count) ?: timeCuts(low, high, count)
        if (cuts == null)
        {
            taskLog.warn("Split skipped: '{}' is neither an integer nor a date ({} .. {})", column, low, high)
            return null
        }
        // 用一条不返回数据的查询确认方言接受这个列名和字面量 / a no-rows probe confirms the dialect accepts the column and literal
        if (cuts.isNotEmpty())
        {
            val probe = plugin.execute(configure, "SELECT 1 $derived WHERE $column < ${cuts[0]} AND 1 = 0")
            if (probe.isSuccessful != true)
            {
                taskLog.warn("Split skipped: the source rejected the split condition: {}", probe.message)
                return null
            }
        }
        val splits = ArrayList<Split>(cuts.size + 2)
        val select = "SELECT * $derived WHERE"
        if (cuts.isEmpty())
        {
            splits.add(Split(1, "$select $column IS NOT NULL", "IS NOT NULL"))
        }
        else
        {
            splits.add(Split(1, "$select $column < ${cuts[0]}", "< ${cuts[0]}"))
            for (i in 1 until cuts.size)
            {
                splits.add(Split(i + 1, "$select $column >= ${cuts[i - 1]} AND $column < ${cuts[i]}", "[${cuts[i - 1]}, ${cuts[i]})"))
            }
            splits.add(Split(cuts.size + 1, "$select $column >= ${cuts.last()}", ">= ${cuts.last()}"))
        }
        splits.add(Split(splits.size + 1, "$select $column IS NULL", "IS NULL"))
        taskLog.info("Planned {} splits on '{}' ({} .. {})", splits.size, column, low, high)
        return SplitPlan(SplitPlan.RANGE, column, splits)
    }

    /** 整数列的切点，非整数返回 null / cut points of an integer column, null when it is not one */
    private fun integerCuts(low: Any, high: Any, count: Int): List<String>?
    {
        val min = asInteger(low) ?: return null
        val max = asInteger(high) ?: return null
        return evenCuts(min, max, count).map { it.toString() }
    }

    /** 日期时间列的切点：取值只有日期时按天切，字面量格式与取值一致 / date or datetime cuts in the value's own format */
    private fun timeCuts(low: Any, high: Any, count: Int): List<String>?
    {
        val min = asTime(low) ?: return null
        val max = asTime(high) ?: return null
        if (min.dateOnly && max.dateOnly)
        {
            return evenCuts(BigInteger.valueOf(min.value.toLocalDate().toEpochDay()), BigInteger.valueOf(max.value.toLocalDate().toEpochDay()), count)
                .map { "'" + LocalDate.ofEpochDay(it.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE) + "'" }
        }
        return evenCuts(BigInteger.valueOf(min.value.toEpochSecond(ZoneOffset.UTC)), BigInteger.valueOf(max.value.toEpochSecond(ZoneOffset.UTC)), count)
            .map { "'" + LocalDateTime.ofEpochSecond(it.toLong(), 0, ZoneOffset.UTC).format(DATETIME) + "'" }
    }

    /** 把 [min, max] 均分成最多 count 段，返回段之间的切点 / the cut points that divide [min, max] into at most count parts */
    private fun evenCuts(min: BigInteger, max: BigInteger, count: Int): List<BigInteger>
    {
        val span = max.subtract(min).add(BigInteger.ONE)
        val parts = span.min(BigInteger.valueOf(count.toLong())).toInt()
        val cuts = ArrayList<BigInteger>(parts)
        for (i in 1 until parts)
        {
            cuts.add(min.add(span.multiply(BigInteger.valueOf(i.toLong())).divide(BigInteger.valueOf(parts.toLong()))))
        }
        return cuts
    }

    private fun asInteger(value: Any): BigInteger? = when (value)
    {
        is BigInteger -> value
        is Byte, is Short, is Int, is Long -> BigInteger.valueOf((value as Number).toLong())
        is BigDecimal -> wholeNumber(value)
        is String -> if (INTEGER.matches(value.trim())) BigInteger(value.trim()) else null
        else -> null
    }

    private fun wholeNumber(value: BigDecimal): BigInteger? =
        if (value.stripTrailingZeros().scale() <= 0) value.toBigInteger() else null

    private class TimeValue(val value: LocalDateTime, val dateOnly: Boolean)

    private fun asTime(value: Any): TimeValue?
    {
        val match = TIME.find(value.toString().trim()) ?: return null
        return try
        {
            val date = LocalDate.parse(match.groupValues[1])
            val time = match.groupValues[2]
            if (time.isEmpty())
            {
                TimeValue(date.atStartOfDay(), true)
            }
            else
            {
                TimeValue(date.atTime(LocalTime.parse(time)), false)
            }
        }
        catch (ex: java.time.DateTimeException)
        {
            null
        }
    }

    /** 查询结果的第一行，按列位置取值；格式化输出可能是 JSON 对象或列表 / first row by position, from JSON objects or lists */
    private fun firstRow(response: Response): List<Any?>?
    {
        if (response.isSuccessful != true)
        {
            taskLog.warn("Split bounds query failed: {}", response.message)
            return null
        }
        return when (val first = response.columns?.firstOrNull())
        {
            is JsonNode -> first.elements().asSequence().map { plain(it) }.toList()
            is List<*> -> first.map { plain(it) }
            is Map<*, *> -> first.values.map { plain(it) }
            else -> null
        }
    }

    private fun plain(value: Any?): Any? = when (value)
    {
        is JsonNode -> when
        {
            value.isNull || value.isMissingNode -> null
            value.isIntegralNumber -> value.bigIntegerValue()
            value.isNumber -> value.decimalValue()
            else -> value.asText()
        }
        else -> value
    }

    companion object
    {
        private const val DERIVED_ALIAS = "metaforge_split_t"
        private const val SPLITS_PER_CHANNEL = 4
        private const val MAX_SPLITS = 256

        private val IDENTIFIER = Regex("^[A-Za-z_][A-Za-z0-9_$]*$")
        private val INTEGER = Regex("^-?\\d+$")
        private val TIME = Regex("^(\\d{4}-\\d{2}-\\d{2})(?:[ T](\\d{2}:\\d{2}:\\d{2}))?")
        private val DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        // 字符串、带引号的标识符与注释里的内容不算关键字 / text inside strings, quoted identifiers and comments does not count
        private val NOISE = Regex("'(?:[^']|'')*'|\"[^\"]*\"|`[^`]*`|--[^\\n]*|/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
        private val BLOCKERS = Regex(
            "\\b(LIMIT|OFFSET|FETCH|TOP|ROWNUM|SAMPLE|GROUP\\s+BY|HAVING|DISTINCT|UNION|INTERSECT|EXCEPT|MINUS|OVER)\\b",
            RegexOption.IGNORE_CASE
        )

        /** 不能安全切分的原因，能切分返回 null / why a query cannot be split safely, null when it can */
        @JvmStatic
        fun unsplittableReason(query: String): String?
        {
            val text = NOISE.replace(query, " ").trim()
            if (!text.startsWith("select", ignoreCase = true))
            {
                return "not a plain SELECT"
            }
            if (text.contains(';'))
            {
                return "multiple statements"
            }
            return BLOCKERS.find(text)?.let { it.value.uppercase().replace(Regex("\\s+"), " ") }
        }
    }
}
