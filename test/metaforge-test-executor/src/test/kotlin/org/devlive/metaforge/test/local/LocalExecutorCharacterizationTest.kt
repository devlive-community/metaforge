package org.devlive.metaforge.test.local

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.common.RunState
import org.devlive.metaforge.executor.configure.ExecutorConfigure
import org.devlive.metaforge.executor.configure.ExecutorProgressListener
import org.devlive.metaforge.executor.configure.ExecutorRequest
import org.devlive.metaforge.executor.configure.OriginColumn
import org.devlive.metaforge.executor.local.LocalExecutorService
import org.devlive.metaforge.spi.model.Configure
import org.devlive.metaforge.spi.model.Response
import org.devlive.metaforge.test.local.support.FakePluginService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

/**
 * LocalExecutorService 的“特性测试”（characterization test）：
 * 只依赖内存 fake，不连任何数据库，用来在重构前锁定当前可观测行为。
 *
 * 注意：这里断言的是【当前实际行为】。Phase 2 已把“取消时上报已提交行数”落地，
 * 相关断言（streamingDriverAbortAfterCancelIsReportedAsStopped）已从 count=0 更新为 count=2；
 * 断言的每次变更都应对应一次有意为之、可见的行为调整。
 */
@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
class LocalExecutorCharacterizationTest
{
    private val mapper = ObjectMapper()

    // ---------------------------------------------------------------------
    // Streaming 双端流式路径
    // ---------------------------------------------------------------------

    @Test
    fun streamingCopiesProjectsAndPreservesNull()
    {
        // 源端返回 3 列，但只映射其中两列，并调换顺序，验证投影 + 丢列 + NULL 保留
        val source = FakePluginService(
            streaming = true,
            headers = listOf("id", "name", "age"),
            streamRows = listOf(
                listOf<Any?>(1, "alice", 30),
                listOf<Any?>(2, null, 41),
                listOf<Any?>(3, "carol", 25)
            )
        )
        val sink = FakePluginService(streaming = true)

        // 目标列顺序 = [full_name(<-name), uid(<-id)]，故意与源 headers 顺序不同
        val request = buildRequest(
            source, sink,
            originColumns = linkedSetOf(
                OriginColumn("full_name", "name"),
                OriginColumn("uid", "id")
            )
        )

        val response = LocalExecutorService().start(request)

        assertTrue(response.successful)
        assertEquals(RunState.SUCCESS, response.state)
        assertEquals(3, response.count)
        assertEquals(
            listOf(
                listOf<Any?>("alice", 1),
                listOf<Any?>(null, 2),
                listOf<Any?>("carol", 3)
            ),
            sink.committedRows.toList()
        )
    }

    @Test
    fun streamingPreCountPopulatesProgressTotal()
    {
        val source = FakePluginService(
            streaming = true,
            headers = listOf("id"),
            streamRows = listOf(listOf<Any?>(1), listOf<Any?>(2)),
            // preCount 会对包了 metaforge_precount_t 的查询调用 execute()
            executeHandler = { sql ->
                if (sql.contains("metaforge_precount_t")) countResponse(42)
                else Response.builder().isSuccessful(true).columns(emptyList<Any>()).build()
            }
        )
        val sink = FakePluginService(streaming = true)

        val progress = CopyOnWriteArrayList<Pair<Long, Long>>()
        val request = buildRequest(
            source, sink,
            originColumns = linkedSetOf(OriginColumn("id", "id"))
        ).apply {
            options = mapOf("preCount" to "true")
            progressListener = ExecutorProgressListener { processed, total -> progress.add(processed to total) }
        }

        val response = LocalExecutorService().start(request)

        assertTrue(response.successful)
        assertEquals(2, response.count)
        // 第一条进度应是 pre-count 后上报的 (0, 42)，最后一条应带上相同 total=42
        assertEquals(0L to 42L, progress.first())
        assertEquals(42L, progress.last().second)
    }

    @Test
    fun streamingStopMidStreamViaCancelledFlag()
    {
        val service = LocalExecutorService()
        val source = FakePluginService(
            streaming = true,
            headers = listOf("id"),
            streamRows = (1..5).map { listOf<Any?>(it) }
        )
        val sink = FakePluginService(streaming = true)
        val request = buildRequest(
            source, sink,
            taskName = "stop-flag",
            originColumns = linkedSetOf(OriginColumn("id", "id"))
        )

        // 在回调第 3 行（index=2）之前请求停止：第 2 行已写入，第 3 行 onRow 入口检测到 cancelled 抛出
        source.beforeRow = { i ->
            if (i == 2)
            {
                val stopResponse = service.stop(stopRequest("stop-flag"))
                assertEquals(RunState.STOPPED, stopResponse.state)
            }
        }

        val response = service.start(request)

        assertFalse(response.successful)
        assertEquals(RunState.STOPPED, response.state)
        assertEquals(2, response.count)
        assertEquals(2, sink.committedRows.size)
    }

    @Test
    fun streamingStopDetectedAfterLoop()
    {
        // 模拟“驱动在 cancel 后让 rs.next() 直接返回 false（不抛异常）”：
        // 所有行都回调完了才 stop，靠循环后的二次 cancelled 检查兜底
        val service = LocalExecutorService()
        val source = FakePluginService(
            streaming = true,
            headers = listOf("id"),
            streamRows = (1..3).map { listOf<Any?>(it) }
        )
        val sink = FakePluginService(streaming = true)
        val request = buildRequest(
            source, sink,
            taskName = "stop-afterloop",
            originColumns = linkedSetOf(OriginColumn("id", "id"))
        )
        source.afterRows = { service.stop(stopRequest("stop-afterloop")) }

        val response = service.start(request)

        assertFalse(response.successful)
        assertEquals(RunState.STOPPED, response.state)
        // 全部 3 行其实都已提交，但因为收到停止请求，最终状态仍被判为 STOPPED
        assertEquals(3, response.count)
        assertEquals(3, sink.committedRows.size)
    }

    @Test
    fun streamingDriverAbortAfterCancelIsReportedAsStopped()
    {
        // 第三条 cancel 检测路径：cancel 后底层 fetch 抛 SQLException，走 catch(Exception)+cancelled 逆判定
        val service = LocalExecutorService()
        val source = FakePluginService(
            streaming = true,
            headers = listOf("id"),
            streamRows = (1..5).map { listOf<Any?>(it) }
        )
        val sink = FakePluginService(streaming = true)
        val request = buildRequest(
            source, sink,
            taskName = "stop-abort",
            originColumns = linkedSetOf(OriginColumn("id", "id"))
        )
        source.beforeRow = { i -> if (i == 2) service.stop(stopRequest("stop-abort")) }
        source.throwAtRow = 2

        val response = service.start(request)

        assertFalse(response.successful)
        assertEquals(RunState.STOPPED, response.state)
        // 前 2 行确实已落库
        assertEquals(2, sink.committedRows.size)
        // Phase 2 修正：取消统一携带“已提交行数”（writer.writtenCount()），
        // 不再依赖每 1000 行才更新一次的 rowsAtStop，故这里能正确报告 count=2。
        assertEquals(2, response.count)
    }

    @Test
    fun streamingTimeoutIsReportedAsTimeout()
    {
        // 源端“慢”：每行前 sleep，使整体远超 timeout；看门狗到点后取消，报告 TIMEOUT
        val source = FakePluginService(
            streaming = true,
            headers = listOf("id"),
            streamRows = (1..200).map { listOf<Any?>(it) }
        )
        source.beforeRow = { Thread.sleep(50) }
        val sink = FakePluginService(streaming = true)
        val request = buildRequest(
            source, sink,
            taskName = "timeout-task",
            originColumns = linkedSetOf(OriginColumn("id", "id"))
        ).apply {
            timeout = 1L
        }

        val response = LocalExecutorService().start(request)

        assertFalse(response.successful)
        assertEquals(RunState.TIMEOUT, response.state)
        assertTrue(response.timeout)
    }

    // ---------------------------------------------------------------------
    // Legacy 回退路径（源或汇任一不支持流式）
    // ---------------------------------------------------------------------

    @Test
    fun legacyWithStreamingSinkUsesBatchWriter()
    {
        // 源端不支持流式 -> execute() 全量返回 ObjectNode；汇端支持流式 -> 走 BatchWriter
        val rows = listOf(
            objectNode("id" to 1, "name" to "alice"),
            objectNode("id" to 2, "name" to null)
        )
        val source = FakePluginService(
            streaming = false,
            executeHandler = { rowsResponse(rows) }
        )
        val sink = FakePluginService(streaming = true)
        val request = buildRequest(
            source, sink,
            originColumns = linkedSetOf(
                OriginColumn("full_name", "name"),
                OriginColumn("uid", "id")
            )
        )

        val response = LocalExecutorService().start(request)

        assertTrue(response.successful)
        assertEquals(RunState.SUCCESS, response.state)
        assertEquals(2, response.count)
        assertEquals(
            listOf(
                listOf<Any?>("alice", 1),
                listOf<Any?>(null, 2)
            ),
            sink.committedRows.toList()
        )
    }

    @Test
    fun legacyNonStreamingOutputIsRejected()
    {
        // Phase 3-C：目标端不支持流式写入时，不再静默拼接手写 INSERT（注入/转义隐患），
        // 而是快速失败。（实运行中所有插件经 JDBC 转换后 supportsStreaming==true，此分支仅为兜底。）
        val source = FakePluginService(
            streaming = false,
            executeHandler = { rowsResponse(listOf(objectNode("id" to 1, "name" to "a"))) }
        )
        val sink = FakePluginService(streaming = false)
        val request = buildRequest(
            source, sink,
            originColumns = linkedSetOf(OriginColumn("uid", "id"))
        )

        val response = LocalExecutorService().start(request)

        assertFalse(response.successful)
        assertEquals(RunState.FAILURE, response.state)
        assertNotNull(response.message)
        assertTrue(response.message!!.contains("does not support batch write"))
        // 没有拼接/下发任何 INSERT 语句
        assertTrue(sink.executedSql.isEmpty())
    }

    // ---------------------------------------------------------------------
    // 失败路径
    // ---------------------------------------------------------------------

    @Test
    fun sourceFailureIsReportedAsFailure()
    {
        val source = FakePluginService(
            streaming = false,
            executeHandler = {
                Response.builder().isSuccessful(false).message("boom").build()
            }
        )
        val sink = FakePluginService(streaming = true)
        val request = buildRequest(
            source, sink,
            originColumns = linkedSetOf(OriginColumn("id", "id"))
        )

        val response = LocalExecutorService().start(request)

        assertFalse(response.successful)
        assertEquals(RunState.FAILURE, response.state)
        assertNotNull(response.message)
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private fun buildRequest(
        source: FakePluginService,
        sink: FakePluginService,
        taskName: String = "",
        originColumns: LinkedHashSet<OriginColumn>
    ): ExecutorRequest
    {
        val input = ExecutorConfigure(
            type = "TestInput",
            configure = null,
            supportOptions = emptySet()
        ).apply {
            plugin = source
            query = "SELECT * FROM source"
            database = "target_db"
            table = "target_tbl"
            originConfigure = Configure()
            this.originColumns = originColumns
        }
        val output = ExecutorConfigure(
            type = "TestOutput",
            configure = null,
            supportOptions = emptySet()
        ).apply {
            plugin = sink
            originConfigure = Configure()
        }
        // workHome=null -> 不落任务日志文件；taskName 非空才可被 stop() 定位
        return ExecutorRequest(null, input, output).apply {
            this.taskName = taskName
            this.userName = "tester"
        }
    }

    private fun stopRequest(taskName: String): ExecutorRequest
    {
        val placeholder = ExecutorConfigure(null)
        return ExecutorRequest(taskName, "", placeholder, placeholder)
    }

    private fun objectNode(vararg pairs: Pair<String, Any?>): ObjectNode
    {
        val node = mapper.createObjectNode()
        for ((k, v) in pairs)
        {
            when (v)
            {
                null -> node.putNull(k)
                is Int -> node.put(k, v)
                is Long -> node.put(k, v)
                is Boolean -> node.put(k, v)
                is Double -> node.put(k, v)
                else -> node.put(k, v.toString())
            }
        }
        return node
    }

    private fun rowsResponse(rows: List<ObjectNode>): Response =
        Response.builder()
            .isSuccessful(true)
            .columns(rows.toList<Any>())
            .build()

    private fun countResponse(total: Long): Response =
        Response.builder()
            .isSuccessful(true)
            .columns(listOf<Any>(mapper.createObjectNode().put("total", total)))
            .build()
}
