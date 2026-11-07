package org.devlive.metaforge.test.metaforge

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.common.RunState
import org.devlive.metaforge.executor.configure.ExecutorConfigure
import org.devlive.metaforge.executor.configure.ExecutorMetricsListener
import org.devlive.metaforge.executor.configure.ExecutorRequest
import org.devlive.metaforge.executor.configure.ExecutorResponse
import org.devlive.metaforge.executor.configure.OriginColumn
import org.devlive.metaforge.executor.metaforge.MetaForgeExecutorService
import org.devlive.metaforge.spi.model.RuntimeConfigure
import org.devlive.metaforge.test.metaforge.support.JdbcPluginService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.AfterClass
import org.junit.Assume
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * 真实数据库端到端测试：用 PostgreSQL 容器作为【源】和【目标】，
 * 通过 [JdbcPluginService]（真实 JDBC）驱动 [MetaForgeExecutorService] 完成一次真正的 source -> target 同步，
 * 然后直接查目标表核对落库数据。覆盖：流式搬运 + 投影/改名 + NULL/数值类型、preCount 总数、真实 stop 取消。
 *
 * 需要本机可用的 Docker。容器由 @BeforeClass/@AfterClass 管理，整个测试类只起停一次；
 * Docker 不可用时优雅跳过。
 */
@SuppressFBWarnings(value = ["OBL_UNSATISFIED_OBLIGATION", "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
class MetaForgeExecutorPostgresE2ETest
{
    companion object
    {
        private var container: PostgreSQLContainer<Nothing>? = null

        @BeforeClass
        @JvmStatic
        fun startContainer()
        {
            // Docker Desktop（Engine 29 / API 1.55）会以 HTTP 400 拒绝 docker-java 默认过低的 API 版本。
            // 钉一个双方都支持的版本（守护进程最低 1.40）。尊重外部已显式指定的值。
            if (System.getProperty("api.version") == null && System.getenv("DOCKER_API_VERSION") == null)
            {
                System.setProperty("api.version", "1.41")
            }
            // Docker 不可用时优雅跳过（例如未装 Docker 的 CI），而不是让整个类失败。
            Assume.assumeTrue(
                "Docker is not available; skipping PostgreSQL E2E tests",
                DockerClientFactory.instance().isDockerAvailable
            )
            container = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:16-alpine"))
                .apply { start() }
        }

        @AfterClass
        @JvmStatic
        fun stopContainer()
        {
            container?.stop()
            container = null
        }

        private fun container(): PostgreSQLContainer<Nothing> =
            container ?: throw IllegalStateException("PostgreSQL container is not started")
    }

    private lateinit var service: JdbcPluginService

    @Before
    fun setup()
    {
        service = JdbcPluginService(container().jdbcUrl, container().username, container().password)
        exec("DROP TABLE IF EXISTS source_t")
        exec("DROP TABLE IF EXISTS target_t")
        exec("CREATE TABLE source_t (id int, name text, amount numeric)")
        exec("CREATE TABLE target_t (uid int, full_name text, amt numeric)")
    }

    @Test
    fun streamingCopiesRealDataWithProjectionAndNull()
    {
        exec("INSERT INTO source_t (id, name, amount) VALUES (1,'alice',10.5),(2,NULL,20),(3,'carol',NULL)")

        val request = buildRequest(
            query = "SELECT id, name, amount FROM source_t ORDER BY id",
            targetTable = "target_t",
            columns = linkedSetOf(
                OriginColumn("uid", "id"),
                OriginColumn("full_name", "name"),
                OriginColumn("amt", "amount")
            )
        )

        val response = MetaForgeExecutorService().start(request)

        assertTrue(response.successful)
        assertEquals(RunState.SUCCESS, response.state)
        assertEquals(3, response.count)

        val rows = query("SELECT uid, full_name, amt FROM target_t ORDER BY uid")
        assertEquals(
            listOf(
                listOf("1", "alice", "10.5"),
                listOf("2", null, "20"),
                listOf("3", "carol", null)
            ),
            rows
        )
    }

    @Test
    fun preCountReportsTotalFromRealDb()
    {
        exec("INSERT INTO source_t (id, name, amount) SELECT g, 'n' || g, g FROM generate_series(1, 250) g")

        val progress = CopyOnWriteArrayList<Pair<Long, Long>>()
        val request = buildRequest(
            query = "SELECT id, name, amount FROM source_t",
            targetTable = "target_t",
            columns = linkedSetOf(
                OriginColumn("uid", "id"),
                OriginColumn("full_name", "name"),
                OriginColumn("amt", "amount")
            )
        ).apply {
            options = mapOf("preCount" to "true")
            metricsListener = ExecutorMetricsListener { m -> progress.add(m.rowsWritten to m.totalRows) }
        }

        val response = MetaForgeExecutorService().start(request)

        assertTrue(response.successful)
        assertEquals(250, response.count)
        assertEquals(250L, count("target_t"))
        // 首个进度事件应是 pre-count 后上报的 (0, 250)
        assertEquals(0L to 250L, progress.first())
        assertEquals(250L, progress.last().second)
    }

    @Test
    fun stopCancelsRunningSyncOnRealDb()
    {
        // 用 generate_series 造一个足够大的慢结果集，跑起来后从另一线程 stop，验证真实 Statement.cancel 生效
        val taskName = "pg-stop"
        val request = buildRequest(
            query = "SELECT g AS id, 'x' AS name, g AS amount FROM generate_series(1, 3000000) g",
            targetTable = "target_t",
            columns = linkedSetOf(
                OriginColumn("uid", "id"),
                OriginColumn("full_name", "name"),
                OriginColumn("amt", "amount")
            ),
            taskName = taskName
        )

        val executor = MetaForgeExecutorService()
        val responseRef = AtomicReference<ExecutorResponse>()
        val worker = Thread { responseRef.set(executor.start(request)) }
        worker.start()

        // 等到目标表已经落了一些行，确保同步确实在跑，再发停止
        var waited = 0
        while (count("target_t") == 0L && waited < 20_000)
        {
            Thread.sleep(50)
            waited += 50
        }
        assertTrue("sync did not start writing in time", count("target_t") > 0L)

        val stopResponse = executor.stop(stopRequest(taskName))
        assertEquals(RunState.STOPPED, stopResponse.state)

        worker.join(60_000)
        val response = responseRef.get()
        assertNotNull("worker did not finish after stop", response)
        assertFalse(response.successful)
        assertEquals(RunState.STOPPED, response.state)
        // 被中途取消：落库行数应远小于 3,000,000
        assertTrue("expected partial rows, got ${count("target_t")}", count("target_t") < 3_000_000L)
    }

    // ---------------------------------------------------------------------
    // 分片并行与限流
    // ---------------------------------------------------------------------

    private fun parallelRequest(query: String, options: Map<String, String>, taskName: String = ""): ExecutorRequest =
        buildRequest(
            query = query,
            targetTable = "target_t",
            columns = linkedSetOf(
                OriginColumn("uid", "id"),
                OriginColumn("full_name", "name"),
                OriginColumn("amt", "amount")
            ),
            taskName = taskName
        ).apply { this.options = options }

    /** 目标表与源表逐行相同：没有丢行，也没有重复 / the target equals the source row for row: nothing lost, nothing duplicated */
    private fun assertCopiedExactlyOnce(expected: Long)
    {
        assertEquals(expected, count("target_t"))
        assertEquals(listOf(listOf("0")), query(
            "SELECT count(*) FROM (" +
                    "(SELECT id, name, amount FROM source_t EXCEPT ALL SELECT uid, full_name, amt FROM target_t) " +
                    "UNION ALL " +
                    "(SELECT uid, full_name, amt FROM target_t EXCEPT ALL SELECT id, name, amount FROM source_t)) diff"
        ))
    }

    @Test
    fun parallelChannelsCopyEveryRowExactlyOnceIncludingNullKeys()
    {
        // 键严重倾斜（大部分行集中在最小的区间），还有 NULL 键的行，这些行不属于任何范围
        exec("INSERT INTO source_t SELECT g, 'n' || g, g FROM generate_series(1, 20000) g")
        exec("INSERT INTO source_t SELECT 1000000 + g, 'far' || g, g FROM generate_series(1, 5) g")
        exec("INSERT INTO source_t SELECT NULL, 'null' || g, g FROM generate_series(1, 37) g")

        val response = MetaForgeExecutorService().start(parallelRequest(
            "SELECT id, name, amount FROM source_t",
            mapOf("concurrency" to "4", "splitStrategy" to "AUTO", "splitColumn" to "id", "batchSize" to "500")
        ))

        assertTrue(response.message, response.successful)
        assertEquals(20042, response.count)
        assertCopiedExactlyOnce(20042)
        val metrics = response.metrics!!
        assertEquals("RANGE", metrics.splitStrategy)
        assertEquals("id", metrics.splitColumn)
        assertEquals(4, metrics.channels)
        // 4 通道 × 每通道 4 个区间 + NULL 分片
        assertEquals(17, metrics.splits.size)
        assertTrue(metrics.splits.all { it.state == "DONE" })
        assertEquals(20042L, metrics.splits.sumOf { it.rowsWritten })
        assertEquals(20042L, metrics.splits.sumOf { it.rowsRead })
        assertEquals(37L, metrics.splits.single { it.range == "IS NULL" }.rowsWritten)
        assertEquals(20042L, metrics.rowsRead)
        assertEquals(20042L, metrics.rowsWritten)
        assertEquals(0, metrics.queueDepth)
        assertEquals(16, metrics.queueCapacity)
        assertTrue("several channels did work", metrics.splits.map { it.channel }.distinct().size > 1)
    }

    @Test
    fun dateAndTimestampColumnsSplitByTime()
    {
        exec("ALTER TABLE source_t ADD COLUMN day date, ADD COLUMN at timestamp")
        exec("INSERT INTO source_t SELECT g, 'n' || g, g, DATE '2026-01-01' + (g % 40), TIMESTAMP '2026-01-01 08:00:00' + g * INTERVAL '37 minute' FROM generate_series(1, 3000) g")
        exec("INSERT INTO source_t (id, name, amount) VALUES (3001, 'no-date', 1)")

        for (column in listOf("day", "at"))
        {
            exec("TRUNCATE target_t")
            val response = MetaForgeExecutorService().start(parallelRequest(
                "SELECT id, name, amount, day, at FROM source_t",
                mapOf("concurrency" to "3", "splitStrategy" to "RANGE", "splitColumn" to column, "batchSize" to "100")
            ))
            assertTrue(response.message, response.successful)
            assertEquals(column, response.metrics!!.splitColumn)
            assertEquals(13, response.metrics!!.splits.size)
            assertEquals(3001, response.count)
            assertEquals(1L, response.metrics!!.splits.single { it.range == "IS NULL" }.rowsWritten)
            assertCopiedExactlyOnce(3001)
        }
    }

    @Test
    fun queriesThatCannotBeSplitSafelyRunAsOneQuery()
    {
        exec("INSERT INTO source_t SELECT g, 'n' || g, g FROM generate_series(1, 500) g")
        val cases = listOf(
            // LIMIT：每片结果不确定 / LIMIT: each split's result is not deterministic
            mapOf("splitColumn" to "id") to "SELECT id, name, amount FROM source_t LIMIT 500",
            // 文本列不能按范围切分 / a text column cannot be split by range
            mapOf("splitColumn" to "name") to "SELECT id, name, amount FROM source_t",
            // 列不存在：探测失败 / the column does not exist
            mapOf("splitColumn" to "missing") to "SELECT id, name, amount FROM source_t",
            // 非标识符，防注入 / not an identifier, guards against injection
            mapOf("splitColumn" to "id) --") to "SELECT id, name, amount FROM source_t",
            // 没有分片列 / no split column at all
            mapOf("splitColumn" to "") to "SELECT id, name, amount FROM source_t"
        )
        for ((options, query) in cases)
        {
            exec("TRUNCATE target_t")
            val response = MetaForgeExecutorService().start(parallelRequest(
                query, options + mapOf("concurrency" to "4", "splitStrategy" to "RANGE", "batchSize" to "100")
            ))
            assertTrue(response.message, response.successful)
            assertEquals("fell back for $options", "NONE", response.metrics!!.splitStrategy)
            assertEquals(1, response.metrics!!.channels)
            assertEquals(1, response.metrics!!.splits.size)
            assertCopiedExactlyOnce(500)
        }
    }

    @Test
    fun theRateLimitHoldsAcrossChannels()
    {
        exec("INSERT INTO source_t SELECT g, 'n' || g, g FROM generate_series(1, 6000) g")
        val started = System.nanoTime()
        val response = MetaForgeExecutorService().start(parallelRequest(
            "SELECT id, name, amount FROM source_t",
            mapOf(
                "concurrency" to "3", "splitStrategy" to "RANGE", "splitColumn" to "id",
                "batchSize" to "200", "maxRowsPerSecond" to "3000"
            )
        ))
        val seconds = (System.nanoTime() - started) / 1e9

        assertTrue(response.message, response.successful)
        assertCopiedExactlyOnce(6000)
        // 6000 行 / 3000 行每秒 = 2 秒；首批不等待，留一点余量；上限防止限流失效以外的异常变慢
        assertTrue("rate limit not applied: took ${seconds}s", seconds >= 1.5)
        assertTrue("took too long: ${seconds}s", seconds < 10)
        assertTrue(response.metrics!!.throttleWaitMs > 0)
    }

    @Test
    fun stopCancelsEveryChannel()
    {
        val taskName = "pg-stop-parallel"
        val request = parallelRequest(
            "SELECT g AS id, 'x' AS name, g AS amount FROM generate_series(1, 3000000) g",
            mapOf("concurrency" to "3", "splitStrategy" to "RANGE", "splitColumn" to "id", "batchSize" to "500"),
            taskName
        )
        val executor = MetaForgeExecutorService()
        val responseRef = AtomicReference<ExecutorResponse>()
        val worker = Thread { responseRef.set(executor.start(request)) }
        worker.start()

        var waited = 0
        while (count("target_t") == 0L && waited < 60_000)
        {
            Thread.sleep(50)
            waited += 50
        }
        assertTrue("sync did not start writing in time", count("target_t") > 0L)
        assertEquals(RunState.STOPPED, executor.stop(stopRequest(taskName)).state)

        worker.join(60_000)
        val response = responseRef.get()
        assertNotNull("worker did not finish after stop", response)
        assertEquals(RunState.STOPPED, response.state)
        // 已读的行都提交了，上报的行数等于目标表实际行数 / the reported count matches what the target holds
        assertEquals(count("target_t"), response.count.toLong())
        assertTrue(response.count < 3_000_000)
    }

    @Test
    fun aWriteFailureInOneChannelEndsTheWholeTask()
    {
        // 目标表拒绝某一行：其余通道被叫停，任务以失败结束而不是挂起
        exec("INSERT INTO source_t SELECT g, 'n' || g, g FROM generate_series(1, 200000) g")
        exec("ALTER TABLE target_t ADD CONSTRAINT no_bad_row CHECK (uid <> 150000)")

        val response = MetaForgeExecutorService().start(parallelRequest(
            "SELECT id, name, amount FROM source_t",
            mapOf("concurrency" to "4", "splitStrategy" to "RANGE", "splitColumn" to "id", "batchSize" to "500")
        ))

        assertFalse(response.successful)
        assertEquals(RunState.FAILURE, response.state)
        assertTrue(response.message!!.contains("no_bad_row"))
        val metrics = response.metrics!!
        assertEquals(1, metrics.splits.count { it.state == "FAILED" })
        assertFalse(metrics.splits.any { it.state == "RUNNING" })
        assertEquals(count("target_t"), metrics.rowsWritten)
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private fun buildRequest(
        query: String,
        targetTable: String,
        columns: LinkedHashSet<OriginColumn>,
        taskName: String = ""
    ): ExecutorRequest
    {
        val input = ExecutorConfigure("postgresql", null, emptySet()).apply {
            plugin = service
            this.query = query
            database = "public"
            table = targetTable
            originConfigure = RuntimeConfigure()
            originColumns = columns
        }
        val output = ExecutorConfigure("postgresql", null, emptySet()).apply {
            plugin = service
            originConfigure = RuntimeConfigure()
        }
        return ExecutorRequest(null, input, output).apply {
            this.taskName = taskName
            userName = "tester"
        }
    }

    private fun stopRequest(taskName: String): ExecutorRequest
    {
        val placeholder = ExecutorConfigure(null)
        return ExecutorRequest(taskName, "", placeholder, placeholder)
    }

    private fun exec(sql: String)
    {
        DriverManager.getConnection(container().jdbcUrl, container().username, container().password).use { conn ->
            conn.createStatement().use { st -> st.execute(sql) }
        }
    }

    private fun query(sql: String): List<List<String?>>
    {
        DriverManager.getConnection(container().jdbcUrl, container().username, container().password).use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery(sql).use { rs ->
                    val n = rs.metaData.columnCount
                    val out = ArrayList<List<String?>>()
                    while (rs.next())
                    {
                        val row = ArrayList<String?>(n)
                        for (i in 1..n)
                        {
                            row.add(rs.getString(i))
                        }
                        out.add(row)
                    }
                    return out
                }
            }
        }
    }

    private fun count(table: String): Long =
        query("SELECT count(*) FROM $table").first().first()!!.toLong()
}
