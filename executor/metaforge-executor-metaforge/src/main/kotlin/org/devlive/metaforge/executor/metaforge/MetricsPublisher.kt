package org.devlive.metaforge.executor.metaforge

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.configure.ExecutorMetrics
import org.devlive.metaforge.executor.configure.ExecutorMetricsListener
import org.slf4j.Logger

/**
 * 把 [MetricsCollector] 的快照交给监听方。定时上报与最终上报互斥：[close] 之后不会再有定时快照，
 * 因此监听方收到的最后一条一定是最终结果。监听方抛出的异常只记日志，不影响任务。
 *
 * Hands collector snapshots to the listener. Timed reports and the final one are serialized, so the
 * last snapshot the listener sees is always the final one; listener errors are only logged.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP2"])
internal class MetricsPublisher(
    private val metrics: MetricsCollector,
    private val listener: ExecutorMetricsListener?,
    private val taskLog: Logger
)
{
    private var closed = false

    @Synchronized
    fun tick()
    {
        if (!closed)
        {
            deliver(metrics.sample())
        }
    }

    /** 发出最终快照并停止后续上报 / publish the final snapshot and stop further reports */
    @Synchronized
    fun close(finished: Boolean): ExecutorMetrics
    {
        closed = true
        val last = metrics.sample(finished)
        deliver(last)
        return last
    }

    private fun deliver(snapshot: ExecutorMetrics)
    {
        try
        {
            listener?.onMetrics(snapshot)
        }
        catch (ex: Exception)
        {
            taskLog.warn("Metrics listener failed: {}", ex.message)
        }
    }
}

/**
 * 记录当前阶段，失败或取消时把它标记为对应状态。
 * Tracks the current stage so a failure or cancellation marks it accordingly.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP2"])
internal class StageTracker(private val metrics: MetricsCollector)
{
    private var current: String? = null

    fun begin(name: String)
    {
        current = name
        metrics.beginStage(name)
    }

    fun end(name: String)
    {
        metrics.endStage(name, "DONE")
        if (current == name)
        {
            current = null
        }
    }

    fun fail(state: String)
    {
        current?.let { metrics.endStage(it, state) }
        current = null
    }
}
