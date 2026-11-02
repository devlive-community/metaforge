package org.devlive.metaforge.executor.metaforge

import org.devlive.metaforge.executor.configure.ExecutorMetrics
import org.devlive.metaforge.executor.configure.ExecutorMetricsListener
import org.devlive.metaforge.executor.configure.TotalSource

/**
 * 一次同步的计数与指标快照。读线程累加读取行数，到上报点时连同已提交行数一起发给 [listener]。
 * 速率按全程平均计算；有总行数时给出进度与预计剩余时间。
 *
 * Counters of one sync run. The reading thread counts rows; at each report point a snapshot with the
 * committed count is sent to [listener]. Rates are run averages; progress and ETA need a known total.
 */
internal class SyncMetrics(private val listener: ExecutorMetricsListener?)
{
    private val startNanos: Long = System.nanoTime()

    @Volatile
    var rowsRead: Long = 0
        private set

    @Volatile
    var totalRows: Long = -1
        private set

    @Volatile
    private var totalSource: TotalSource = TotalSource.UNKNOWN

    fun setTotal(total: Long, source: TotalSource)
    {
        if (total >= 0)
        {
            totalRows = total
            totalSource = source
        }
    }

    fun recordRead()
    {
        rowsRead ++
    }

    fun snapshot(rowsWritten: Long, finished: Boolean = false): ExecutorMetrics
    {
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        val seconds = elapsedMs / 1000.0
        val readRate = if (seconds > 0) rowsRead / seconds else 0.0
        val writeRate = if (seconds > 0) rowsWritten / seconds else 0.0
        val total = totalRows
        val progress = when
        {
            finished -> 100.0
            total > 0 -> minOf(100.0, rowsWritten * 100.0 / total)
            total == 0L -> 0.0
            else -> -1.0
        }
        val eta = when
        {
            finished -> 0L
            total >= 0 && writeRate > 0 -> ((total - rowsWritten).coerceAtLeast(0) / writeRate).toLong()
            else -> -1L
        }
        return ExecutorMetrics(
            rowsRead = rowsRead,
            rowsWritten = rowsWritten,
            totalRows = total,
            totalSource = totalSource,
            progress = progress,
            elapsedMs = elapsedMs,
            etaSeconds = eta,
            readRowsPerSecond = readRate,
            writeRowsPerSecond = writeRate
        )
    }

    fun publish(rowsWritten: Long, finished: Boolean = false): ExecutorMetrics
    {
        val metrics = snapshot(rowsWritten, finished)
        listener?.onMetrics(metrics)
        return metrics
    }
}
