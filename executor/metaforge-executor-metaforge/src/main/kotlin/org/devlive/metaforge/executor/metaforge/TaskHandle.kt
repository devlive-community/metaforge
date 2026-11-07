package org.devlive.metaforge.executor.metaforge

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** 取消原因：区分“用户主动停止”与“任务超时”，供 start() 映射成 STOPPED / TIMEOUT。 */
internal enum class CancelReason
{
    USER,
    TIMEOUT
}

/**
 * 单一取消信号源。stop() / 超时看门狗调 [cancel]，各 sync 策略只读 [isCancelled] / 调 [throwIfCancelled]。
 * 取消时统一抛 [TaskCancelledException]，并携带“已提交行数”（committed），由 start() 按 [reason] 转成 STOPPED / TIMEOUT。
 *
 * The single source of truth for cancellation. Cancellation always surfaces as a
 * [TaskCancelledException] carrying the committed row count; [reason] tells USER stop from TIMEOUT.
 */
internal class CancellationToken
{
    private val cancelled: AtomicBoolean = AtomicBoolean(false)

    /** 首个取消原因胜出（用户停止与超时若竞争，以先到者为准） */
    @Volatile
    var reason: CancelReason? = null
        private set

    val isCancelled: Boolean
        get() = cancelled.get()

    fun cancel(reason: CancelReason = CancelReason.USER)
    {
        if (cancelled.compareAndSet(false, true))
        {
            this.reason = reason
        }
    }

    fun throwIfCancelled(processed: Long)
    {
        if (cancelled.get())
        {
            throw TaskCancelledException(processed)
        }
    }
}

/**
 * 已注册的活跃任务句柄。
 * - cancellation: 取消信号，stop() 设置、通道读取
 * - 源端 Statement：各通道正在执行的查询，取消时全部 cancel()，不必等下一行
 * - taskLog: 让 stop() 把"用户停止"事件写到任务专属日志里
 *
 * An in-flight task handle shared between start() and stop().
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
internal class TaskHandle
{
    val cancellation: CancellationToken = CancellationToken()

    private val statements: MutableSet<java.sql.Statement> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var cancelRequested: Boolean = false

    @Volatile
    var taskLog: Logger? = null

    /** 登记正在执行的查询；取消已发生时立即取消它 / register a running query; cancel it at once if cancellation already happened */
    fun addStatement(statement: java.sql.Statement)
    {
        statements.add(statement)
        if (cancelRequested)
        {
            cancel(statement)
        }
    }

    fun removeStatement(statement: java.sql.Statement)
    {
        statements.remove(statement)
    }

    /**
     * 异步取消所有正在执行的查询。Statement.cancel() 通常会新开连接发 KILL QUERY，源库网络异常时会阻塞，
     * 所以放到独立线程。
     * Cancels every running query off this thread, since Statement.cancel() may block on a new connection.
     */
    fun cancelStatements()
    {
        cancelRequested = true
        statements.forEach { cancel(it) }
    }

    private fun cancel(statement: java.sql.Statement)
    {
        TaskRegistry.submitCancel {
            try
            {
                if (!statement.isClosed)
                {
                    statement.cancel()
                    taskLog?.warn("Source JDBC statement cancelled")
                }
            }
            catch (ex: Exception)
            {
                taskLog?.warn("Cancel source statement failed: {}", ex.message)
            }
        }
    }
}

/** 取消传播专用异常，携带已提交行数便于上游记录 */
internal class TaskCancelledException(val processed: Long) : RuntimeException("Task cancelled by user")
