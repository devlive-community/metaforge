package org.devlive.metaforge.executor.metaforge

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.configure.SplitMetrics
import org.devlive.metaforge.spi.adapter.BatchWriter
import org.slf4j.Logger
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** 一个分片的运行进度 / running progress of one split */
internal class SplitProgress(val split: Split)
{
    @Volatile
    var state: String = "PENDING"

    @Volatile
    var channel: Int = 0

    @Volatile
    var rowsRead: Long = 0

    @Volatile
    var rowsWritten: Long = 0

    /** 已提交到的分片键字面量，只有写线程更新 / literal of the key committed so far, updated by the writer only */
    @Volatile
    var watermark: String? = null

    @Volatile
    var retries: Long = 0

    @Volatile
    private var startedAt: Long = 0

    @Volatile
    private var elapsedMs: Long = 0

    /** 从断点恢复已提交的进度 / restore committed progress from a checkpoint */
    fun restore(checkpoint: SplitCheckpoint)
    {
        watermark = checkpoint.watermark
        rowsWritten = checkpoint.rowsWritten
        rowsRead = checkpoint.rowsWritten
        if (checkpoint.done)
        {
            state = "DONE"
        }
    }

    fun start(channelId: Int)
    {
        channel = channelId
        startedAt = System.nanoTime()
        state = "RUNNING"
    }

    fun finish(finalState: String)
    {
        if (startedAt > 0)
        {
            elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        }
        state = finalState
    }

    fun metrics(): SplitMetrics = SplitMetrics(
        split.id, split.range, state, channel, rowsRead, rowsWritten,
        if (state == "RUNNING") (System.nanoTime() - startedAt) / 1_000_000 else elapsedMs,
        retries, watermark
    )

    fun checkpoint(): SplitCheckpoint =
        SplitCheckpoint(split.id, split.query, split.range, split.nullOnly, state == "DONE", watermark, rowsWritten)
}

/** 写入容错设置 / write fault-tolerance settings */
internal class WriteTolerance(val retries: Int, val backoffMs: Long, val maxErrorRows: Long)

/**
 * 一次传输的多条通道共享的状态：取消信号、首个失败、待处理分片、坏行记录。任一通道失败时取消其他通道正在
 * 执行的查询，让任务尽快结束。
 *
 * State shared by the channels of one transfer: cancellation, the first failure, the pending splits
 * and the bad-row log. A failing channel cancels the queries of the others so the task ends quickly.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
internal class ChannelGroup(
    val handle: TaskHandle,
    val metrics: MetricsCollector,
    val limiter: RateLimiter,
    val tolerance: WriteTolerance,
    val dirty: DirtyLog,
    val maxGroupRows: Int,
    val taskLog: Logger,
    splits: List<SplitProgress>
)
{
    private val pending = ConcurrentLinkedQueue(splits.filter { it.state != "DONE" })
    val failure = AtomicReference<Throwable?>()

    val token: CancellationToken
        get() = handle.cancellation

    val stopped: Boolean
        get() = token.isCancelled || failure.get() != null

    fun nextSplit(): SplitProgress? = if (stopped) null else pending.poll()

    fun fail(error: Throwable)
    {
        if (failure.compareAndSet(null, error))
        {
            handle.cancelStatements()
        }
    }

    /**
     * 之后无法再精确续传（如同一个键的行多到放不进一批）：放弃断点，已读的行照常提交。
     * The task can no longer resume exactly, for example when one key has too many rows for a batch.
     */
    fun loseResume(reason: String)
    {
        if (metrics.resumable)
        {
            taskLog.warn("The sync can no longer pause or resume: {}", reason)
            metrics.resumable = false
            handle.pausable = false
        }
    }
}

/**
 * 一条读写通道：所在线程依次取分片、读取并攒批，写线程独占一个 [BatchWriter] 写入目标端，二者之间是
 * 容量为 [queueCapacity] 的有界队列（满了读取方等待，形成背压）。每批由写线程显式提交，失败可整批重写。
 *
 * 可续传的分片只在分片键变化处切批：同一个键的行一定在同一批，一批提交后水位就是它最后一行的键，
 * 续传条件 `key > 水位` 不丢不重。这类分片在停止、暂停或出错时放弃尚未成批的行（续传会重新读取）。
 * 不可续传的分片按 [batchSize] 切批，已读的行总是先提交再报告结果。
 *
 * 写入失败时：暂时性错误重开连接并退避重试；数据错误在 [WriteTolerance.maxErrorRows] 内二分定位坏行并跳过；
 * 其余直接失败。取消与失败由 [ChannelGroup] 记录，交给调用方统一处理。
 *
 * One read/write channel: its thread reads and batches rows, a writer thread owns a [BatchWriter], and
 * a bounded queue between them applies back-pressure. Resumable splits cut batches only where the key
 * changes, so a committed batch's last key is an exact watermark. Failed writes are retried after
 * transient errors, bad rows are isolated by bisection within the limit, and anything else fails.
 */
// NP_*: 假阳性，Kotlin 的安全调用已判空，lateinit 的 writer 在写线程启动前赋值
@SuppressFBWarnings(value = ["EI_EXPOSE_REP2", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "NP_NULL_ON_SOME_PATH"])
internal class Channel(
    private val id: Int,
    private val taskName: String,
    private val batchSize: Int,
    private val queueCapacity: Int,
    private val chunkRows: Int,
    private val group: ChannelGroup
)
{
    private class Batch(val rows: ArrayList<List<Any?>>, val bytes: Long, val split: SplitProgress?, val endState: String?, val watermark: String?)

    private val metrics = group.metrics
    private val tolerance = group.tolerance
    private val queue = ArrayBlockingQueue<Batch>(queueCapacity.coerceAtLeast(1))

    @Volatile
    private var writerFailed = false

    // 只在写线程中访问（结束后由 join 保证可见）/ writer thread only
    private var committed: Long = 0
    private lateinit var writer: BatchWriter

    /** 处理分片直到取完或任务停止 / process splits until none are left or the task stops */
    fun run(readerFor: (SplitProgress) -> SourceReader, openWriter: () -> BatchWriter)
    {
        writer = try
        {
            openWriter()
        }
        catch (ex: Throwable)
        {
            group.fail(ex)
            return
        }
        val writerThread = Thread({ writeLoop(openWriter) }, "metaforge-writer-${taskName.ifBlank { "task" }}-$id")
        writerThread.isDaemon = true
        writerThread.start()
        while (true)
        {
            val progress = group.nextSplit() ?: break
            progress.start(id)
            readSplit(progress, readerFor(progress))
        }
        handOff(END)
        writerThread.join()
        group.taskLog.info("Channel {} done: committed={}", id, committed)
    }

    private fun readSplit(progress: SplitProgress, reader: SourceReader)
    {
        val keyed = progress.split.keyColumn != null
        var current = ArrayList<List<Any?>>(batchSize)
        var currentBytes = 0L
        var chunkCount = 0L
        var chunkBytes = 0L
        var read = progress.rowsRead
        var lastKey: Any? = null
        var endState = "DONE"
        try
        {
            reader.read { row, key ->
                // 源端还在吐行时检测到取消或其他通道失败，立即中断读取
                group.token.throwIfCancelled(0)
                if (group.stopped)
                {
                    throw ChannelStoppedException()
                }
                val resumable = keyed && metrics.resumable
                if (resumable && current.size >= group.maxGroupRows && key == lastKey)
                {
                    group.loseResume("more than ${group.maxGroupRows} rows share the split key in split ${progress.split.id}")
                }
                val cut = if (keyed && metrics.resumable) current.size >= batchSize && key != lastKey else current.size >= batchSize
                if (cut)
                {
                    handOff(Batch(current, currentBytes, progress, null, KeyLiteral.of(lastKey)))
                    current = ArrayList(batchSize)
                    currentBytes = 0L
                }
                val bytes = ValueSize.ofRow(row)
                metrics.recordRead(bytes)
                read ++
                progress.rowsRead = read
                current.add(row)
                currentBytes += bytes
                lastKey = key
                if (group.limiter.enabled)
                {
                    chunkCount ++
                    chunkBytes += bytes
                    if (chunkCount >= chunkRows)
                    {
                        metrics.recordThrottleWait(group.limiter.acquire(chunkCount, chunkBytes) { group.stopped })
                        chunkCount = 0
                        chunkBytes = 0
                    }
                }
            }
        }
        catch (ex: Throwable)
        {
            if (group.stopped || ex is ChannelStoppedException)
            {
                // 取消，或其他通道失败后被中断 / cancelled, or interrupted after another channel failed
                endState = "CANCELLED"
            }
            else
            {
                endState = "FAILED"
                group.fail(ex)
            }
        }
        if (endState != "DONE" && keyed && metrics.resumable)
        {
            // 尚未成批的行可能只有某个键的一部分，不提交；续传会从水位之后重新读取
            // These rows may hold only part of a key's rows, so they stay uncommitted and a resume reads them again.
            metrics.undoRead(current.size.toLong(), currentBytes)
            progress.rowsRead = read - current.size
            current = ArrayList(0)
            currentBytes = 0L
        }
        handOff(Batch(current, currentBytes, progress, endState, if (current.isEmpty()) null else KeyLiteral.of(lastKey)))
    }

    /** 放入队列；写线程已失败时直接丢弃（不会再有人消费）/ enqueue; drop once the writer has failed */
    private fun handOff(batch: Batch)
    {
        val started = System.nanoTime()
        try
        {
            while (!writerFailed)
            {
                if (queue.offer(batch, POLL_MS, TimeUnit.MILLISECONDS))
                {
                    if (batch !== END)
                    {
                        metrics.queueDepth.incrementAndGet()
                    }
                    return
                }
            }
        }
        finally
        {
            if (batch !== END)
            {
                metrics.recordReaderWait(System.nanoTime() - started)
            }
        }
    }

    private fun writeLoop(openWriter: () -> BatchWriter)
    {
        var inFlight: SplitProgress? = null
        try
        {
            while (true)
            {
                val waitStart = System.nanoTime()
                val batch = queue.take()
                metrics.recordWriterWait(System.nanoTime() - waitStart)
                if (batch === END)
                {
                    break
                }
                metrics.queueDepth.decrementAndGet()
                inFlight = batch.split
                if (batch.rows.isNotEmpty())
                {
                    commitBatch(batch, openWriter)
                }
                if (batch.endState != null)
                {
                    batch.split?.finish(batch.endState)
                    inFlight = null
                }
            }
            closeWriter()
        }
        catch (ex: Throwable)
        {
            closeWriter()
            inFlight?.finish("FAILED")
            writerFailed = true
            group.fail(ex)
            // 让读取方不再阻塞在满队列上 / unblock a reader waiting on a full queue
            val dropped = queue.size
            queue.clear()
            metrics.queueDepth.addAndGet(- dropped.toLong())
        }
    }

    private fun closeWriter()
    {
        try
        {
            writer.close()
        }
        catch (ex: Exception)
        {
            group.taskLog.warn("Close writer failed: {}", ex.message)
        }
    }

    /** 提交一批：暂时性错误重试，数据错误定位坏行，其余抛出 / commit a batch, retrying or isolating bad rows as needed */
    private fun commitBatch(batch: Batch, openWriter: () -> BatchWriter)
    {
        val started = System.nanoTime()
        var attempt = 0
        var written: Long
        while (true)
        {
            try
            {
                writeRows(batch.rows)
                written = batch.rows.size.toLong()
                break
            }
            catch (ex: Exception)
            {
                val kind = SyncErrors.classify(ex)
                if (kind == SyncErrors.Kind.TRANSIENT && attempt < tolerance.retries && !group.stopped)
                {
                    attempt ++
                    metrics.recordBatchRetry()
                    batch.split?.let { it.retries = it.retries + 1 }
                    group.taskLog.warn("Write failed, retry {} of {}: {}", attempt, tolerance.retries, ex.message)
                    reopenWriter(openWriter, attempt)
                    continue
                }
                if (kind == SyncErrors.Kind.DATA && tolerance.maxErrorRows > 0 && writer.failedWritesLeaveNoRows())
                {
                    written = isolateBadRows(batch, ex)
                    break
                }
                metrics.recordFailed(batch.rows.size.toLong())
                throw ex
            }
        }
        metrics.recordBatch(System.nanoTime() - started)
        credit(batch, written)
        batch.split?.let { split -> batch.watermark?.let { split.watermark = it } }
    }

    private fun writeRows(rows: List<List<Any?>>)
    {
        for (row in rows)
        {
            writer.addRow(row)
        }
        writer.flush()
    }

    /** 关闭出错的连接，退避后重开；重开失败留给下一次写入重试 / reopen after a backoff; a failed open is retried by the next write */
    private fun reopenWriter(openWriter: () -> BatchWriter, attempt: Int)
    {
        closeWriter()
        val deadline = System.currentTimeMillis() + tolerance.backoffMs * (1L shl (attempt - 1).coerceAtMost(6))
        while (!group.stopped && System.currentTimeMillis() < deadline)
        {
            Thread.sleep(minOf(POLL_MS, (deadline - System.currentTimeMillis()).coerceAtLeast(1)))
        }
        writer = try
        {
            openWriter()
        }
        catch (ex: Exception)
        {
            group.taskLog.warn("Reopen writer failed: {}", ex.message)
            FailingWriter(ex)
        }
    }

    /**
     * 整批失败后二分定位：好的行照常提交，单行失败才算坏行。超过上限或遇到非数据错误时任务失败，且此时一批
     * 已提交一部分，无法再精确续传。
     *
     * Bisect a rejected batch: good rows commit, a single failing row is a bad one. Exceeding the limit
     * or a non-data error fails the task, and with part of the batch committed it can no longer resume.
     */
    private fun isolateBadRows(batch: Batch, cause: Exception): Long
    {
        val splitId = batch.split?.split?.id ?: 0
        var written = 0L
        fun tolerate(rows: List<List<Any?>>, error: Exception)
        {
            if (rows.size == 1)
            {
                group.dirty.record(splitId, rows[0], error)
                metrics.recordSkipped()
                if (metrics.rowsSkippedCount > tolerance.maxErrorRows)
                {
                    throw IllegalStateException(
                        "More than ${tolerance.maxErrorRows} rows were rejected by the target; the last error: ${error.message}", error
                    )
                }
                return
            }
            val middle = rows.size / 2
            for (part in listOf(rows.subList(0, middle), rows.subList(middle, rows.size)))
            {
                try
                {
                    writeRows(part)
                    written += part.size
                }
                catch (ex: Exception)
                {
                    if (SyncErrors.classify(ex) != SyncErrors.Kind.DATA)
                    {
                        throw ex
                    }
                    tolerate(part, ex)
                }
            }
        }
        try
        {
            tolerate(batch.rows, cause)
        }
        catch (ex: Exception)
        {
            group.loseResume("a batch failed after part of it was committed")
            credit(batch, written)
            metrics.recordFailed(batch.rows.size - written)
            throw ex
        }
        return written
    }

    /** 把提交的行数与对应字节计入指标 / credit committed rows and their share of the batch's bytes */
    private fun credit(batch: Batch, written: Long)
    {
        val bytes = if (batch.rows.isEmpty()) 0L else batch.bytes * written / batch.rows.size
        committed += written
        metrics.recordWrite(written, bytes)
        batch.split?.let { it.rowsWritten = it.rowsWritten + written }
    }

    /** 重开连接失败时占位，让下一次写入带着原错误进入重试 / stands in after a failed reopen so the next write retries */
    private class FailingWriter(private val error: Exception) : BatchWriter
    {
        override fun addRow(row: List<*>)
        {
            throw error
        }

        override fun flush()
        {
            throw error
        }

        override fun writtenCount(): Long = 0

        override fun close()
        {
        }
    }

    /** 其他通道失败时用于中断本通道读取 / interrupts this channel's read when another channel failed */
    private class ChannelStoppedException : RuntimeException("Stopped because another channel failed")

    companion object
    {
        private const val POLL_MS = 100L
        private val END = Batch(ArrayList(0), 0, null, null, null)
    }
}

/**
 * 运行一次传输：[concurrency] 条通道从同一个分片队列取活（分片数通常多于通道数，负载不均时自动平衡）。
 * 第一条通道在调用线程运行，其余各开一个线程。返回已提交总行数；取消抛 [TaskCancelledException]，失败抛首个错误。
 * 结束后 [progress] 仍可读取，用于生成断点。
 *
 * Runs one transfer with [concurrency] channels sharing one split queue. Returns the committed count;
 * throws [TaskCancelledException] on cancellation and the first error on failure.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
internal class Transfer(
    private val taskName: String,
    private val batchSize: Int,
    private val queueCapacity: Int,
    private val concurrency: Int,
    private val maxGroupRows: Int,
    private val tolerance: WriteTolerance,
    private val dirty: DirtyLog,
    private val handle: TaskHandle,
    private val metrics: MetricsCollector,
    private val limiter: RateLimiter,
    private val taskLog: Logger,
    val progress: List<SplitProgress>
)
{
    fun run(readerFor: (SplitProgress) -> SourceReader, openWriter: () -> BatchWriter): Long
    {
        val channels = concurrency.coerceIn(1, progress.count { it.state != "DONE" }.coerceAtLeast(1))
        metrics.channels = channels
        metrics.queueCapacity = queueCapacity * channels

        val group = ChannelGroup(handle, metrics, limiter, tolerance, dirty, maxGroupRows, taskLog, progress)
        val chunkRows = limiter.chunkRows(channels)
        val newChannel = { id: Int -> Channel(id, taskName, batchSize, queueCapacity, chunkRows, group) }
        val threads = (2..channels).map { id ->
            Thread({
                try
                {
                    newChannel(id).run(readerFor, openWriter)
                }
                catch (ex: Throwable)
                {
                    group.fail(ex)
                }
            }, "metaforge-channel-${taskName.ifBlank { "task" }}-$id").apply {
                isDaemon = true
                start()
            }
        }
        try
        {
            newChannel(1).run(readerFor, openWriter)
        }
        catch (ex: Throwable)
        {
            group.fail(ex)
        }
        threads.forEach { it.join() }
        // 写线程失败时读取方的收尾批次会被丢弃 / a failed writer drops the reader's closing batch
        progress.filter { it.state == "RUNNING" }.forEach { it.finish("CANCELLED") }
        // 暂停后未完成的分片会在继续时接着跑 / after a pause the unfinished splits carry on when resumed
        if (handle.cancellation.reason == CancelReason.PAUSE)
        {
            progress.filter { it.state == "CANCELLED" }.forEach { it.state = "PENDING" }
        }

        val committed = metrics.rowsWrittenCount
        if (handle.cancellation.isCancelled)
        {
            throw TaskCancelledException(committed)
        }
        // 驱动抛出的 Error 也只是这次任务失败，不能带走工作线程 / an Error from a driver fails this task, not the worker thread
        group.failure.get()?.let { throw if (it is Exception) it else IllegalStateException(it.message, it) }
        return committed
    }
}
