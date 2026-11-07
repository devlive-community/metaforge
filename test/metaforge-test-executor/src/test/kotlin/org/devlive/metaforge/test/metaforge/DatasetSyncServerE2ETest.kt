package org.devlive.metaforge.test.metaforge

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.test.e2e.PluginE2E
import org.devlive.metaforge.test.metaforge.support.ChaosProxy
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.sql.DriverManager
import java.time.Duration

/**
 * 全链路端到端：解压真实发行包（dist/ 下的服务包和插件包），以独立进程启动真实服务，元数据放在真实 MySQL，
 * 源端是 PostgreSQL，数据集落到 ClickHouse，所有操作都经 HTTP API，与页面调用的接口相同。
 * 没有 dist/ 或 Docker 时跳过；先执行 `./mvnw install` 打包，否则测到的是旧包。
 *
 * Full-stack end-to-end: unpack the real distribution, run the real server as its own process with
 * metadata in MySQL, a PostgreSQL source and a ClickHouse dataset store, and drive it through the same
 * HTTP API the pages use. Skipped without dist/ or Docker; run `./mvnw install` first.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION", "OBL_UNSATISFIED_OBLIGATION", "ODR_OPEN_DATABASE_RESOURCE", "DMI_CONSTANT_DB_PASSWORD", "NP_NULL_ON_SOME_PATH", "NP_NULL_PARAM_DEREF", "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"])
class DatasetSyncServerE2ETest
{
    companion object
    {
        private const val ROWS = 60000
        private val PLUGINS = listOf(
            "metaforge-executor-metaforge", "metaforge-plugin-postgresql", "metaforge-plugin-mysql", "metaforge-plugin-clickhouse",
            "metaforge-convert-json", "metaforge-convert-none", "metaforge-scheduler-local", "metaforge-fs-local", "metaforge-parser-mysql"
        )
        private val mapper = ObjectMapper()
        private val http: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

        private lateinit var postgres: GenericContainer<*>
        private lateinit var clickhouse: GenericContainer<*>
        private lateinit var metadata: GenericContainer<*>
        private lateinit var sourceProxy: ChaosProxy
        private lateinit var home: File
        private var server: Process? = null
        private var port = 0
        private var token = ""
        private var source = ""

        private val pgUrl: String
            get() = "jdbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/test"
        private val chUrl: String
            get() = "jdbc:clickhouse://${clickhouse.host}:${clickhouse.getMappedPort(8123)}/metaforge"

        @BeforeClass
        @JvmStatic
        fun start()
        {
            PluginE2E.assumeDocker()
            val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "executor/metaforge-executor-metaforge/pom.xml").isFile }
            val dist = File(root, "dist")
            val serverPackage = File(dist, "metaforge-server-2026.0.0-bin.tar.gz")
            Assume.assumeTrue("Run ./mvnw install first: $serverPackage is missing", serverPackage.isFile)
            PLUGINS.forEach { Assume.assumeTrue("$it is not packaged", File(dist, "plugins/$it-bin.tar.gz").isFile) }

            postgres = GenericContainer<Nothing>("postgres:16").apply {
                withEnv("POSTGRES_USER", "test"); withEnv("POSTGRES_PASSWORD", "test"); withEnv("POSTGRES_DB", "test")
                withExposedPorts(5432)
                waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2).withStartupTimeout(Duration.ofMinutes(2)))
                start()
            }
            clickhouse = GenericContainer<Nothing>("clickhouse/clickhouse-server:26.8.34.13").apply {
                withEnv("CLICKHOUSE_USER", "test"); withEnv("CLICKHOUSE_PASSWORD", "test"); withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1"); withEnv("CLICKHOUSE_DB", "metaforge")
                withExposedPorts(8123)
                waitingFor(Wait.forHttp("/ping").forPort(8123).withStartupTimeout(Duration.ofMinutes(3)))
                start()
            }
            metadata = GenericContainer<Nothing>("mysql:8.4").apply {
                withEnv("MYSQL_ROOT_PASSWORD", "test"); withEnv("MYSQL_DATABASE", "metaforge")
                withExposedPorts(3306)
                waitingFor(Wait.forLogMessage(".*ready for connections.*port: 3306.*", 1).withStartupTimeout(Duration.ofMinutes(3)))
                start()
            }
            sourceProxy = ChaosProxy(postgres.host, postgres.getMappedPort(5432))
            seedSource()

            // 与安装到生产一样：解压服务包，插件解压到 plugins/<组件名>/ / unpack like a production install
            home = Files.createTempDirectory("metaforge-e2e").toFile()
            untar(serverPackage, home)
            val unpacked = home.listFiles()!!.single { it.isDirectory }
            home = unpacked
            PLUGINS.forEach { plugin ->
                val target = File(home, "plugins/$plugin").apply { mkdirs() }
                untar(File(dist, "plugins/$plugin-bin.tar.gz"), target)
                File(target, ".${System.currentTimeMillis()}.installed").createNewFile()
            }
            port = ServerSocket(0).use { it.localPort }
            File(home, "configure/application.properties").appendText(
                "\nserver.port=$port\nserver.address=127.0.0.1\n" +
                        "spring.datasource.url=jdbc:mysql://${metadata.host}:${metadata.getMappedPort(3306)}/metaforge?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8\n" +
                        "spring.datasource.username=root\nspring.datasource.password=test\nspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver\n" +
                        "metaforge.captcha.enable=false\nmetaforge.registration.enable=false\n"
            )
            startServer()
            install()
        }

        @AfterClass
        @JvmStatic
        fun stop()
        {
            server?.destroyForcibly()?.waitFor()
            if (this::sourceProxy.isInitialized) sourceProxy.close()
            if (this::postgres.isInitialized) postgres.stop()
            if (this::clickhouse.isInitialized) clickhouse.stop()
            if (this::metadata.isInitialized) metadata.stop()
            if (this::home.isInitialized) home.parentFile?.deleteRecursively()
        }

        private fun untar(archive: File, into: File)
        {
            val process = ProcessBuilder("tar", "-xzf", archive.absolutePath, "-C", into.absolutePath).redirectErrorStream(true).start()
            process.inputStream.readAllBytes()
            assertEquals("unpack $archive", 0, process.waitFor())
        }

        private fun seedSource()
        {
            DriverManager.getConnection(pgUrl, "test", "test").use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE TABLE source_t (id BIGINT PRIMARY KEY, k BIGINT, val TEXT)")
                    statement.execute("INSERT INTO source_t SELECT g, CASE WHEN g % 1000 = 0 THEN NULL ELSE g / 7 END, 'v' || g FROM generate_series(1, $ROWS) g")
                    statement.execute("CREATE TABLE bad_t (id BIGINT PRIMARY KEY, amount TEXT)")
                    statement.execute("INSERT INTO bad_t SELECT g, CASE WHEN g % 5000 = 0 THEN 'not-a-number' ELSE g::text END FROM generate_series(1, $ROWS) g")
                }
            }
        }

        private fun startServer()
        {
            val java = File(System.getProperty("java.home"), "bin/java").absolutePath
            val jvm = File(home, "configure/jvm.conf").takeIf { it.isFile }?.readLines().orEmpty()
                .map { it.trim() }.filter { it.startsWith("--add-opens") || it.startsWith("--add-exports") }
            val command = listOf(java) + jvm + listOf(
                "-Xmx1g", "-cp", "${home.absolutePath}/lib/*", "org.devlive.metaforge.server.MetaForge",
                "--spring.config.location=${home.absolutePath}/configure/"
            )
            server = ProcessBuilder(command).directory(home).redirectErrorStream(true).redirectOutput(File(home, "server.out")).start()
            val deadline = System.currentTimeMillis() + 240_000
            while (System.currentTimeMillis() < deadline)
            {
                assertTrue("the server exited:\n${serverLog()}", server!!.isAlive)
                try
                {
                    if (request("GET", "/api/v1/setup/status", null).get("status")?.asBoolean() == true)
                    {
                        return
                    }
                }
                catch (ex: java.io.IOException)
                {
                    // 还没起来 / not up yet
                }
                Thread.sleep(1000)
            }
            throw AssertionError("the server did not start:\n${serverLog()}")
        }

        private fun serverLog(): String = File(home, "server.out").takeIf { it.isFile }?.readLines().orEmpty().takeLast(40).joinToString("\n")

        private fun request(method: String, path: String, body: Any?): JsonNode
        {
            val builder = HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path")).timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
            if (token.isNotEmpty())
            {
                builder.header("Authorization", "Bearer $token")
            }
            val publisher = if (body == null) HttpRequest.BodyPublishers.noBody() else HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))
            val response = http.send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString())
            return mapper.readTree(response.body().ifBlank { "{}" })
        }

        /** 调用接口并要求成功，返回 data / call the API, require success, return data */
        private fun api(method: String, path: String, body: Any? = null): JsonNode
        {
            val response = request(method, path, body)
            assertTrue("$method $path failed: $response", response.get("status")?.asBoolean() == true)
            return response.get("data") ?: mapper.nullNode()
        }

        private fun signIn()
        {
            token = ""
            val auth = api("POST", "/api/auth/signin", mapOf("username" to "admin", "password" to "admin123456"))
            token = requireNotNull(auth.get("token")) { "no token in $auth" }.asText()
        }

        private fun install()
        {
            if (!api("GET", "/api/v1/setup/status").get("installed").asBoolean())
            {
                api("POST", "/api/v1/setup/install", mapOf(
                    "username" to "admin", "password" to "admin123456", "name" to "Admin",
                    "database" to mapOf("type" to "current")
                ))
            }
            signIn()
            // 数据集落到真实 ClickHouse；保存后立即生效 / the dataset store is the real ClickHouse
            api("PUT", "/api/v1/configure/runtime/save/DATASET/Default", mapOf(
                "type" to "ClickHouse", "host" to clickhouse.host, "port" to clickhouse.getMappedPort(8123).toString(),
                "username" to "test", "password" to "test", "database" to "metaforge", "tableDefaultEngine" to "MergeTree",
                "tablePrefix" to "metaforge_", "maxConcurrentSyncs" to "4"
            ))
            // 与页面相同：取插件声明的表单，填值后保存 / like the page: fill in the form the plugin declares and save it
            val plugin = api("GET", "/api/v2/source/plugins").first { it.get("name").asText() == "PostgreSQL" }
            val configure = plugin.get("configure").deepCopy<com.fasterxml.jackson.databind.node.ObjectNode>()
            val values = mapOf("name" to "e2e-source", "host" to "127.0.0.1", "port" to sourceProxy.port, "username" to "test", "password" to "test", "database" to "test")
            configure.get("configures").forEach { field ->
                values[field.get("field").asText()]?.let { (field as com.fasterxml.jackson.databind.node.ObjectNode).putPOJO("value", it) }
            }
            api("POST", "/api/v2/source", mapOf("name" to "PostgreSQL", "type" to plugin.get("type").asText(), "configure" to configure))
            val sources = api("POST", "/api/v2/source/list", mapOf("page" to 1, "size" to 100)).get("content")
            source = requireNotNull(sources.firstOrNull { it.get("name").asText() == "e2e-source" }?.get("code")) { "no source in $sources" }.asText()
        }

        private fun columns(vararg columns: Triple<String, String, Boolean>): List<Map<String, Any?>> = columns.mapIndexed { index, (name, type, key) ->
            mapOf(
                "name" to name, "aliasName" to name, "original" to name, "type" to type, "comment" to "", "defaultValue" to "", "position" to index,
                "nullable" to !key, "length" to 0, "orderByKey" to key, "partitionKey" to false, "primaryKey" to key, "samplingKey" to false,
                "mode" to if (type == "STRING") "DIMENSION" else "METRIC", "customColumn" to false, "virtualColumn" to false
            )
        }

        private var sequence = 0

        /** 创建数据集并等它建好目标表 / create a dataset and wait for its target table */
        private fun dataset(query: String, columns: List<Map<String, Any?>>): String
        {
            sequence ++
            val saved = api("POST", "/api/v1/dataset", mapOf(
                "name" to "e2e-$sequence", "description" to "", "query" to query, "syncMode" to "MANUAL", "scheduler" to "LocalScheduler", "executor" to "MetaForge",
                "source" to mapOf("code" to source), "columns" to columns
            ))
            val code = requireNotNull(saved.get("code")) { "no code in $saved" }.asText()
            val deadline = System.currentTimeMillis() + 60_000
            while (System.currentTimeMillis() < deadline)
            {
                // 保存是异步的，刚提交时可能还查不到 / saving is asynchronous, so it may not be there yet
                val response = request("GET", "/api/v1/dataset/info/$code", null)
                if (response.get("status")?.asBoolean() != true)
                {
                    Thread.sleep(300)
                    continue
                }
                val info = response.get("data")
                val states = info.get("state")?.map { it.asText() }.orEmpty()
                if (states.contains("TABLE_SUCCESS") && !info.get("tableName").isNull)
                {
                    return code
                }
                assertFalse("building the dataset failed: ${info.get("message")}", states.any { it.endsWith("FAILED") })
                Thread.sleep(300)
            }
            throw AssertionError("the dataset was not built in time")
        }

        private fun table(code: String): String = api("GET", "/api/v1/dataset/info/$code").get("tableName").asText()

        private fun longs(sql: String): List<Long>
        {
            DriverManager.getConnection(chUrl, "test", "test").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(sql).use { rs ->
                        rs.next()
                        return (1..rs.metaData.columnCount).map { rs.getLong(it) }
                    }
                }
            }
        }
    }

    private val rowColumns = columns(Triple("id", "NUMBER_SIGNED", true), Triple("k", "NUMBER_SIGNED", false), Triple("val", "STRING", false))
    private val rowQuery = "SELECT id, k, val FROM source_t"

    /** 分片且限流，保证任务跑得够久，能在中途操作 / split and rate limited, so the run lasts long enough to act midway */
    private fun options(extra: Map<String, String> = emptyMap()): Map<String, String> = mapOf(
        "concurrency" to "3", "splitStrategy" to "RANGE", "splitColumn" to "k", "splitCount" to "6", "batchSize" to "500",
        "maxRowsPerSecond" to "12000", "writeRetryBackoffMs" to "200"
    ) + extra

    private fun sync(code: String, options: Map<String, String>): Long = api("PUT", "/api/v1/dataset/syncData/$code", options).get("id").asLong()

    private fun view(id: Long): JsonNode = api("GET", "/api/v1/dataset/history/metrics/$id")

    private fun waitFor(id: Long, what: String, seconds: Int = 180, done: (JsonNode) -> Boolean): JsonNode
    {
        val deadline = System.currentTimeMillis() + seconds * 1000L
        var last: JsonNode? = null
        while (System.currentTimeMillis() < deadline)
        {
            last = view(id)
            if (done(last))
            {
                return last
            }
            // 已经结束却不是想等的状态，不用再等 / already finished in another state, so stop waiting
            val state = last.get("state").asText()
            if (state in listOf("FAILURE", "SUCCESS", "STOPPED", "TIMEOUT", "INTERRUPTED") && !last.get("live").asBoolean())
            {
                throw AssertionError("waiting for $what, but the sync ended as $state: $last")
            }
            Thread.sleep(200)
        }
        throw AssertionError("timed out waiting for $what, last view: $last\n${serverLog()}")
    }

    /** 等到状态；终态还要等收尾完成（live=false），此后才能再次运行 / wait for a state; final states also wait for the wrap-up */
    private fun waitState(id: Long, state: String, seconds: Int = 180): JsonNode = waitFor(id, state, seconds) {
        it.get("state").asText() == state && (state == "PAUSED" || !it.get("live").asBoolean())
    }

    private fun waitWritten(id: Long, rows: Long): JsonNode = waitFor(id, "$rows rows written") { (it.get("metrics")?.get("rowsWritten")?.asLong() ?: 0) >= rows }

    private fun assertExactCopy(code: String, rows: Long = ROWS.toLong())
    {
        val target = "metaforge.`${table(code)}`"
        val (count, distinct, sum) = longs("SELECT COUNT(*), COUNT(DISTINCT id), SUM(id) FROM $target")
        assertEquals("row count", rows, count)
        assertEquals("distinct ids", rows, distinct)
        assertEquals("sum of ids", rows * (rows + 1) / 2, sum)
    }

    private fun written(code: String): Long = longs("SELECT COUNT(*) FROM metaforge.`${table(code)}`")[0]

    private fun transitions(view: JsonNode): List<String> = view.get("transitions").map { it.get("to").asText() }

    @Test
    fun a_pauseResumeStopAndRunAgainThroughTheApiCopyEveryRowOnce()
    {
        val code = dataset(rowQuery, rowColumns)
        val id = sync(code, options())

        // 页面用的指标都从接口拿到 / the metrics the pages show come through the API
        val running = waitWritten(id, 3000)
        assertEquals("RUNNING", running.get("state").asText())
        assertTrue(running.get("live").asBoolean())
        val live = running.get("metrics")
        assertEquals("RANGE", live.get("splitStrategy").asText())
        assertEquals("k", live.get("splitColumn").asText())
        assertTrue(live.get("resumable").asBoolean())
        assertEquals(3, live.get("channels").asInt())
        assertEquals(7, live.get("splits").size())
        assertTrue(live.get("throttleWaitMs").asLong() > 0)
        assertTrue(live.get("bytesWritten").asLong() > 0)
        assertTrue(live.get("series").size() > 0)
        // 列表页的行内进度 / the progress on the list page
        val listed = api("GET", "/api/v1/dataset/sync/active").first { it.get("datasetCode").asText() == code }
        assertEquals(id, listed.get("historyId").asLong())
        assertEquals("RUNNING", listed.get("state").asText())
        assertTrue(listed.get("rowsWritten").asLong() > 0)

        api("PUT", "/api/v1/dataset/history/pause/$id")
        val paused = waitState(id, "PAUSED")
        val pausedAt = paused.get("metrics").get("rowsWritten").asLong()
        assertTrue("paused midway: $pausedAt", pausedAt in 3000 until ROWS)
        assertEquals("the page shows what the target holds", written(code), pausedAt)
        Thread.sleep(1500)
        assertEquals("nothing is written while paused", pausedAt, written(code))
        // 暂停时数据集仍被占用，不能再次同步 / a paused sync still holds the dataset
        assertFalse(request("PUT", "/api/v1/dataset/syncData/$code", options()).get("status").asBoolean())

        api("PUT", "/api/v1/dataset/history/resume/$id")
        waitWritten(id, pausedAt + 3000)
        api("PUT", "/api/v1/dataset/history/stop/$id")
        val stopped = waitState(id, "STOPPED")
        val stoppedAt = stopped.get("metrics").get("rowsWritten").asLong()
        assertTrue(stoppedAt > pausedAt)
        assertTrue("a stopped split run can continue", stopped.get("retryable").asBoolean())
        assertEquals(written(code), stoppedAt)

        api("PUT", "/api/v1/dataset/history/retry/$id")
        val finished = waitState(id, "SUCCESS")
        assertFalse("a finished sync leaves the list", api("GET", "/api/v1/dataset/sync/active").any { it.get("datasetCode").asText() == code })
        assertEquals(2, finished.get("attempt").asInt())
        assertFalse(finished.get("retryable").asBoolean())
        val metrics = finished.get("metrics")
        assertEquals(ROWS.toLong(), metrics.get("rowsWritten").asLong())
        assertEquals(100.0, metrics.get("progress").asDouble(), 0.0)
        assertTrue(metrics.get("splits").all { it.get("state").asText() == "DONE" })
        assertEquals("FINALIZE", metrics.get("stages").last().get("name").asText())
        // PAUSING / STOPPING 可能来不及记录就已经完成，不比较 / the in-between states may be skipped when the work finishes first
        assertEquals(
            listOf("CREATED", "QUEUE", "RUNNING", "PAUSED", "QUEUE", "RUNNING", "STOPPED", "QUEUE", "RUNNING", "SUCCESS"),
            transitions(finished).filter { it != "PAUSING" && it != "STOPPING" }
        )
        assertExactCopy(code)
    }

    @Test
    fun b_aLostSourceIsRetriedAutomaticallyFromTheCheckpoint()
    {
        val code = dataset(rowQuery, rowColumns)
        val id = sync(code, options(mapOf("maxRetries" to "2", "retryIntervalSeconds" to "1")))

        waitWritten(id, 5000)
        sourceProxy.dropAll()

        val finished = waitState(id, "SUCCESS")
        assertTrue(transitions(finished).contains("RETRYING"))
        assertEquals(2, finished.get("attempt").asInt())
        assertExactCopy(code)
    }

    @Test
    fun c_lostSourceWithoutRetriesFailsAndRunsAgainOnRequest()
    {
        val code = dataset(rowQuery, rowColumns)
        // 不指定分片列：默认取数据集主键 / no split column: the dataset's primary key is the default
        val id = sync(code, options() - "splitColumn")

        val running = waitWritten(id, 5000)
        assertEquals("id", running.get("metrics").get("splitColumn").asText())
        sourceProxy.dropAll()

        val failed = waitState(id, "FAILURE")
        assertTrue(failed.get("retryable").asBoolean())
        api("PUT", "/api/v1/dataset/history/retry/$id")
        waitState(id, "SUCCESS")
        assertExactCopy(code)
    }

    @Test
    fun d_rejectedRowsAreSkippedAndShownInTheMetrics()
    {
        val code = dataset("SELECT id, amount FROM bad_t", columns(Triple("id", "NUMBER_SIGNED", true), Triple("amount", "NUMBER_SIGNED", false)))
        val bad = (ROWS / 5000).toLong()
        val id = sync(code, mapOf("concurrency" to "2", "splitStrategy" to "RANGE", "splitColumn" to "id", "batchSize" to "500", "maxErrorRows" to "$bad"))

        val finished = waitState(id, "SUCCESS")
        val metrics = finished.get("metrics")
        assertEquals(bad, metrics.get("rowsSkipped").asLong())
        assertEquals(ROWS - bad, metrics.get("rowsWritten").asLong())
        assertEquals(bad.toInt(), metrics.get("dirtySamples").size())
        assertEquals("not-a-number", metrics.get("dirtySamples")[0].get("row").get("amount").asText())
        assertEquals(ROWS - bad, written(code))
        assertEquals(0L, longs("SELECT COUNT(*) FROM metaforge.`${table(code)}` WHERE id % 5000 = 0")[0])
    }

    @Test
    fun e_withoutSplitsARunCannotPauseAndCannotRunAgainAfterWriting()
    {
        val code = dataset(rowQuery, rowColumns)
        val id = sync(code, mapOf("batchSize" to "500", "maxRowsPerSecond" to "12000"))

        val running = waitWritten(id, 2000)
        assertFalse(running.get("metrics").get("resumable").asBoolean())
        val refused = request("PUT", "/api/v1/dataset/history/pause/$id", null)
        assertFalse(refused.get("status").asBoolean())
        assertTrue(refused.toString(), refused.get("message").asText().contains("cannot be paused"))

        api("PUT", "/api/v1/dataset/history/stop/$id")
        val stopped = waitState(id, "STOPPED")
        assertFalse(stopped.get("retryable").asBoolean())
        val again = request("PUT", "/api/v1/dataset/history/retry/$id", null)
        assertFalse(again.get("status").asBoolean())
        assertTrue(again.toString(), again.get("message").asText().contains("could write rows twice"))
    }

    @Test
    fun f_syncsBeyondTheLimitQueueAndADatasetRunsOneAtATime()
    {
        val settings = api("GET", "/api/v1/configure/runtime/detail/DATASET/Default").get("values")
        val limited = mapper.convertValue(settings, Map::class.java).toMutableMap()
        limited["maxConcurrentSyncs"] = "1"
        api("PUT", "/api/v1/configure/runtime/save/DATASET/Default", limited)
        try
        {
            val first = dataset(rowQuery, rowColumns)
            val second = dataset(rowQuery, rowColumns)
            val third = dataset(rowQuery, rowColumns)
            val firstId = sync(first, options())
            waitWritten(firstId, 1000)
            val secondId = sync(second, options(mapOf("maxRowsPerSecond" to "0")))
            val thirdId = sync(third, options(mapOf("maxRowsPerSecond" to "0")))

            assertEquals("QUEUE", view(secondId).get("state").asText())
            assertEquals("QUEUE", view(thirdId).get("state").asText())
            assertFalse("a dataset runs one sync at a time", request("PUT", "/api/v1/dataset/syncData/$first", options()).get("status").asBoolean())

            api("PUT", "/api/v1/dataset/history/stop/$thirdId")
            assertEquals("a queued sync is cancelled at once", "STOPPED", view(thirdId).get("state").asText())
            assertEquals(0L, written(third))

            api("PUT", "/api/v1/dataset/history/stop/$firstId")
            waitState(firstId, "STOPPED")
            waitState(secondId, "SUCCESS")
            assertExactCopy(second)
        }
        finally
        {
            limited["maxConcurrentSyncs"] = "4"
            api("PUT", "/api/v1/configure/runtime/save/DATASET/Default", limited)
        }
    }

    @Test
    fun g_aPausedRunSurvivesAServerCrashAndARunningOneRefusesToRunAgain()
    {
        val pausedCode = dataset(rowQuery, rowColumns)
        val pausedId = sync(pausedCode, options())
        waitWritten(pausedId, 3000)
        api("PUT", "/api/v1/dataset/history/pause/$pausedId")
        waitState(pausedId, "PAUSED")
        val pausedAt = written(pausedCode)

        val runningCode = dataset(rowQuery, rowColumns)
        val runningId = sync(runningCode, options())
        waitWritten(runningId, 3000)

        // 真实崩溃：kill -9 后重启同一个安装目录 / a real crash: kill -9, then restart the same install
        server!!.destroyForcibly().waitFor()
        token = ""
        startServer()
        signIn()

        val interruptedPaused = view(pausedId)
        assertEquals("INTERRUPTED", interruptedPaused.get("state").asText())
        assertTrue("a paused run's checkpoint is exact", interruptedPaused.get("retryable").asBoolean())
        assertEquals(pausedAt, written(pausedCode))

        val interruptedRunning = view(runningId)
        assertEquals("INTERRUPTED", interruptedRunning.get("state").asText())
        assertFalse("rows may have been committed after the last save", interruptedRunning.get("retryable").asBoolean())
        val refused = request("PUT", "/api/v1/dataset/history/retry/$runningId", null)
        assertFalse(refused.get("status").asBoolean())

        api("PUT", "/api/v1/dataset/history/retry/$pausedId")
        waitState(pausedId, "SUCCESS")
        assertExactCopy(pausedCode)
        assertNotNull(view(pausedId).get("metrics"))
    }
}
