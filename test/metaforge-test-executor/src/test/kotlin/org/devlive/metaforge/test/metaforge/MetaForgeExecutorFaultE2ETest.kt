package org.devlive.metaforge.test.metaforge

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.common.RunState
import org.devlive.metaforge.executor.configure.ExecutorConfigure
import org.devlive.metaforge.executor.configure.ExecutorMetrics
import org.devlive.metaforge.executor.configure.ExecutorMetricsListener
import org.devlive.metaforge.executor.configure.ExecutorRequest
import org.devlive.metaforge.executor.configure.ExecutorResponse
import org.devlive.metaforge.executor.configure.OriginColumn
import org.devlive.metaforge.executor.metaforge.MetaForgeExecutorService
import org.devlive.metaforge.plugin.ClickHouseService
import org.devlive.metaforge.plugin.MySQLService
import org.devlive.metaforge.plugin.PostgreSQLService
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.RuntimeConfigure
import org.devlive.metaforge.test.e2e.PluginE2E
import org.devlive.metaforge.test.metaforge.support.ChaosProxy
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.sql.DriverManager
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * 真实端到端：数据库跑在容器里，读写全走生产路径（真实插件、真实 JDBC 驱动、JdbcStreamAdapter），
 * 网络故障由 [ChaosProxy] 真实切断连接。每组测试都核对目标表的实际行，而不只看执行器的上报。
 * 源/目标组合覆盖 PostgreSQL、MySQL、ClickHouse 各自作为源和目标。
 *
 * Real end-to-end runs: databases in containers, reads and writes through the production path, and
 * network failures made by cutting real connections. Each test checks the rows actually in the target.
 */
@RunWith(Parameterized::class)
@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION", "OBL_UNSATISFIED_OBLIGATION", "ODR_OPEN_DATABASE_RESOURCE", "DMI_CONSTANT_DB_PASSWORD", "NP_NULL_ON_SOME_PATH", "NP_NULL_PARAM_DEREF", "ME_MUTABLE_ENUM_FIELD", "NP_NONNULL_RETURN_VIOLATION", "SIC_INNER_SHOULD_BE_STATIC_ANON"])
class MetaForgeExecutorFaultE2ETest(private val sourceEngine: Engine, private val targetEngine: Engine)
{
    /** 一种真实数据库：容器、建表语法与写入位置 / one real database: container, DDL dialect and where rows go */
    enum class Engine(val long: String, val nullableLong: String, val text: String, val shortText: String, val schema: String, val tableSuffix: String)
    {
        POSTGRES("BIGINT", "BIGINT", "TEXT", "VARCHAR(8)", "public", ""),
        MYSQL("BIGINT", "BIGINT", "VARCHAR(64)", "VARCHAR(8)", "test", ""),
        CLICKHOUSE("Int64", "Nullable(Int64)", "String", "FixedString(8)", "default", " ENGINE = MergeTree ORDER BY id");

        lateinit var container: GenericContainer<*>
        lateinit var proxy: ChaosProxy

        fun service(): PluginService = when (this)
        {
            POSTGRES -> PostgreSQLService()
            MYSQL -> MySQLService()
            CLICKHOUSE -> ClickHouseService()
        }

        private val mappedPort: Int
            get() = container.getMappedPort(if (this == POSTGRES) 5432 else if (this == MYSQL) 3306 else 8123)

        fun start()
        {
            container = when (this)
            {
                POSTGRES -> GenericContainer<Nothing>("postgres:16").apply {
                    withEnv("POSTGRES_USER", "test"); withEnv("POSTGRES_PASSWORD", "test"); withEnv("POSTGRES_DB", "test")
                    withExposedPorts(5432)
                    waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2).withStartupTimeout(Duration.ofMinutes(2)))
                }
                MYSQL -> GenericContainer<Nothing>("mysql:8.4").apply {
                    withEnv("MYSQL_ROOT_PASSWORD", "test"); withEnv("MYSQL_USER", "test"); withEnv("MYSQL_PASSWORD", "test"); withEnv("MYSQL_DATABASE", "test")
                    withExposedPorts(3306)
                    waitingFor(Wait.forLogMessage(".*ready for connections.*port: 3306.*", 1).withStartupTimeout(Duration.ofMinutes(3)))
                }
                CLICKHOUSE -> GenericContainer<Nothing>("clickhouse/clickhouse-server:26.8.34.13").apply {
                    withEnv("CLICKHOUSE_USER", "test"); withEnv("CLICKHOUSE_PASSWORD", "test"); withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1")
                    withExposedPorts(8123)
                    waitingFor(Wait.forHttp("/ping").forPort(8123).withStartupTimeout(Duration.ofMinutes(3)))
                }
            }
            container.start()
            proxy = ChaosProxy(container.host, mappedPort)
        }

        fun stop()
        {
            if (this::proxy.isInitialized) proxy.close()
            if (this::container.isInitialized) container.stop()
        }

        private fun url(): String = when (this)
        {
            POSTGRES -> "jdbc:postgresql://${container.host}:$mappedPort/test"
            MYSQL -> "jdbc:mysql://${container.host}:$mappedPort/test?useSSL=false&allowPublicKeyRetrieval=true"
            CLICKHOUSE -> "jdbc:clickhouse://${container.host}:$mappedPort/default"
        }

        /** 直连容器，不经过故障代理，用来建表和核对结果 / direct to the container, bypassing the proxy, for setup and checks */
        fun exec(vararg statements: String)
        {
            DriverManager.getConnection(url(), "test", "test").use { connection ->
                connection.createStatement().use { statement -> statements.forEach { statement.execute(it) } }
            }
        }

        fun longs(sql: String): List<Long>
        {
            DriverManager.getConnection(url(), "test", "test").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(sql).use { rs ->
                        rs.next()
                        return (1..rs.metaData.columnCount).map { rs.getLong(it) }
                    }
                }
            }
        }

        fun text(sql: String): String?
        {
            DriverManager.getConnection(url(), "test", "test").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(sql).use { rs -> return if (rs.next()) rs.getString(1) else null }
                }
            }
        }

        /** 插入 rows 行：id=1..rows，k=id/7（不唯一），每 1000 行一个 NULL 键；val 由 valueOf 给出 SQL 表达式 */
        fun seed(rows: Int, value: String)
        {
            val generator = when (this)
            {
                POSTGRES -> "SELECT g::bigint AS id, CASE WHEN g % 1000 = 0 THEN NULL ELSE g / 7 END AS k, ${value.replace("{id}", "g::text")} AS val FROM generate_series(1, $rows) g"
                MYSQL -> "SELECT g AS id, CASE WHEN g % 1000 = 0 THEN NULL ELSE g DIV 7 END AS k, ${value.replace("{id}", "g")} AS val FROM (" +
                        "SELECT a.d + b.d * 10 + c.d * 100 + e.d * 1000 + f.d * 10000 + 1 AS g FROM " +
                        DIGITS.joinToString(" CROSS JOIN ") { "(SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) $it" } +
                        ") nums WHERE g <= $rows"
                CLICKHOUSE -> "SELECT toInt64(number + 1) AS id, if((number + 1) % 1000 = 0, NULL, toInt64(intDiv(number + 1, 7))) AS k, ${value.replace("{id}", "toString(number + 1)")} AS val FROM numbers($rows)"
            }
            exec("INSERT INTO source_t (id, k, val) $generator")
        }

        companion object
        {
            private val DIGITS = listOf("a", "b", "c", "e", "f")
        }
    }

    companion object
    {
        private const val ROWS = 60000

        @JvmStatic
        @Parameterized.Parameters(name = "{0} to {1}")
        fun pairs(): List<Array<Any>> = listOf(
            arrayOf(Engine.POSTGRES, Engine.MYSQL),
            arrayOf(Engine.MYSQL, Engine.CLICKHOUSE),
            arrayOf(Engine.CLICKHOUSE, Engine.POSTGRES)
        )

        @ClassRule
        @JvmField
        val folder = TemporaryFolder()

        @BeforeClass
        @JvmStatic
        fun start()
        {
            PluginE2E.assumeDocker()
            Engine.values().forEach { it.start() }
        }

        @AfterClass
        @JvmStatic
        fun stop()
        {
            Engine.values().forEach { it.stop() }
        }
    }

    private lateinit var taskName: String

    @Before
    fun tables()
    {
        taskName = "fault-${System.nanoTime()}"
        sourceEngine.exec(
            "DROP TABLE IF EXISTS source_t",
            "CREATE TABLE source_t (id ${sourceEngine.long}, k ${sourceEngine.nullableLong}, val ${sourceEngine.text})${sourceEngine.tableSuffix}"
        )
        targetEngine.exec(
            "DROP TABLE IF EXISTS target_t",
            "CREATE TABLE target_t (id ${targetEngine.long}, k ${targetEngine.nullableLong}, val ${targetEngine.text})${targetEngine.tableSuffix}"
        )
        sourceEngine.proxy.down = false
        targetEngine.proxy.down = false
    }

    private fun configure(engine: Engine, service: PluginService): RuntimeConfigure =
        PluginE2E.configure("127.0.0.1", engine.proxy.port, "test", "test", if (engine == Engine.CLICKHOUSE) "default" else "test")
            .also {
                it.setDriver(service.driver())
                if (engine == Engine.MYSQL)
                {
                    it.setEnv(java.util.Optional.of(mapOf("useSSL" to "false", "allowPublicKeyRetrieval" to "true")))
                }
            }

    private val workHome: String by lazy { folder.newFolder(taskName).absolutePath }

    private fun request(options: Map<String, String>, query: String = "SELECT id, k, val FROM source_t"): ExecutorRequest
    {
        val source = sourceEngine.service()
        val target = targetEngine.service()
        val input = ExecutorConfigure(sourceEngine.name, null, emptySet()).apply {
            plugin = source
            this.query = query
            database = targetEngine.schema
            table = "target_t"
            originConfigure = configure(sourceEngine, source)
            originColumns = linkedSetOf(OriginColumn("id", "id"), OriginColumn("k", "k"), OriginColumn("val", "val"))
        }
        val output = ExecutorConfigure(targetEngine.name, null, emptySet()).apply {
            plugin = target
            originConfigure = configure(targetEngine, target)
        }
        return ExecutorRequest(taskName, "tester", input, output).apply {
            timeout = 0
            this.workHome = this@MetaForgeExecutorFaultE2ETest.workHome
            this.options = options
        }
    }

    private fun split(extra: Map<String, String> = emptyMap()): Map<String, String> = mapOf(
        "concurrency" to "3", "splitStrategy" to "RANGE", "splitColumn" to "k", "splitCount" to "6",
        "batchSize" to "500", "metricsIntervalMs" to "200"
    ) + extra

    private val sumOfIds = ROWS.toLong() * (ROWS + 1) / 2

    /** 目标表逐行核对：没有丢行也没有重复 / check the target's rows: nothing lost, nothing duplicated */
    private fun assertExactCopy(rows: Int = ROWS)
    {
        val (count, distinct, sum) = targetEngine.longs("SELECT COUNT(*), COUNT(DISTINCT id), SUM(id) FROM target_t")
        assertEquals("row count", rows.toLong(), count)
        assertEquals("distinct ids", rows.toLong(), distinct)
        assertEquals("sum of ids", rows.toLong() * (rows + 1) / 2, sum)
        assertEquals("NULL keys arrive as NULL", (rows / 1000).toLong(), targetEngine.longs("SELECT COUNT(*) FROM target_t WHERE k IS NULL")[0])
        assertEquals("v12345", targetEngine.text("SELECT val FROM target_t WHERE id = 12345"))
    }

    /** 后台运行，到写入 minRows 行后执行 action，返回结果 / run in the background, act once minRows are written, return the result */
    private fun runAndAct(service: MetaForgeExecutorService, request: ExecutorRequest, minRows: Long, action: () -> Unit): ExecutorResponse
    {
        val result = AtomicReference<ExecutorResponse>()
        val latest = AtomicReference<ExecutorMetrics>()
        request.metricsListener = ExecutorMetricsListener { latest.set(it) }
        val worker = Thread { result.set(service.start(request)) }
        worker.start()
        val deadline = System.currentTimeMillis() + 120_000
        while ((latest.get()?.rowsWritten ?: 0) < minRows && worker.isAlive && System.currentTimeMillis() < deadline)
        {
            Thread.sleep(20)
        }
        assertTrue("the run ended before it wrote $minRows rows: ${result.get()?.message}", worker.isAlive)
        action()
        worker.join(180_000)
        assertFalse("the run did not end", worker.isAlive)
        return result.get()
    }

    @Test
    fun pausedRunsResumeExactlyEvenWithRepeatedKeysAndNulls()
    {
        sourceEngine.seed(ROWS, "'v' || {id}".let { if (sourceEngine == Engine.POSTGRES) it else "concat('v', {id})" })
        val service = MetaForgeExecutorService()
        // 限流让任务跑得够久，暂停一定发生在中途 / the rate limit keeps the run long enough to pause midway
        val request = request(split(mapOf("maxRowsPerSecond" to "15000")))

        val paused = runAndAct(service, request, 3000) {
            assertTrue(service.pause(request).successful)
        }
        assertEquals(paused.message, RunState.PAUSED, paused.state)
        assertFalse(paused.successful)
        assertNotNull("a paused run leaves a checkpoint", paused.checkpoint)
        val metrics = paused.metrics!!
        assertTrue(metrics.resumable)
        assertTrue("paused midway: ${metrics.rowsWritten}", metrics.rowsWritten in 3000 until ROWS)
        assertEquals("the report matches the target", targetEngine.longs("SELECT COUNT(*) FROM target_t")[0], metrics.rowsWritten)
        assertEquals("stages show the pause", "PAUSED", metrics.stages.single { it.name == "TRANSFER" }.state)
        assertFalse(metrics.splits.any { it.state == "RUNNING" })
        assertTrue(metrics.splits.any { it.state == "PENDING" })

        // 第二次：续传后再停止，断点继续有效 / resume, stop, and the newer checkpoint still works
        request.checkpoint = paused.checkpoint
        val stopped = runAndAct(service, request, metrics.rowsWritten + 3000) {
            assertTrue(service.stop(request).successful)
        }
        assertEquals(stopped.message, RunState.STOPPED, stopped.state)
        assertNotNull(stopped.checkpoint)
        assertEquals(targetEngine.longs("SELECT COUNT(*) FROM target_t")[0], stopped.metrics!!.rowsWritten)
        assertTrue("totals carry over", stopped.metrics!!.rowsWritten > metrics.rowsWritten)
        assertTrue("elapsed time carries over", stopped.metrics!!.elapsedMs > metrics.elapsedMs)

        request.checkpoint = stopped.checkpoint
        request.options = split()
        val finished = service.start(request)

        assertTrue(finished.message, finished.successful)
        assertEquals(ROWS, finished.count)
        assertNull(finished.checkpoint)
        assertExactCopy()
        assertEquals(ROWS.toLong(), finished.metrics!!.rowsRead)
        assertEquals(ROWS.toLong(), finished.metrics!!.rowsWritten)
        assertTrue(finished.metrics!!.splits.all { it.state == "DONE" })
    }

    @Test
    fun lostTargetConnectionsAreRetriedWithoutLosingRows()
    {
        sourceEngine.seed(ROWS, if (sourceEngine == Engine.POSTGRES) "'v' || {id}" else "concat('v', {id})")
        val service = MetaForgeExecutorService()
        val request = request(split(mapOf("maxRowsPerSecond" to "15000", "writeRetries" to "8", "writeRetryBackoffMs" to "200")))

        val response = runAndAct(service, request, 5000) {
            // 真实断开：目标端连接全部切断，并且 1 秒内连不上 / real outage: every target connection cut, reconnects refused for a second
            targetEngine.proxy.down = true
            targetEngine.proxy.dropAll()
            Thread.sleep(1000)
            targetEngine.proxy.down = false
        }

        assertTrue(response.message, response.successful)
        assertTrue("batches were retried", response.metrics!!.batchRetries > 0)
        val (count, distinct, sum) = targetEngine.longs("SELECT COUNT(*), COUNT(DISTINCT id), SUM(id) FROM target_t")
        assertEquals("no row may be lost", ROWS.toLong(), distinct)
        // 写入途中断连时无法知道这批是否已生效：ClickHouse 没有事务，服务端可能已执行完这条 INSERT。
        // 所以这里是“至少一次”：不丢行，每条通道最多重复一批（一批 500 行，再加同一个键的最多 7 行）。
        // A cut during a write leaves the batch's fate unknown: ClickHouse may have run the INSERT already.
        // So this is at-least-once: nothing lost, and at most one batch per channel repeats.
        assertTrue("at most a batch per channel repeats: $count", count <= ROWS + 3 * 507)
        if (count == ROWS.toLong())
        {
            assertEquals(sumOfIds, sum)
        }
    }

    @Test
    fun aLostSourceConnectionFailsRetryablyAndResumesExactly()
    {
        // ClickHouse 走 HTTP，小结果集早已缓冲到客户端，断连碰不到它；用大结果集验证连接被切断时会报错而不是静默少行
        // ClickHouse answers over HTTP and buffers small results, so a large one shows a cut stream fails instead of losing rows.
        val rows = if (sourceEngine == Engine.CLICKHOUSE) 1_500_000 else ROWS
        sourceEngine.seed(rows, if (sourceEngine == Engine.POSTGRES) "'v' || {id}" else "concat('v', {id})")
        val service = MetaForgeExecutorService()
        val request = request(split(mapOf("maxRowsPerSecond" to "15000")))

        val failed = runAndAct(service, request, 5000) {
            sourceEngine.proxy.dropAll()
        }

        assertEquals(failed.message, RunState.FAILURE, failed.state)
        assertTrue("a lost connection is worth retrying: ${failed.message}", failed.retryable)
        assertNotNull(failed.checkpoint)
        assertEquals(targetEngine.longs("SELECT COUNT(*) FROM target_t")[0], failed.metrics!!.rowsWritten)

        request.checkpoint = failed.checkpoint
        request.options = split()
        val finished = service.start(request)

        assertTrue(finished.message, finished.successful)
        assertExactCopy(rows)
    }

    /** 目标列放不下的值：每 5000 行一个，共 ROWS/5000 个坏行 / values too long for the target column */
    private fun seedBadRows()
    {
        targetEngine.exec("DROP TABLE target_t", "CREATE TABLE target_t (id ${targetEngine.long}, k ${targetEngine.nullableLong}, val ${targetEngine.shortText})${targetEngine.tableSuffix}")
        val value = when (sourceEngine)
        {
            Engine.POSTGRES -> "CASE WHEN g % 5000 = 0 THEN 'far-too-long-for-the-column' ELSE 'ok' END"
            Engine.MYSQL -> "CASE WHEN g % 5000 = 0 THEN 'far-too-long-for-the-column' ELSE 'ok' END"
            Engine.CLICKHOUSE -> "if((number + 1) % 5000 = 0, 'far-too-long-for-the-column', 'ok')"
        }
        sourceEngine.seed(ROWS, value)
    }

    @Test
    fun rejectedRowsAreIsolatedRecordedAndSkippedWithinTheLimit()
    {
        seedBadRows()
        val bad = (ROWS / 5000).toLong()

        val response = MetaForgeExecutorService().start(request(split(mapOf("maxErrorRows" to "$bad"))))

        assertTrue(response.message, response.successful)
        val metrics = response.metrics!!
        assertEquals(bad, metrics.rowsSkipped)
        assertEquals(ROWS - bad, metrics.rowsWritten)
        assertEquals(0L, metrics.rowsFailed)
        // 坏行被精确定位：目标表恰好少这些行，好行一行不少 / exactly the bad rows are missing
        val (count, distinct) = targetEngine.longs("SELECT COUNT(*), COUNT(DISTINCT id) FROM target_t")
        assertEquals(ROWS - bad, count)
        assertEquals(ROWS - bad, distinct)
        assertEquals(0L, targetEngine.longs("SELECT COUNT(*) FROM target_t WHERE id % 5000 = 0")[0])
        assertEquals(bad.toInt(), metrics.dirtySamples.size)
        assertTrue(metrics.dirtySamples.all { it.row["val"] == "far-too-long-for-the-column" && it.error.isNotBlank() })
        val dirtyFile = File(workHome, "$taskName.dirty.jsonl")
        assertEquals("every rejected row is in the file", bad.toInt(), dirtyFile.readLines().size)
    }

    @Test
    fun tooManyRejectedRowsFailTheRunAndCannotResume()
    {
        seedBadRows()

        val response = MetaForgeExecutorService().start(request(split(mapOf("maxErrorRows" to "2", "concurrency" to "1"))))

        assertEquals(response.message, RunState.FAILURE, response.state)
        assertTrue(response.message, response.message!!.contains("More than 2 rows"))
        assertFalse(response.retryable)
        assertNull("a batch committed in part cannot resume exactly", response.checkpoint)
        assertTrue(response.metrics!!.rowsFailed > 0)
        assertEquals(targetEngine.longs("SELECT COUNT(*) FROM target_t")[0], response.metrics!!.rowsWritten)
    }

    @Test
    fun withoutToleranceTheFirstRejectedBatchFailsAndWritesNothingOfIt()
    {
        seedBadRows()

        val response = MetaForgeExecutorService().start(request(split(mapOf("concurrency" to "1"))))

        assertEquals(response.message, RunState.FAILURE, response.state)
        assertFalse("bad data does not get better by retrying", response.retryable)
        assertEquals(0L, response.metrics!!.rowsSkipped)
        assertTrue(response.metrics!!.rowsFailed > 0)
        assertEquals("the failed batch left nothing behind", targetEngine.longs("SELECT COUNT(*) FROM target_t")[0], response.metrics!!.rowsWritten)
    }

    @Test
    fun aRunWithoutSplitsCannotPauseOrResume()
    {
        sourceEngine.seed(ROWS, if (sourceEngine == Engine.POSTGRES) "'v' || {id}" else "concat('v', {id})")
        val service = MetaForgeExecutorService()
        val request = request(mapOf("batchSize" to "500", "metricsIntervalMs" to "200", "maxRowsPerSecond" to "15000"))

        val response = runAndAct(service, request, 2000) {
            val refused = service.pause(request)
            assertFalse(refused.successful)
            assertTrue(refused.message, refused.message!!.contains("cannot be paused"))
            assertTrue(service.stop(request).successful)
        }

        assertEquals(RunState.STOPPED, response.state)
        assertNull("no checkpoint: running again could write rows twice", response.checkpoint)
        assertFalse(response.metrics!!.resumable)
        assertEquals("rows read before the stop are committed", targetEngine.longs("SELECT COUNT(*) FROM target_t")[0], response.metrics!!.rowsWritten)
    }

    @Test
    fun tooManyRowsWithOneKeyGiveUpResumingButStillCopyEverything()
    {
        sourceEngine.seed(ROWS, if (sourceEngine == Engine.POSTGRES) "'v' || {id}" else "concat('v', {id})")
        // 所有行键相同（常量列）：这一组超过上限后放弃续传 / every row shares one key, past the group limit
        val constant = "SELECT id, k, val, 1 AS same FROM source_t"
        val request = request(mapOf("splitStrategy" to "RANGE", "splitColumn" to "same", "batchSize" to "500", "maxGroupRows" to "2000"), constant)

        val response = MetaForgeExecutorService().start(request)

        assertTrue(response.message, response.successful)
        assertFalse(response.metrics!!.resumable)
        assertExactCopy()
    }

    @Test
    fun metricsReportRealProgressWhileRunning()
    {
        sourceEngine.seed(ROWS, if (sourceEngine == Engine.POSTGRES) "'v' || {id}" else "concat('v', {id})")
        val snapshots = CopyOnWriteArrayList<ExecutorMetrics>()
        val request = request(split(mapOf("maxRowsPerSecond" to "30000", "preCount" to "true"))).apply {
            metricsListener = ExecutorMetricsListener { snapshots.add(it) }
        }

        val response = MetaForgeExecutorService().start(request)

        assertTrue(response.message, response.successful)
        assertExactCopy()
        val running = snapshots.filter { it.rowsWritten in 1 until ROWS }
        assertTrue("reported while running", running.size >= 3)
        assertTrue(running.all { it.totalRows == ROWS.toLong() && it.progress in 0.0..100.0 && it.etaSeconds >= 0 })
        assertTrue(running.any { it.writeRowsPerSecond > 0 && it.bytesWritten > 0 && it.splits.any { split -> split.state == "RUNNING" } })
        assertEquals(snapshots.map { it.rowsWritten }, snapshots.map { it.rowsWritten }.sorted())
        val last = response.metrics!!
        assertEquals(100.0, last.progress, 0.0)
        assertEquals(ROWS.toLong(), last.splits.sumOf { it.rowsWritten })
        assertEquals("RANGE", last.splitStrategy)
        assertTrue(last.batchesCommitted > 0)
    }
}
