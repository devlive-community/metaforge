package org.devlive.metaforge.executor.local

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.spi.adapter.BatchWriter
import org.devlive.metaforge.spi.adapter.RowCallback

/**
 * 流式路径：源端 fetchSize 拉取，目标端 PreparedStatement 批量写。
 * 全程不在 JVM 内物化整个结果集。
 *
 * 仅当源与汇【都】支持流式时选用。取消处理统一由 [runCancelable] 收敛，
 * 返回值与取消时上报的行数均为“已提交行数”（writer.writtenCount()）。
 */
@SuppressFBWarnings(value = ["RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"])
internal class StreamingSyncStrategy : SyncStrategy
{
    override fun sync(context: SyncContext): Long
    {
        val inputPlugin = context.inputPlugin
        val inputConfigure = context.inputConfigure
        val query = context.query
        val fetchSize = context.fetchSize
        val outputPlugin = context.outputPlugin
        val outputConfigure = context.outputConfigure
        val database = context.database
        val table = context.table
        val targetColumns = context.targetColumns
        val sourceKeys = context.sourceKeys
        val batchSize = context.batchSize
        val taskLog = context.taskLog
        val totalCount = context.totalCount
        val progressListener = context.progressListener
        val token = context.handle.cancellation

        taskLog.info(
                "Streaming sync start: target=`{}`.`{}` columns={} fetchSize={} batchSize={} total={}",
                database, table, targetColumns.size, fetchSize, batchSize, totalCount
        )
        val startNanos = System.nanoTime()
        var read = 0L
        val writer: BatchWriter = outputPlugin.openBatchWriter(
            outputConfigure, database, table, targetColumns, batchSize
        )
        writer.runCancelable(token) { w ->
            // onSchema 时把 sourceKeys 一次性解析成列下标数组，避免每行做 HashMap 查找
            var keyIndexes = IntArray(0)
            inputPlugin.executeStream(inputConfigure, query, fetchSize, object : RowCallback
            {
                override fun onSchema(headers: List<String>, types: List<String>)
                {
                    val indexByHeader = HashMap<String, Int>(headers.size * 2)
                    headers.forEachIndexed { i, h -> indexByHeader[h.lowercase()] = i }
                    keyIndexes = IntArray(sourceKeys.size) { k ->
                        indexByHeader[sourceKeys[k].lowercase()]
                            ?: throw IllegalStateException("Source column '${sourceKeys[k]}' not found in query result")
                    }
                    taskLog.info("Streaming sync: source returned headers={}", headers)
                }

                override fun onStatement(statement: java.sql.Statement)
                {
                    context.handle.sourceStatement = statement
                }

                override fun onRow(row: List<Any?>)
                {
                    // 路径 1：源端还在吐行时检测到取消，立即中断（携带已提交行数）
                    token.throwIfCancelled(w.writtenCount())
                    val projected = ArrayList<Any?>(keyIndexes.size)
                    for (idx in keyIndexes)
                    {
                        projected.add(row[idx])
                    }
                    w.addRow(projected)
                    read ++
                    if (read % PROGRESS_INTERVAL == 0L)
                    {
                        val seconds = (System.nanoTime() - startNanos) / 1_000_000_000.0
                        val rps = if (seconds > 0) (read / seconds).toLong() else 0L
                        taskLog.info(
                                "Streaming sync progress: read={} committed={} elapsed={}s rps={}",
                                read, w.writtenCount(), "%.1f".format(seconds), rps
                        )
                        progressListener?.onProgress(w.writtenCount(), totalCount)
                    }
                }
            })
        }
        val committed = writer.writtenCount()
        val totalSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0
        taskLog.info(
                "Streaming sync done: read={} committed={} elapsed={}s",
                read, committed, "%.1f".format(totalSeconds)
        )
        return committed
    }
}
