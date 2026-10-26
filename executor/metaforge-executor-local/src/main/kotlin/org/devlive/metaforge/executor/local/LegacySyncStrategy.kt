package org.devlive.metaforge.executor.local

import com.fasterxml.jackson.databind.node.ObjectNode
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

/**
 * 回退路径：源端不支持流式（executeStream 不可用）时，先用 execute() 全量读取，
 * 再由 BatchWriter 分批写入目标端。
 *
 * 说明：本仓库所有插件都经 “metaforge” JDBC 转换驱动访问（PluginService.type() 默认 JDBC，
 * supportsStreaming()==true），因此实际运行中几乎总是走 [StreamingSyncStrategy]，此回退仅为
 * 未来可能 opt-out 流式的插件保留。目标端若不支持流式则直接失败——不再退回到不安全的手拼 SQL。
 */
@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION", "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"])
internal class LegacySyncStrategy : SyncStrategy
{
    override fun sync(context: SyncContext): Long
    {
        val inputPlugin = context.inputPlugin
        val inputConfigure = context.inputConfigure
        val query = context.query
        val outputPlugin = context.outputPlugin
        val outputConfigure = context.outputConfigure
        val database = context.database
        val table = context.table
        val batchSize = context.batchSize
        val taskLog = context.taskLog
        val totalCount = context.totalCount
        val progressListener = context.progressListener
        val token = context.handle.cancellation

        taskLog.info("Legacy sync start: target=`{}`.`{}` batchSize={}", database, table, batchSize)

        // 目标端必须支持批量/流式写入。全部插件经 JDBC 转换后都满足；这里显式兜底，
        // 避免历史上“双端非流式”时静默拼接 INSERT 字符串（存在注入与类型/转义隐患）。
        if (!outputPlugin.supportsStreaming())
        {
            throw IllegalStateException(
                "Output plugin '${outputPlugin.name()}' does not support batch write (supportsStreaming=false); " +
                        "the local executor no longer falls back to hand-built SQL. Use a JDBC/streaming-capable target."
            )
        }

        val startNanos = System.nanoTime()
        val inputResult = inputPlugin.execute(inputConfigure, query)
        if (inputResult.isSuccessful != true)
        {
            throw RuntimeException(inputResult.message ?: "Input plugin failed")
        }
        val rows = inputResult.columns ?: return 0L
        taskLog.info("Legacy sync: source materialized {} rows", rows.size)
        // 全量路径已经能拿到精确总数，覆盖 pre-count 的估算
        val effectiveTotal = if (totalCount >= 0) totalCount else rows.size.toLong()

        val targetColumns = context.targetColumns
        val sourceKeys = context.sourceKeys
        var read = 0L
        val writer = outputPlugin.openBatchWriter(
            outputConfigure, database, table, targetColumns, batchSize
        )
        writer.runCancelable(token) { w ->
            for (item in rows)
            {
                token.throwIfCancelled(w.writtenCount())
                val node = item as? ObjectNode ?: continue
                val projected = ArrayList<Any?>(sourceKeys.size)
                for (key in sourceKeys)
                {
                    projected.add(ValueCodec.jsonNodeToJdbcValue(node.get(key)))
                }
                w.addRow(projected)
                read ++
                if (read % PROGRESS_INTERVAL == 0L)
                {
                    taskLog.info("Legacy sync progress: read={} committed={}", read, w.writtenCount())
                    progressListener?.onProgress(w.writtenCount(), effectiveTotal)
                }
            }
        }
        val committed = writer.writtenCount()
        val totalSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0
        taskLog.info("Legacy sync done: read={} committed={} elapsed={}s", read, committed, "%.1f".format(totalSeconds))
        return committed
    }
}
