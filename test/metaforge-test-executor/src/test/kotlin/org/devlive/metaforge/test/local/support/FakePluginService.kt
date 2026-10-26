package org.devlive.metaforge.test.local.support

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.PluginType
import org.devlive.metaforge.spi.adapter.BatchWriter
import org.devlive.metaforge.spi.adapter.RowCallback
import org.devlive.metaforge.spi.model.Configure
import org.devlive.metaforge.spi.model.Response
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.sql.Statement
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * 纯内存的 PluginService 假实现，专门用于 LocalExecutorService 的特性测试（characterization test）。
 * 不连接任何真实数据库，把流式读、批量写、execute()、以及 JDBC Statement.cancel() 全部做成可控 / 可观测。
 *
 * A network-free fake PluginService used to pin down the current behaviour of LocalExecutorService.
 * Streaming reads, batch writes, execute() and JDBC Statement.cancel() are all controllable and observable.
 *
 * 作为“源端”（source）时使用：
 *   - streaming=true  -> executeStream() 会把 [streamRows] 按 [headers] 顺序逐行回调
 *   - streaming=false -> 走 execute()，由 [executeHandler] 决定返回什么（用于 legacy 全量读 & pre-count）
 *
 * 作为“汇端”（sink）时使用：
 *   - streaming=true  -> openBatchWriter() 返回 [CapturingBatchWriter]，把落库行收集到 [committedRows]
 *   - streaming=false -> 走 execute()，[executedSql] 收集所有拼出来的 INSERT 语句
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
class FakePluginService(
    private val streaming: Boolean = true,
    private val headers: List<String> = emptyList(),
    private val streamRows: List<List<Any?>> = emptyList(),
    private val types: List<String> = headers.map { "VARCHAR" },
    private val executeHandler: ((String) -> Response)? = null
) : PluginService
{
    /** 汇端：真正“落库”的行，按写入顺序（已投影为目标列顺序） */
    val committedRows: CopyOnWriteArrayList<List<Any?>> = CopyOnWriteArrayList()

    /** 汇端：本 fake 打开过的所有 batch writer，便于断言 flush / writtenCount */
    val batchWriters: CopyOnWriteArrayList<CapturingBatchWriter> = CopyOnWriteArrayList()

    /** 汇端：execute() 收到的所有 SQL（legacy 双端非流式路径的 INSERT 字符串） */
    val executedSql: CopyOnWriteArrayList<String> = CopyOnWriteArrayList()

    /** 源端：底层 Statement.cancel() 被调用的次数 */
    val cancelCount: AtomicInteger = AtomicInteger(0)

    /** 源端：在回调第 i 行之前触发的钩子，测试可在这里调用 service.stop() 模拟“流式中途停止” */
    @Volatile
    var beforeRow: ((Int) -> Unit)? = null

    /** 源端：所有行回调完成之后触发的钩子，用于模拟“驱动在 cancel 后让 rs.next() 直接返回 false” */
    @Volatile
    var afterRows: (() -> Unit)? = null

    /** 源端：在即将回调第 [throwAtRow] 行时抛异常，模拟“驱动 cancel 后 fetch 抛 SQLException” */
    @Volatile
    var throwAtRow: Int = -1

    override fun supportsStreaming(): Boolean = streaming

    override fun type(): PluginType = if (streaming) PluginType.JDBC else PluginType.HTTP

    override fun executeStream(configure: Configure, content: String, fetchSize: Int, callback: RowCallback)
    {
        callback.onStatement(newFakeStatement())
        callback.onSchema(headers, types)
        for (i in streamRows.indices)
        {
            beforeRow?.invoke(i)
            if (throwAtRow == i)
            {
                throw RuntimeException("Simulated driver abort after cancel")
            }
            callback.onRow(streamRows[i])
        }
        afterRows?.invoke()
    }

    override fun openBatchWriter(
        configure: Configure,
        database: String,
        table: String,
        columns: List<String>,
        batchSize: Int
    ): BatchWriter
    {
        val writer = CapturingBatchWriter(columns, batchSize, committedRows)
        batchWriters.add(writer)
        return writer
    }

    override fun execute(configure: Configure, content: String): Response
    {
        executedSql.add(content)
        return executeHandler?.invoke(content)
            ?: Response.builder()
                .isSuccessful(true)
                .columns(emptyList<Any>())
                .build()
    }

    /**
     * 用动态代理造一个只实现了 isClosed()/cancel() 的假 Statement，
     * 其余方法返回各自类型的零值，够 LocalExecutorService.stop() 使用。
     */
    private fun newFakeStatement(): Statement
    {
        val handler = InvocationHandler { _, method, _ ->
            when (method.name)
            {
                "isClosed" -> false
                "cancel" ->
                {
                    cancelCount.incrementAndGet()
                    null
                }

                else -> when (method.returnType)
                {
                    java.lang.Boolean.TYPE -> false
                    java.lang.Integer.TYPE -> 0
                    java.lang.Long.TYPE -> 0L
                    else -> null
                }
            }
        }
        return Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(Statement::class.java),
            handler
        ) as Statement
    }
}

/**
 * 内存版 BatchWriter：模拟 JdbcBatchWriter 的“攒够 batchSize 才 flush、close 时 flush 剩余”语义，
 * writtenCount() 只统计已 flush（已提交）的行数，与真实实现保持一致。
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP2"])
class CapturingBatchWriter(
    private val columns: List<String>,
    private val batchSize: Int,
    private val sink: MutableList<List<Any?>>
) : BatchWriter
{
    private val buffer: MutableList<List<Any?>> = ArrayList()
    private var committed: Long = 0L

    /** 累计调用 flush() 的次数，便于断言 batch 切分行为 */
    var flushCount: Int = 0
        private set

    override fun addRow(row: List<*>)
    {
        require(row.size == columns.size) {
            "Row size ${row.size} does not match column count ${columns.size}"
        }
        buffer.add(ArrayList(row))
        if (buffer.size >= batchSize)
        {
            flush()
        }
    }

    override fun writtenCount(): Long = committed

    override fun close()
    {
        if (buffer.isNotEmpty())
        {
            flush()
        }
    }

    private fun flush()
    {
        sink.addAll(buffer)
        committed += buffer.size
        buffer.clear()
        flushCount++
    }
}
