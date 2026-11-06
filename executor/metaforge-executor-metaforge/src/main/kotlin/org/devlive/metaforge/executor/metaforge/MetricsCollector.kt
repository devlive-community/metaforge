package org.devlive.metaforge.executor.metaforge

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.configure.Bottleneck
import org.devlive.metaforge.executor.configure.ExecutorMetrics
import org.devlive.metaforge.executor.configure.MetricsSample
import org.devlive.metaforge.executor.configure.StageMetrics
import org.devlive.metaforge.executor.configure.TotalSource
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder

/**
 * 一次同步的指标采集。读写线程只做原子累加；[sample] 由定时上报线程按固定间隔调用，
 * 生成快照并维护最近几秒的速率窗口与最近 [SERIES_SIZE] 个采样点。
 *
 * Collects metrics of one sync. Reader and writer threads only bump atomic counters; [sample] runs
 * on the reporting thread, builds the snapshot and keeps the rate window and the recent series.
 *
 * @param clock 纳秒时钟，测试可注入 / nanosecond clock, injectable for tests
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
class MetricsCollector(private val clock: () -> Long = System::nanoTime)
{
    private val startNanos: Long = clock()

    private val rowsRead = LongAdder()
    private val bytesRead = LongAdder()
    private val rowsWritten = LongAdder()
    private val bytesWritten = LongAdder()
    private val batches = LongAdder()
    private val batchNanos = LongAdder()
    private val readerWaitNanos = LongAdder()
    private val writerWaitNanos = LongAdder()

    @Volatile
    private var totalRows: Long = -1

    @Volatile
    private var totalSource: TotalSource = TotalSource.UNKNOWN

    @Volatile
    var channels: Int = 1

    @Volatile
    var queueCapacity: Int = 0

    /** 当前排队批次数，由通道维护 / batches queued right now, kept by the channels */
    val queueDepth = AtomicLong()

    private val stages = LinkedHashMap<String, StageMetrics>()
    private val stageStart = HashMap<String, Long>()
    private val window = ArrayDeque<Point>()
    private val series = ArrayDeque<MetricsSample>()
    private var peakWrite = 0.0

    private class Point(val nanos: Long, val read: Long, val written: Long, val bytesRead: Long, val bytesWritten: Long,
                        val readerWait: Long, val writerWait: Long)

    fun setTotal(total: Long, source: TotalSource)
    {
        if (total >= 0)
        {
            totalRows = total
            totalSource = source
        }
    }

    val rowsReadCount: Long
        get() = rowsRead.sum()

    val rowsWrittenCount: Long
        get() = rowsWritten.sum()

    fun recordRead(bytes: Long)
    {
        rowsRead.increment()
        bytesRead.add(bytes)
    }

    fun recordWrite(rows: Long, bytes: Long)
    {
        if (rows > 0)
        {
            rowsWritten.add(rows)
            bytesWritten.add(bytes)
        }
    }

    fun recordBatch(nanos: Long)
    {
        batches.increment()
        batchNanos.add(nanos)
    }

    fun recordReaderWait(nanos: Long) = readerWaitNanos.add(nanos)

    fun recordWriterWait(nanos: Long) = writerWaitNanos.add(nanos)

    @Synchronized
    fun beginStage(name: String)
    {
        stages[name] = StageMetrics(name, "RUNNING", System.currentTimeMillis(), 0)
        stageStart[name] = clock()
    }

    @Synchronized
    fun endStage(name: String, state: String)
    {
        val stage = stages.getOrPut(name) { StageMetrics(name, state, System.currentTimeMillis(), 0) }
        val started = stageStart.remove(name)
        stage.state = state
        stage.elapsedMs = if (started == null) 0 else (clock() - started) / 1_000_000
    }

    /** 未开始就结束（如未开启的预计数）/ a stage that never runs, e.g. a disabled pre-count */
    @Synchronized
    fun skipStage(name: String)
    {
        stages[name] = StageMetrics(name, "SKIPPED", System.currentTimeMillis(), 0)
    }

    /**
     * 采一次样：推进速率窗口与采样序列并返回快照。finished=true 时进度记 100%、剩余时间记 0。
     * Take one sample: advance the rate window and series and return the snapshot.
     */
    @Synchronized
    fun sample(finished: Boolean = false): ExecutorMetrics
    {
        val now = clock()
        val point = Point(now, rowsRead.sum(), rowsWritten.sum(), bytesRead.sum(), bytesWritten.sum(),
            readerWaitNanos.sum(), writerWaitNanos.sum())
        window.addLast(point)
        while (window.size > 1 && now - window.first.nanos > WINDOW_NANOS)
        {
            window.removeFirst()
        }
        val oldest = window.first
        val span = (now - oldest.nanos) / 1e9
        val elapsedNanos = now - startNanos
        val elapsed = elapsedNanos / 1e9

        val readRate = rate(point.read - oldest.read, span, point.read, elapsed)
        val writeRate = rate(point.written - oldest.written, span, point.written, elapsed)
        val readBytesRate = rate(point.bytesRead - oldest.bytesRead, span, point.bytesRead, elapsed)
        val writeBytesRate = rate(point.bytesWritten - oldest.bytesWritten, span, point.bytesWritten, elapsed)
        if (writeRate > peakWrite)
        {
            peakWrite = writeRate
        }

        val elapsedMs = elapsedNanos / 1_000_000
        series.addLast(MetricsSample(elapsedMs, readRate, writeRate))
        while (series.size > SERIES_SIZE)
        {
            series.removeFirst()
        }

        // A successful run has read the whole source, so the rows read are its exact total
        // even when no pre-count or materialized read supplied one.
        val completed = finished && totalRows < 0
        val total = if (completed) point.read else totalRows
        val written = point.written
        val progress = when
        {
            finished -> 100.0
            total > 0 -> minOf(100.0, written * 100.0 / total)
            total == 0L -> 0.0
            else -> -1.0
        }
        val etaRate = if (writeRate > 0) writeRate else if (elapsed > 0) written / elapsed else 0.0
        val eta = when
        {
            finished -> 0L
            total >= 0 && etaRate > 0 -> ((total - written).coerceAtLeast(0) / etaRate).toLong()
            else -> -1L
        }
        val batchCount = batches.sum()
        return ExecutorMetrics(
            rowsRead = point.read,
            rowsWritten = written,
            bytesRead = point.bytesRead,
            bytesWritten = point.bytesWritten,
            totalRows = total,
            totalSource = if (completed) TotalSource.COMPLETED else totalSource,
            progress = progress,
            elapsedMs = elapsedMs,
            etaSeconds = eta,
            readRowsPerSecond = readRate,
            writeRowsPerSecond = writeRate,
            readBytesPerSecond = readBytesRate,
            writeBytesPerSecond = writeBytesRate,
            avgReadRowsPerSecond = if (elapsed > 0) point.read / elapsed else 0.0,
            avgWriteRowsPerSecond = if (elapsed > 0) written / elapsed else 0.0,
            peakWriteRowsPerSecond = peakWrite,
            batchesCommitted = batchCount,
            avgBatchMs = if (batchCount > 0) batchNanos.sum() / 1e6 / batchCount else 0.0,
            channels = channels,
            queueDepth = queueDepth.get().toInt(),
            queueCapacity = queueCapacity,
            readerWaitMs = point.readerWait / 1_000_000,
            writerWaitMs = point.writerWait / 1_000_000,
            bottleneck = if (finished) Bottleneck.NONE else bottleneck(oldest, point),
            stages = stages.values.map { it.copy() }.toMutableList(),
            series = series.map { it.copy() }.toMutableList()
        )
    }

    /** 窗口过短（刚开始）时退回全程平均 / fall back to the run average while the window is too short */
    private fun rate(delta: Long, span: Double, cumulative: Long, elapsed: Double): Double = when
    {
        span >= MIN_WINDOW_SECONDS -> delta / span
        elapsed > 0 -> cumulative / elapsed
        else -> 0.0
    }

    /**
     * 窗口内读取方等待占比过半 -> 目标端慢；写入方等待占比过半 -> 源端慢。
     * Readers waiting most of the window means the target is slow; writers waiting means the source is.
     */
    private fun bottleneck(oldest: Point, latest: Point): Bottleneck
    {
        val span = latest.nanos - oldest.nanos
        if (span <= 0)
        {
            return Bottleneck.NONE
        }
        val lanes = channels.coerceAtLeast(1)
        val readerShare = (latest.readerWait - oldest.readerWait).toDouble() / (span * lanes)
        val writerShare = (latest.writerWait - oldest.writerWait).toDouble() / (span * lanes)
        return when
        {
            readerShare >= BUSY_SHARE && readerShare >= writerShare -> Bottleneck.TARGET
            writerShare >= BUSY_SHARE -> Bottleneck.SOURCE
            else -> Bottleneck.NONE
        }
    }

    companion object
    {
        /** 速率窗口长度 / length of the rate window */
        const val WINDOW_NANOS: Long = 5_000_000_000L

        /** 保留的采样点数（默认 1 秒一次即 2 分钟）/ samples kept, two minutes at the default interval */
        const val SERIES_SIZE: Int = 120

        private const val MIN_WINDOW_SECONDS = 0.5
        private const val BUSY_SHARE = 0.5
    }
}
