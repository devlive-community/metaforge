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

    @Volatile
    private var startedAt: Long = 0

    @Volatile
    private var elapsedMs: Long = 0

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
        if (state == "RUNNING") (System.nanoTime() - startedAt) / 1_000_000 else elapsedMs
    )
}

/**
 * 一次传输的多条通道共享的状态：取消信号、首个失败、待处理分片。任一通道失败时取消其他通道正在执行的查询，
 * 让任务尽快结束。
 *
 * State shared by the channels of one transfer: cancellation, the first failure and the pending splits.
 * A failing channel cancels the queries of the others so the task ends quickly.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
internal class ChannelGroup(
    val handle: TaskHandle,
    val metrics: MetricsCollector,
    val limiter: RateLimiter,
    val taskLog: Logger,
    splits: List<SplitProgress>
)
{
    private val pending = ConcurrentLinkedQueue(splits)
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
}

/**
 * 一条读写通道：所在线程依次取分片、读取并攒批，写线程独占一个 [BatchWriter] 写入目标端，二者之间是
 * 容量为 [queueCapacity] 的有界队列（满了读取方等待，形成背压）。读取不会被 executeBatch 阻塞。
 *
 * 每批 [batchSize] 行，与 BatchWriter 的提交粒度一致；分片结束时立即 flush，所以每批对应一次提交，
 * 一批提交的行都属于同一分片；单写线程保证批次有序。
 *
 * 结束语义：无论读取正常结束、被取消还是出错，已读出的行都会先提交；取消与失败由 [ChannelGroup] 记录，
 * 交给调用方统一处理。
 *
 * One read/write channel: its thread takes splits, reads and batches rows; a writer thread owns a
 * [BatchWriter]; a bounded queue between them applies back-pressure. Rows already read are always
 * committed; cancellation and failures are recorded on the [ChannelGroup].
 */
// NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE: 假阳性，Kotlin 的安全调用 batch.split?.finish() 已判空
@SuppressFBWarnings(value = ["EI_EXPOSE_REP2", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"])
internal class Channel(
    private val id: Int,
    private val taskName: String,
    private val batchSize: Int,
    private val queueCapacity: Int,
    private val chunkRows: Int,
    private val group: ChannelGroup
)
{
    private class Batch(val rows: ArrayList<List<Any?>>, val bytes: Long, val split: SplitProgress?, val endState: String?)

    private val metrics = group.metrics
    private val queue = ArrayBlockingQueue<Batch>(queueCapacity.coerceAtLeast(1))

    @Volatile
    private var writerFailed = false

    // 已提交行数与已送入 writer、尚未提交的行数和字节；只在写线程中访问（结束后由 join 保证可见）
    // committed count and rows and bytes fed but not yet committed; writer thread only
    private var committed: Long = 0
    private var pendingRows: Long = 0
    private var pendingBytes: Long = 0

    /** 处理分片直到取完或任务停止 / process splits until none are left or the task stops */
    fun run(readerFor: (Split) -> SourceReader, openWriter: () -> BatchWriter)
    {
        val writer = try
        {
            openWriter()
        }
        catch (ex: Throwable)
        {
            group.fail(ex)
            return
        }
        val writerThread = Thread({ writeLoop(writer) }, "metaforge-writer-${taskName.ifBlank { "task" }}-$id")
        writerThread.isDaemon = true
        writerThread.start()
        while (true)
        {
            val progress = group.nextSplit() ?: break
            progress.start(id)
            readSplit(progress, readerFor(progress.split))
        }
        handOff(END)
        writerThread.join()
        group.taskLog.info("Channel {} done: committed={}", id, committed)
    }

    private fun readSplit(progress: SplitProgress, reader: SourceReader)
    {
        var current = ArrayList<List<Any?>>(batchSize)
        var currentBytes = 0L
        var chunkCount = 0L
        var chunkBytes = 0L
        var read = 0L
        var endState = "DONE"
        try
        {
            reader.read { row ->
                // 源端还在吐行时检测到取消或其他通道失败，立即中断读取
                group.token.throwIfCancelled(0)
                if (group.stopped)
                {
                    throw ChannelStoppedException()
                }
                val bytes = ValueSize.ofRow(row)
                metrics.recordRead(bytes)
                read ++
                progress.rowsRead = read
                current.add(row)
                currentBytes += bytes
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
                if (current.size >= batchSize)
                {
                    handOff(Batch(current, currentBytes, progress, null))
                    current = ArrayList(batchSize)
                    currentBytes = 0L
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
        // 已读出的行一律提交，分片结束时写线程 flush 并记录结果
        handOff(Batch(current, currentBytes, progress, endState))
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

    private fun writeLoop(writer: BatchWriter)
    {
        var inFlight: SplitProgress? = null
        var closed = false
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
                val started = System.nanoTime()
                for (row in batch.rows)
                {
                    writer.addRow(row)
                }
                pendingRows += batch.rows.size
                pendingBytes += batch.bytes
                if (batch.endState != null)
                {
                    writer.flush()
                }
                if (writer.writtenCount() > committed)
                {
                    metrics.recordBatch(System.nanoTime() - started)
                }
                settle(writer.writtenCount(), batch.split)
                if (batch.endState != null)
                {
                    batch.split?.finish(batch.endState)
                    inFlight = null
                }
            }
            closed = true
            closeCounted(writer, null)
        }
        catch (ex: Throwable)
        {
            if (!closed)
            {
                // 出错时仍提交已读行 / rows already read are still committed on failure
                try
                {
                    closeCounted(writer, inFlight)
                }
                catch (suppressed: Throwable)
                {
                    ex.addSuppressed(suppressed)
                }
            }
            settle(writer.writtenCount(), inFlight)
            inFlight?.finish("FAILED")
            writerFailed = true
            group.fail(ex)
            // 让读取方不再阻塞在满队列上 / unblock a reader waiting on a full queue
            val dropped = queue.size
            queue.clear()
            metrics.queueDepth.addAndGet(-dropped.toLong())
        }
    }

    /** close() 提交的剩余行也是一次批次 / rows committed by close() also count as a batch */
    private fun closeCounted(writer: BatchWriter, split: SplitProgress?)
    {
        val before = writer.writtenCount()
        val started = System.nanoTime()
        writer.close()
        if (writer.writtenCount() > before)
        {
            metrics.recordBatch(System.nanoTime() - started)
        }
        settle(writer.writtenCount(), split)
    }

    /**
     * 把新提交的行数与按比例折算的字节计入指标。整批提交时字节是精确的。
     * Credit newly committed rows and their share of the pending bytes (exact for whole batches).
     */
    private fun settle(written: Long, split: SplitProgress?)
    {
        val delta = written - committed
        if (delta <= 0)
        {
            return
        }
        val rows = minOf(delta, pendingRows)
        val bytes = if (pendingRows > 0) pendingBytes * rows / pendingRows else 0L
        pendingRows -= rows
        pendingBytes -= bytes
        committed = written
        metrics.recordWrite(delta, bytes)
        split?.let { it.rowsWritten += delta }
    }

    /** 其他通道失败时用于中断本通道读取 / interrupts this channel's read when another channel failed */
    private class ChannelStoppedException : RuntimeException("Stopped because another channel failed")

    companion object
    {
        private const val POLL_MS = 100L
        private val END = Batch(ArrayList(0), 0, null, null)
    }
}

/**
 * 运行一次传输：[concurrency] 条通道从同一个分片队列取活（分片数通常多于通道数，负载不均时自动平衡）。
 * 第一条通道在调用线程运行，其余各开一个线程。返回已提交总行数；取消抛 [TaskCancelledException]，失败抛首个错误。
 *
 * Runs one transfer with [concurrency] channels sharing one split queue. Returns the committed count;
 * throws [TaskCancelledException] on cancellation and the first error on failure.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP2", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
internal class Transfer(
    private val taskName: String,
    private val batchSize: Int,
    private val queueCapacity: Int,
    private val concurrency: Int,
    private val handle: TaskHandle,
    private val metrics: MetricsCollector,
    private val limiter: RateLimiter,
    private val taskLog: Logger
)
{
    fun run(plan: SplitPlan, readerFor: (Split) -> SourceReader, openWriter: () -> BatchWriter): Long
    {
        val progress = plan.splits.map { SplitProgress(it) }
        val channels = concurrency.coerceIn(1, progress.size.coerceAtLeast(1))
        metrics.channels = channels
        metrics.queueCapacity = queueCapacity * channels
        metrics.splitStrategy = plan.strategy
        metrics.splitColumn = plan.column
        metrics.splitSource = { progress.map { it.metrics() } }

        val group = ChannelGroup(handle, metrics, limiter, taskLog, progress)
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

        val committed = metrics.rowsWrittenCount
        if (handle.cancellation.isCancelled)
        {
            throw TaskCancelledException(committed)
        }
        group.failure.get()?.let { throw it }
        return committed
    }
}
