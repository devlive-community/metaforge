package org.devlive.metaforge.executor.metaforge

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.ExecutorService
import org.devlive.metaforge.executor.common.RunState
import org.devlive.metaforge.executor.configure.ExecutorRequest
import org.devlive.metaforge.executor.configure.ExecutorResponse
import org.devlive.metaforge.executor.configure.TotalSource
import org.devlive.metaforge.executor.workflow.WorkflowPlan
import org.devlive.metaforge.lib.logger.LoggerExecutor
import org.devlive.metaforge.lib.logger.logback.LogbackExecutor
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.RuntimeConfigure
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ScheduledFuture

/**
 * MetaForge 执行器：在 MetaForge 进程内把“源查询结果”搬运到“目标表”，按 taskName 支持运行时停止与超时。
 *
 * 一次任务分阶段执行：PREPARE（解析参数）→ PRECOUNT（可选，统计总行数）→ PLAN（规划分片）→
 * TRANSFER（经 [Channel] 读写流水线搬运）。源端读取由 [SourceReader] 完成（流式或全量回退），
 * 指标由 [MetricsCollector] 采集、[MetricsPublisher] 定时上报；活跃任务与后台线程委托给 [TaskRegistry]。
 *
 * Runs a sync in stages (PREPARE, optional PRECOUNT, PLAN, TRANSFER) through a [Channel] pipeline,
 * with metrics collected by [MetricsCollector] and reported on a timer by [MetricsPublisher].
 */
@SuppressFBWarnings(value = ["RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
class MetaForgeExecutorService : ExecutorService
{
    private val log = LoggerFactory.getLogger(MetaForgeExecutorService::class.java)

    override fun start(request: ExecutorRequest): ExecutorResponse
    {
        val response = ExecutorResponse()
        val loggerExecutor: LoggerExecutor<*>? = newTaskLogger(request)
        val taskLog: Logger = loggerExecutor?.getLogger() ?: log
        val handle = TaskHandle()
        handle.taskLog = taskLog
        val options = request.options
        val metrics = MetricsCollector()
        val publisher = MetricsPublisher(metrics, request.metricsListener, taskLog)
        val stages = StageTracker(metrics)
        if (request.taskName.isNotBlank())
        {
            TaskRegistry.register(request.taskName, handle)
        }
        // 整任务超时看门狗：timeout<=0 表示不限时（默认）。到点后触发一次“超时取消”。
        val timeoutFuture: ScheduledFuture<*>? =
            if (request.timeout > 0)
            {
                TaskRegistry.scheduleTimeout(request.timeout)
                {
                    taskLog.warn("Task [ {} ] exceeded timeout of {}s, cancelling", request.taskName, request.timeout)
                    requestCancel(handle, request.taskName, CancelReason.TIMEOUT)
                }
            }
            else
            {
                null
            }
        // 指标按时间定时上报，与读写线程解耦 / metrics are reported on a timer, off the read/write threads
        val metricsInterval = (options["metricsIntervalMs"]?.toLongOrNull() ?: DEFAULT_METRICS_INTERVAL_MS)
            .coerceAtLeast(MIN_METRICS_INTERVAL_MS)
        val metricsFuture: ScheduledFuture<*> = TaskRegistry.scheduleMetrics(metricsInterval) { publisher.tick() }
        try
        {
            taskLog.info("MetaForge executor task starting: task={} user={}", request.taskName, request.userName)
            stages.begin(STAGE_PREPARE)

            val input = request.input
            val output = request.output
            val inputPlugin = input.plugin ?: throw IllegalArgumentException("Input plugin is null")
            val outputPlugin = output.plugin ?: throw IllegalArgumentException("Output plugin is null")
            val inputConfigure = input.originConfigure ?: throw IllegalArgumentException("Input configure is null")
            val outputConfigure = output.originConfigure ?: throw IllegalArgumentException("Output configure is null")
            // MetaForge 约定：目标库 / 目标表 由调用方放在 input 上（见 DataSetServiceImpl）
            val database = input.database ?: throw IllegalArgumentException("Target database is null")
            val table = input.table ?: throw IllegalArgumentException("Target table is null")
            val query = input.query ?: throw IllegalArgumentException("Input query is null")

            val originColumns = input.originColumns.toList()
            if (originColumns.isEmpty())
            {
                throw IllegalArgumentException("Input column mapping is empty")
            }

            // MetaForge 专属可调项从 options 读取（调用方把 executor 的 effective 配置塞进 options）
            val fetchSize = (options["fetchSize"]?.toIntOrNull()?.takeIf { it > 0 }) ?: DEFAULT_FETCH_SIZE
            val batchSize = (options["batchSize"]?.toIntOrNull()?.takeIf { it > 0 }) ?: DEFAULT_BATCH_SIZE
            val queueCapacity = (options["queueCapacity"]?.toIntOrNull()?.takeIf { it > 0 }) ?: DEFAULT_QUEUE_CAPACITY
            val preCountEnabled = options["preCount"]?.toBoolean() ?: false

            taskLog.info(
                    "Resolved input plugin={} output plugin={} streaming={}/{} fetchSize={} batchSize={} queueCapacity={} preCount={}",
                    inputPlugin.name(), outputPlugin.name(),
                    inputPlugin.supportsStreaming(), outputPlugin.supportsStreaming(),
                    fetchSize, batchSize, queueCapacity, preCountEnabled
            )
            taskLog.info("Source query: {}", query)

            // 目标端必须支持批量写入。全部插件经 JDBC 转换后都满足；这里显式兜底，
            // 避免历史上“双端非流式”时静默拼接 INSERT 字符串（存在注入与类型/转义隐患）。
            if (!outputPlugin.supportsStreaming())
            {
                throw IllegalStateException(
                    "Output plugin '${outputPlugin.name()}' does not support batch write (supportsStreaming=false); " +
                            "the MetaForge executor no longer falls back to hand-built SQL. Use a JDBC/streaming-capable target."
                )
            }
            stages.end(STAGE_PREPARE)

            // 可选：先跑 SELECT COUNT(*) 拿到源端总行数，作为进度分母
            // Optional: pre-count to populate the total row count used for progress percentage
            var totalKnown = false
            if (preCountEnabled)
            {
                stages.begin(STAGE_PRECOUNT)
                val totalCount = preCount(inputPlugin, inputConfigure, query, taskLog)
                stages.end(STAGE_PRECOUNT)
                if (totalCount >= 0)
                {
                    totalKnown = true
                    taskLog.info("Pre-count: source total rows = {}", totalCount)
                    metrics.setTotal(totalCount, TotalSource.PRECOUNT)
                }
                // 立即上报一次，让上层马上知道总行数
                publisher.tick()
            }
            else
            {
                metrics.skipStage(STAGE_PRECOUNT)
            }

            // 目前整段查询作为单个分片 / the whole query is one split for now
            stages.begin(STAGE_PLAN)
            val sourceKeys = originColumns.map { it.original }
            val reader: SourceReader =
                if (inputPlugin.supportsStreaming())
                {
                    StreamingSourceReader(inputPlugin, inputConfigure, query, fetchSize, sourceKeys, handle, taskLog)
                }
                else
                {
                    MaterializedSourceReader(inputPlugin, inputConfigure, query, sourceKeys, metrics, totalKnown, taskLog)
                }
            metrics.channels = 1
            metrics.queueCapacity = queueCapacity
            stages.end(STAGE_PLAN)

            stages.begin(STAGE_TRANSFER)
            taskLog.info("Transfer start: target=`{}`.`{}` columns={} batchSize={} queueCapacity={}",
                database, table, originColumns.size, batchSize, queueCapacity)
            val writer = outputPlugin.openBatchWriter(outputConfigure, database, table, originColumns.map { it.name }, batchSize)
            val channel = Channel(1, request.taskName, batchSize, queueCapacity, metrics, handle.cancellation, taskLog)
            val previewLimit = (options["previewRows"]?.toIntOrNull() ?: 0).coerceIn(0, 50)
            val preview = mutableListOf<Map<String, Any?>>()
            var previewBytes = 0
            val sampledReader = object : SourceReader
            {
                override fun read(sink: RowSink)
                {
                    reader.read { row ->
                        sink.accept(row)
                        if (preview.size < previewLimit && previewBytes < 65536)
                        {
                            val sample = linkedMapOf<String, Any?>()
                            originColumns.forEachIndexed { index, column ->
                                if (previewBytes < 65536)
                                {
                                    val value = row.getOrNull(index)
                                    val safe = when (value)
                                    {
                                        null, is Number, is Boolean -> value
                                        is ByteArray -> "[binary]"
                                        else -> value.toString().take(512)
                                    }
                                    previewBytes += column.name.length + (safe?.toString()?.length ?: 4)
                                    sample[column.name] = safe
                                }
                            }
                            preview.add(sample)
                        }
                    }
                }
            }
            val written = channel.run(sampledReader, writer)
            if (previewLimit > 0)
            {
                response.preview = preview
            }
            stages.end(STAGE_TRANSFER)

            response.count = if (written > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else written.toInt()
            response.successful = true
            response.state = RunState.SUCCESS
            taskLog.info("MetaForge executor task completed: rows={} state=SUCCESS", written)
        }
        catch (ex: TaskCancelledException)
        {
            stages.fail("CANCELLED")
            applyCancelled(response, handle, request, ex.processed, taskLog)
        }
        catch (ex: Exception)
        {
            // 正常情况下取消都会被通道归一成 TaskCancelledException（见上）。
            // 这里只兜底“取消发生在写入循环之外”（如 openBatchWriter / 全量读取阶段就被取消）：
            // 此时尚无已提交行数，count 记 0。
            if (handle.cancellation.isCancelled)
            {
                stages.fail("CANCELLED")
                applyCancelled(response, handle, request, 0L, taskLog)
            }
            else
            {
                stages.fail("FAILED")
                taskLog.error("MetaForge executor failed", ex)
                response.successful = false
                response.state = RunState.FAILURE
                response.message = ex.message
            }
        }
        finally
        {
            metricsFuture.cancel(false)
            // 最终上报一次：成功时进度 100%；之后不再有定时上报
            response.metrics = publisher.close(response.successful)
            // 任务已结束：撤掉尚未触发的超时看门狗，避免事后误触发
            timeoutFuture?.cancel(false)
            if (request.taskName.isNotBlank())
            {
                TaskRegistry.unregister(request.taskName, handle)
            }
            loggerExecutor?.destroy()
        }
        return response
    }

    private fun newTaskLogger(request: ExecutorRequest): LoggerExecutor<*>?
    {
        val workHome = request.workHome
        val taskName = request.taskName
        if (workHome.isNullOrBlank() || taskName.isBlank())
        {
            return null
        }
        return try
        {
            val dir = java.io.File(workHome)
            if (!dir.exists())
            {
                dir.mkdirs()
            }
            LogbackExecutor(workHome, "$taskName.log")
        }
        catch (ex: Exception)
        {
            log.warn("Create task logger at {} failed: {}", workHome, ex.message)
            null
        }
    }

    override fun compile(plan: WorkflowPlan): ExecutorRequest = MetaForgeWorkflowCompiler.compile(plan)

    override fun stop(request: ExecutorRequest): ExecutorResponse
    {
        val taskName = request.taskName
        if (taskName.isBlank())
        {
            return ExecutorResponse(false, false, RunState.FAILURE, "taskName is required")
        }
        val handle = TaskRegistry.find(taskName)
        if (handle == null)
        {
            return ExecutorResponse(false, false, RunState.FAILURE, "Task [ $taskName ] is not running on this node")
        }
        requestCancel(handle, taskName, CancelReason.USER)
        return ExecutorResponse(false, true, RunState.STOPPED, null)
    }

    /**
     * 触发取消：设标志位（纯内存、立即完成）+ 异步 cancel 源端 Statement。
     * 用户停止与超时看门狗共用此逻辑，仅取消原因不同。
     *
     * Statement.cancel() 实现里通常会新开一个 JDBC 连接发 KILL QUERY，
     * 如果源 DB 网络异常会阻塞调用线程，所以放到独立线程执行。
     */
    private fun requestCancel(handle: TaskHandle, taskName: String, reason: CancelReason)
    {
        handle.cancellation.cancel(reason)
        val what = if (reason == CancelReason.TIMEOUT) "Timeout cancel" else "Cancel"
        log.info("{} requested for task [ {} ]", what, taskName)
        handle.taskLog?.warn("{} requested for task [ {} ]", what, taskName)
        val stmt = handle.sourceStatement
        if (stmt != null)
        {
            TaskRegistry.submitCancel {
                try
                {
                    if (!stmt.isClosed)
                    {
                        stmt.cancel()
                        handle.taskLog?.warn("Source JDBC statement cancelled for task [ {} ]", taskName)
                    }
                }
                catch (ex: Exception)
                {
                    log.warn("Cancel source statement failed: {}", ex.message)
                    handle.taskLog?.warn("Cancel source statement failed: {}", ex.message)
                }
            }
        }
    }

    /**
     * 把“取消”落到 response 上：按取消原因区分 STOPPED（用户停止）与 TIMEOUT（超时）。
     * processed 为已提交行数（committed）。
     */
    private fun applyCancelled(
        response: ExecutorResponse,
        handle: TaskHandle,
        request: ExecutorRequest,
        processed: Long,
        taskLog: Logger
    )
    {
        response.count = if (processed > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else processed.toInt()
        response.successful = false
        if (handle.cancellation.reason == CancelReason.TIMEOUT)
        {
            taskLog.warn("MetaForge executor task timed out: rows={} task={}", processed, request.taskName)
            response.state = RunState.TIMEOUT
            response.timeout = true
            response.message = "Timed out after ${request.timeout}s"
        }
        else
        {
            taskLog.warn("MetaForge executor task stopped by user: rows={} task={}", processed, request.taskName)
            response.state = RunState.STOPPED
            response.message = "Stopped by user"
        }
    }

    /**
     * 跑 SELECT COUNT(*) FROM (userQuery) 估算总行数。失败时返回 -1，不影响后续流程。
     * Run SELECT COUNT(*) over the user's query. On failure returns -1 — progress just lacks the denominator.
     */
    private fun preCount(plugin: PluginService, configure: RuntimeConfigure, query: String, taskLog: Logger): Long
    {
        return try
        {
            val trimmed = query.trim().trimEnd(';')
            val sql = "SELECT COUNT(*) AS total FROM ($trimmed) metaforge_precount_t"
            taskLog.info("Pre-count starting: {}", sql)
            val t0 = System.nanoTime()
            val result = plugin.execute(configure, sql)
            val elapsedMs = (System.nanoTime() - t0) / 1_000_000
            taskLog.info("Pre-count returned in {} ms", elapsedMs)
            if (result.isSuccessful != true)
            {
                taskLog.warn("Pre-count failed, will run without total: {}", result.message)
                return -1L
            }
            val rows = result.columns ?: return -1L
            if (rows.isEmpty()) return -1L
            val first = rows[0]
            when (first)
            {
                is com.fasterxml.jackson.databind.node.ObjectNode -> first.get("total")?.asLong(-1L) ?: -1L
                is List<*> -> (first.firstOrNull() as? Number)?.toLong() ?: -1L
                is Number -> first.toLong()
                else -> -1L
            }
        }
        catch (ex: Exception)
        {
            taskLog.warn("Pre-count threw, will run without total: {}", ex.message)
            -1L
        }
    }

    companion object
    {
        private const val DEFAULT_FETCH_SIZE = 1000
        private const val DEFAULT_BATCH_SIZE = 1000
        private const val DEFAULT_QUEUE_CAPACITY = 4
        private const val DEFAULT_METRICS_INTERVAL_MS = 1000L
        private const val MIN_METRICS_INTERVAL_MS = 200L

        const val STAGE_PREPARE = "PREPARE"
        const val STAGE_PRECOUNT = "PRECOUNT"
        const val STAGE_PLAN = "PLAN"
        const val STAGE_TRANSFER = "TRANSFER"
    }
}
