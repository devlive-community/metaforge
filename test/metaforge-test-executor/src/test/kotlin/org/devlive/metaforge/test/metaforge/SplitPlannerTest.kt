package org.devlive.metaforge.test.metaforge

import com.fasterxml.jackson.databind.ObjectMapper
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.metaforge.RateLimiter
import org.devlive.metaforge.executor.metaforge.SplitPlan
import org.devlive.metaforge.executor.metaforge.SplitPlanner
import org.devlive.metaforge.spi.model.Response
import org.devlive.metaforge.spi.model.RuntimeConfigure
import org.devlive.metaforge.test.metaforge.support.FakePluginService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.slf4j.LoggerFactory

@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
class SplitPlannerTest
{
    private val mapper = ObjectMapper()
    private val log = LoggerFactory.getLogger(SplitPlannerTest::class.java)
    private val query = "SELECT id FROM t"

    /** 源端返回给定的 MIN/MAX（JSON 对象形式），其余查询成功 / a source answering MIN/MAX with the given values */
    private fun planner(low: Any?, high: Any?, probeOk: Boolean = true, sql: MutableList<String> = ArrayList()): SplitPlanner
    {
        val plugin = FakePluginService(streaming = true, executeHandler = { content ->
            sql.add(content)
            when
            {
                content.startsWith("SELECT MIN(") ->
                {
                    val node = mapper.createObjectNode()
                    node.set<com.fasterxml.jackson.databind.JsonNode>("MIN_VALUE", mapper.valueToTree(low))
                    node.set<com.fasterxml.jackson.databind.JsonNode>("MAX_VALUE", mapper.valueToTree(high))
                    Response.builder().isSuccessful(true).columns(listOf<Any>(node)).build()
                }
                content.startsWith("SELECT 1 ") && !probeOk -> Response.builder().isSuccessful(false).message("bad literal").build()
                else -> Response.builder().isSuccessful(true).columns(emptyList<Any>()).build()
            }
        })
        return SplitPlanner(plugin, RuntimeConfigure(), log)
    }

    private fun conditions(plan: SplitPlan): List<String> = plan.splits.map { it.query.substringAfter(" WHERE ") }

    @Test
    fun rangesPartitionTheValuesWithOpenEndsAndANullSplit()
    {
        val plan = planner(1, 100).plan(query, "RANGE", "id", 2, 4)

        assertEquals(SplitPlan.RANGE, plan.strategy)
        assertEquals("id", plan.column)
        assertEquals(
            listOf("id < 26", "id >= 26 AND id < 51", "id >= 51 AND id < 76", "id >= 76", "id IS NULL"),
            conditions(plan)
        )
        assertEquals(listOf(1, 2, 3, 4, 5), plan.splits.map { it.id })
        assertTrue(plan.splits.all { it.query.startsWith("SELECT * FROM (SELECT id FROM t) metaforge_split_t WHERE ") })
    }

    @Test
    fun theDefaultSplitCountIsFourPerChannelAndNarrowRangesGetFewerSplits()
    {
        assertEquals(13, planner(1, 1_000_000).plan(query, "AUTO", "id", 3, 0).splits.size)
        // 只有 3 个不同取值：3 段 + NULL / only three distinct values
        assertEquals(listOf("id < 8", "id >= 8 AND id < 9", "id >= 9", "id IS NULL"), conditions(planner(7, 9).plan(query, "RANGE", "id", 4, 0)))
        // 只有 1 个取值 / a single value
        assertEquals(listOf("id IS NOT NULL", "id IS NULL"), conditions(planner(5, 5).plan(query, "RANGE", "id", 4, 0)))
    }

    @Test
    fun negativeAndHugeIntegersAndDecimalsWithoutFractionSplit()
    {
        assertEquals(listOf("id < 0", "id >= 0", "id IS NULL"), conditions(planner(-5, 4).plan(query, "RANGE", "id", 1, 2)))
        val huge = planner(java.math.BigInteger("18446744073709551000"), java.math.BigInteger("18446744073709551615")).plan(query, "RANGE", "id", 1, 2)
        assertEquals(SplitPlan.RANGE, huge.strategy)
        assertEquals("id < 18446744073709551308", conditions(huge)[0])
        assertEquals(SplitPlan.RANGE, planner(java.math.BigDecimal("10.00"), java.math.BigDecimal("20")).plan(query, "RANGE", "id", 1, 2).strategy)
        assertEquals(SplitPlan.RANGE, planner("10", "20").plan(query, "RANGE", "id", 1, 2).strategy)
    }

    @Test
    fun datesSplitByDayAndTimestampsBySecondInTheirOwnFormat()
    {
        assertEquals(
            listOf("id < '2026-01-03'", "id >= '2026-01-03'", "id IS NULL"),
            conditions(planner("2026-01-01", "2026-01-04").plan(query, "RANGE", "id", 1, 2))
        )
        assertEquals(
            listOf("id < '2026-01-01 00:00:30'", "id >= '2026-01-01 00:00:30'", "id IS NULL"),
            conditions(planner("2026-01-01 00:00:00.0", "2026-01-01 00:00:59").plan(query, "RANGE", "id", 1, 2))
        )
    }

    private fun assertWhole(plan: SplitPlan)
    {
        assertEquals(SplitPlan.NONE, plan.strategy)
        assertNull(plan.column)
        assertEquals(1, plan.splits.size)
        assertEquals(query, plan.splits[0].query)
    }

    @Test
    fun strategyAndColumnDecideWhetherToPlan()
    {
        val sql = ArrayList<String>()
        val source = planner(1, 100, sql = sql)
        assertWhole(source.plan(query, "NONE", "id", 4, 0))
        assertWhole(source.plan(query, "AUTO", "id", 1, 0))
        assertWhole(source.plan(query, "RANGE", "", 4, 0))
        assertWhole(source.plan(query, "RANGE", null, 4, 0))
        assertTrue("no query was sent to the source", sql.isEmpty())
        assertEquals(SplitPlan.RANGE, source.plan(query, "range", "id", 1, 2).strategy)
    }

    @Test
    fun aColumnThatIsNotAPlainIdentifierNeverReachesTheSource()
    {
        val sql = ArrayList<String>()
        val source = planner(1, 100, sql = sql)
        for (column in listOf("id; DROP TABLE t", "id) --", "a.b", "1id", "a b", "\"id\""))
        {
            assertWhole(source.plan(query, "RANGE", column, 4, 0))
        }
        assertTrue(sql.isEmpty())
    }

    @Test
    fun unusableBoundsFallBackToOneQuery()
    {
        assertWhole(planner(null, null).plan(query, "RANGE", "id", 4, 0))
        assertWhole(planner("abc", "xyz").plan(query, "RANGE", "id", 4, 0))
        assertWhole(planner(1.5, 9.5).plan(query, "RANGE", "id", 4, 0))
        assertWhole(planner(1, 100, probeOk = false).plan(query, "RANGE", "id", 4, 0))
        val failing = FakePluginService(streaming = true, executeHandler = { Response.builder().isSuccessful(false).message("no such column").build() })
        assertWhole(SplitPlanner(failing, RuntimeConfigure(), log).plan(query, "RANGE", "id", 4, 0))
        val throwing = FakePluginService(streaming = true, executeHandler = { throw IllegalStateException("connection lost") })
        assertWhole(SplitPlanner(throwing, RuntimeConfigure(), log).plan(query, "RANGE", "id", 4, 0))
    }

    @Test
    fun unsafeQueriesAreDetectedOutsideStringsAndComments()
    {
        for (unsafe in listOf(
            "SELECT id FROM t LIMIT 10",
            "select id from t limit 10 offset 5",
            "SELECT id FROM t FETCH FIRST 5 ROWS ONLY",
            "SELECT TOP 5 id FROM t",
            "SELECT DISTINCT id FROM t",
            "SELECT id, count(*) c FROM t GROUP  BY id",
            "SELECT id FROM a UNION ALL SELECT id FROM b",
            "SELECT id, row_number() OVER (ORDER BY id) n FROM t",
            "WITH x AS (SELECT 1 id) SELECT id FROM x",
            "SELECT id FROM t; DELETE FROM t",
            "DELETE FROM t"
        ))
        {
            assertNotNull(unsafe, SplitPlanner.unsplittableReason(unsafe))
        }
        for (safe in listOf(
            "SELECT id, 'limit 5' AS label FROM t",
            "SELECT id, \"group by\" FROM t",
            "SELECT id /* limit 5 */ FROM t -- union\nWHERE id > 0",
            "SELECT id, limits, topic, overall FROM t JOIN u ON t.id = u.id WHERE id > 0 ORDER BY id"
        ))
        {
            assertNull(safe, SplitPlanner.unsplittableReason(safe))
        }
    }

    @Test
    fun theRateLimiterChargesLaterCallersAndGivesNoCreditForIdleTime()
    {
        var now = 0L
        val second = 1_000_000_000L
        val limiter = RateLimiter(100, 0) { now }
        assertTrue(limiter.enabled)
        assertEquals(0L, limiter.reserve(50, 0))
        assertEquals(second / 2, limiter.reserve(50, 0))
        assertEquals(second, limiter.reserve(50, 0))
        now += 10 * second
        assertEquals("idle time earns no burst", 0L, limiter.reserve(100, 0))
        assertEquals(second, limiter.reserve(1, 0))

        val bytes = RateLimiter(0, 1000) { now }
        assertEquals(0L, bytes.reserve(1_000_000, 500))
        assertEquals(second / 2, bytes.reserve(1, 1))

        val both = RateLimiter(100, 1000) { now }
        both.reserve(10, 2000)
        assertEquals("the tighter limit decides", 2 * second, both.reserve(1, 1))

        val none = RateLimiter(0, 0) { now }
        assertEquals(false, none.enabled)
        assertEquals(0L, none.reserve(1_000_000, 1_000_000))
    }

    @Test
    fun rateLimiterChunksAreAboutATenthOfASecondAcrossChannels()
    {
        assertEquals(25, RateLimiter(1000, 0).chunkRows(4))
        assertEquals(1, RateLimiter(5, 0).chunkRows(4))
        assertEquals(256, RateLimiter(1_000_000, 0).chunkRows(1))
        assertEquals(256, RateLimiter(0, 1000).chunkRows(4))
    }

    @Test
    fun waitingStopsAsSoonAsTheTaskStops()
    {
        val limiter = RateLimiter(1, 0)
        limiter.reserve(1000, 0)
        val started = System.nanoTime()
        limiter.acquire(1, 0) { true }
        assertTrue((System.nanoTime() - started) < 1_000_000_000L)
    }
}
