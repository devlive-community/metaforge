package org.devlive.metaforge.test.local

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.common.RunState
import org.devlive.metaforge.executor.configure.ExecutorConfigure
import org.devlive.metaforge.executor.configure.ExecutorProgressListener
import org.devlive.metaforge.executor.configure.ExecutorRequest
import org.devlive.metaforge.executor.configure.ExecutorResponse
import org.devlive.metaforge.executor.configure.OriginColumn
import org.devlive.metaforge.executor.local.LocalExecutorService
import org.devlive.metaforge.spi.model.Configure
import org.devlive.metaforge.test.local.support.JdbcPluginService
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
 * 通过 [JdbcPluginService]（真实 JDBC）驱动 [LocalExecutorService] 完成一次真正的 source -> target 同步，
 * 然后直接查目标表核对落库数据。覆盖：流式搬运 + 投影/改名 + NULL/数值类型、preCount 总数、真实 stop 取消。
 *
 * 需要本机可用的 Docker。容器由 @BeforeClass/@AfterClass 管理，整个测试类只起停一次；
 * Docker 不可用时优雅跳过。
 */
@SuppressFBWarnings(value = ["OBL_UNSATISFIED_OBLIGATION", "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE"])
class LocalExecutorPostgresE2ETest
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

        val response = LocalExecutorService().start(request)

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
            progressListener = ExecutorProgressListener { processed, total -> progress.add(processed to total) }
        }

        val response = LocalExecutorService().start(request)

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

        val executor = LocalExecutorService()
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
            originConfigure = Configure()
            originColumns = columns
        }
        val output = ExecutorConfigure("postgresql", null, emptySet()).apply {
            plugin = service
            originConfigure = Configure()
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
