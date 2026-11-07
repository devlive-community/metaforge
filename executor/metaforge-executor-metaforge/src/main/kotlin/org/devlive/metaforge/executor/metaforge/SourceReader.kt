package org.devlive.metaforge.executor.metaforge

import com.fasterxml.jackson.databind.node.ObjectNode
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.configure.TotalSource
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.adapter.RowCallback
import org.devlive.metaforge.spi.model.RuntimeConfigure
import org.slf4j.Logger

/** 接收一行已投影到目标列顺序的数据 / receives one row already projected to the target column order */
internal fun interface RowSink
{
    fun accept(row: List<Any?>)
}

/**
 * 从源端读取一段数据并逐行交给 [RowSink]。在调用线程中运行，以便 stop() 能直接 cancel 源端 Statement。
 * Reads rows from the source and hands each one to the [RowSink], on the calling thread so stop()
 * can cancel the source statement directly.
 */
internal interface SourceReader
{
    fun read(sink: RowSink)
}

/**
 * 流式读取：源端按 fetchSize 拉取，不在 JVM 内物化整个结果集。onSchema 时把源列名解析成下标，
 * 避免每行做 HashMap 查找。
 *
 * Streaming read with the source's fetch size; source column names resolve to indexes once in onSchema.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP2"])
internal class StreamingSourceReader(
    private val plugin: PluginService,
    private val configure: RuntimeConfigure,
    private val query: String,
    private val fetchSize: Int,
    private val sourceKeys: List<String>,
    private val handle: TaskHandle,
    private val taskLog: Logger
) : SourceReader
{
    override fun read(sink: RowSink)
    {
        var keyIndexes = IntArray(0)
        var running: java.sql.Statement? = null
        try
        {
            plugin.executeStream(configure, query, fetchSize, object : RowCallback
            {
                override fun onSchema(headers: List<String>, types: List<String>)
                {
                    val indexByHeader = HashMap<String, Int>(headers.size * 2)
                    headers.forEachIndexed { i, h -> indexByHeader[h.lowercase()] = i }
                    keyIndexes = IntArray(sourceKeys.size) { k ->
                        indexByHeader[sourceKeys[k].lowercase()]
                            ?: throw IllegalStateException("Source column '${sourceKeys[k]}' not found in query result")
                    }
                    taskLog.info("Streaming read: source returned headers={}", headers)
                }

                override fun onStatement(statement: java.sql.Statement)
                {
                    running = statement
                    handle.addStatement(statement)
                }

                override fun onRow(row: List<Any?>)
                {
                    val projected = ArrayList<Any?>(keyIndexes.size)
                    for (idx in keyIndexes)
                    {
                        projected.add(row[idx])
                    }
                    sink.accept(projected)
                }
            })
        }
        finally
        {
            running?.let { handle.removeStatement(it) }
        }
    }
}

/**
 * 回退读取：源端不支持流式时用 execute() 全量读取后逐行交出。
 * 说明：所有插件都经 JDBC 转换驱动访问（supportsStreaming()==true），实际几乎总走流式，此回退仅为兜底。
 *
 * Fallback for sources without streaming: execute() materializes the result, then rows are handed out.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP2"])
internal class MaterializedSourceReader(
    private val plugin: PluginService,
    private val configure: RuntimeConfigure,
    private val query: String,
    private val sourceKeys: List<String>,
    private val metrics: MetricsCollector,
    private val totalKnown: Boolean,
    private val taskLog: Logger
) : SourceReader
{
    @SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
    override fun read(sink: RowSink)
    {
        val result = plugin.execute(configure, query)
        if (result.isSuccessful != true)
        {
            throw RuntimeException(result.message ?: "Input plugin failed")
        }
        val rows = result.columns ?: return
        taskLog.info("Materialized read: source returned {} rows", rows.size)
        // 全量读取已经拿到精确总数；pre-count 已给出时以它为准
        if (!totalKnown)
        {
            metrics.setTotal(rows.size.toLong(), TotalSource.MATERIALIZED)
        }
        for (item in rows)
        {
            val node = item as? ObjectNode ?: continue
            val projected = ArrayList<Any?>(sourceKeys.size)
            for (key in sourceKeys)
            {
                projected.add(ValueCodec.jsonNodeToJdbcValue(node.get(key)))
            }
            sink.accept(projected)
        }
    }
}

/**
 * 从读出的行里采样预览，供工作流展示：最多 [limit] 行、总量约 64KB、单值最多 512 字符，二进制不展示。
 * 多个分片可能并行读取，因此加锁；采满后读取路径只多一次 volatile 判断。
 *
 * Samples rows for the workflow preview, bounded by rows, total size and value length. Splits may be
 * read in parallel, hence the lock; once full it costs readers one volatile check.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP2"])
internal class PreviewSampler(private val limit: Int, private val columns: List<String>)
{
    private val samples = ArrayList<Map<String, Any?>>()
    private var bytes = 0

    @Volatile
    private var full = limit <= 0

    fun wrap(reader: SourceReader): SourceReader = object : SourceReader
    {
        override fun read(sink: RowSink)
        {
            reader.read { row ->
                sink.accept(row)
                if (!full)
                {
                    sample(row)
                }
            }
        }
    }

    @Synchronized
    fun rows(): List<Map<String, Any?>> = ArrayList(samples)

    @Synchronized
    private fun sample(row: List<Any?>)
    {
        if (samples.size >= limit || bytes >= MAX_BYTES)
        {
            full = true
            return
        }
        val sample = linkedMapOf<String, Any?>()
        columns.forEachIndexed { index, column ->
            if (bytes < MAX_BYTES)
            {
                val safe = when (val value = row.getOrNull(index))
                {
                    null, is Number, is Boolean -> value
                    is ByteArray -> "[binary]"
                    else -> value.toString().take(MAX_VALUE_LENGTH)
                }
                bytes += column.length + (safe?.toString()?.length ?: 4)
                sample[column] = safe
            }
        }
        samples.add(sample)
    }

    private companion object
    {
        const val MAX_BYTES = 65536
        const val MAX_VALUE_LENGTH = 512
    }
}
