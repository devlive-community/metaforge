package org.devlive.metaforge.executor.local

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.ExecutorService
import org.devlive.metaforge.executor.common.RunState
import org.devlive.metaforge.executor.configure.ExecutorRequest
import org.devlive.metaforge.executor.configure.ExecutorResponse
import org.devlive.metaforge.lib.logger.LoggerExecutor
import org.devlive.metaforge.lib.logger.logback.LogbackExecutor
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.Configure
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ScheduledFuture

/**
 * 本地执行器：在 MetaForge 进程内把“源查询结果”搬运到“目标表”，支持流式与回退两种同步策略、
 * 以及按 taskName 的运行时停止。
 *
 * 编排职责保留在这里；具体搬运逻辑委托给 [SyncStrategy]（[StreamingSyncStrategy] / [LegacySyncStrategy]），
 * 活跃任务与取消线程池委托给 [TaskRegistry]，值/字面量转换委托给 [ValueCodec]。
 */
@SuppressFBWarnings(value = ["RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
class LocalExecutorService : ExecutorService
{
    private val log = LoggerFactory.getLogger(LocalExecutorService::class.java)

    override fun start(request: ExecutorRequest): ExecutorResponse
    {
        val response = ExecutorResponse()
        val loggerExecutor: LoggerExecutor<*>? = newTaskLogger(request)
        val taskLog: Logger = loggerExecutor?.getLogger() ?: log
        val handle = TaskHandle()
        handle.taskLog = taskLog
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
        try
        {
            taskLog.info("Local executor task starting: task={} user={}", request.taskName, request.userName)

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

            // Local 专属可调项从 options 读取（调用方把 executor 的 effective 配置塞进 options）
            val options = request.options
            val fetchSize = (options["fetchSize"]?.toIntOrNull()?.takeIf { it > 0 }) ?: DEFAULT_FETCH_SIZE
            val batchSize = (options["batchSize"]?.toIntOrNull()?.takeIf { it > 0 }) ?: DEFAULT_BATCH_SIZE
            val preCountEnabled = options["preCount"]?.toBoolean() ?: false
            val progressListener = request.progressListener

            taskLog.info(
                    "Resolved input plugin={} output plugin={} streaming={}/{} fetchSize={} batchSize={} preCount={}",
                    inputPlugin.name(), outputPlugin.name(),
                    inputPlugin.supportsStreaming(), outputPlugin.supportsStreaming(),
                    fetchSize, batchSize, preCountEnabled
            )
            taskLog.info("Source query: {}", query)

            // 可选：先跑 SELECT COUNT(*) 拿到源端总行数，作为进度分母
            // Optional: pre-count to populate the total row count used for progress percentage
            val totalCount: Long = if (preCountEnabled) preCount(inputPlugin, inputConfigure, query, taskLog) else -1L
            if (totalCount >= 0)
            {
                taskLog.info("Pre-count: source total rows = {}", totalCount)
                // 报告 0 进度，让上层立即知道 total
                progressListener?.onProgress(0L, totalCount)
            }

            val context = SyncContext(
                inputPlugin, inputConfigure, query, fetchSize,
                outputPlugin, outputConfigure, database, table,
                originColumns,
                originColumns.map { it.name },
                originColumns.map { it.original },
                batchSize, taskLog, totalCount, progressListener, handle
            )

            val strategy: SyncStrategy =
                if (inputPlugin.supportsStreaming() && outputPlugin.supportsStreaming())
                {
                    StreamingSyncStrategy()
                }
                else
                {
                    LegacySyncStrategy()
                }

            val written = strategy.sync(context)

            response.count = if (written > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else written.toInt()
            response.successful = true
            response.state = RunState.SUCCESS
            // 最终上报一次：确保 100%
            progressListener?.onProgress(written, if (totalCount >= 0) totalCount else written)
            taskLog.info("Local executor task completed: rows={} state=SUCCESS", written)
        }
        catch (ex: TaskCancelledException)
        {
            applyCancelled(response, handle, request, ex.processed, taskLog)
        }
        catch (ex: Exception)
        {
            // 正常情况下取消都会被策略归一成 TaskCancelledException（见上）。
            // 这里只兜底“取消发生在写入循环之外”（如 openBatchWriter / 全量读取阶段就被取消）：
            // 此时尚无已提交行数，count 记 0。
            if (handle.cancellation.isCancelled)
            {
                applyCancelled(response, handle, request, 0L, taskLog)
            }
            else
            {
                taskLog.error("Local executor failed", ex)
                response.successful = false
                response.state = RunState.FAILURE
                response.message = ex.message
            }
        }
        finally
        {
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
            taskLog.warn("Local executor task timed out: rows={} task={}", processed, request.taskName)
            response.state = RunState.TIMEOUT
            response.timeout = true
            response.message = "Timed out after ${request.timeout}s"
        }
        else
        {
            taskLog.warn("Local executor task stopped by user: rows={} task={}", processed, request.taskName)
            response.state = RunState.STOPPED
            response.message = "Stopped by user"
        }
    }

    /**
     * 跑 SELECT COUNT(*) FROM (userQuery) 估算总行数。失败时返回 -1，不影响后续流程。
     * Run SELECT COUNT(*) over the user's query. On failure returns -1 — progress just lacks the denominator.
     */
    private fun preCount(plugin: PluginService, configure: Configure, query: String, taskLog: Logger): Long
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
    }
}
