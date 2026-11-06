package org.devlive.metaforge.executor.metaforge

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.spi.adapter.BatchWriter
import org.slf4j.Logger
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * 一条读写通道：调用线程读取源端并攒批，写线程独占一个 [BatchWriter] 写入目标端，二者之间是
 * 容量为 [queueCapacity] 的有界队列（满了读取方等待，形成背压）。读取不再被 executeBatch 阻塞。
 *
 * 每批 [batchSize] 行，与 BatchWriter 的 flush 粒度一致，所以一批对应一次提交；单写线程保证批次有序。
 *
 * 结束语义（与重构前一致）：无论读取正常结束、被取消还是出错，已读出的行都会先交给写线程并提交，
 * 然后：取消 -> [TaskCancelledException]（携带已提交行数）；读取或写入出错 -> 原样抛出。
 *
 * One read/write channel: the calling thread reads and batches rows, a writer thread owns a
 * [BatchWriter], and a bounded queue between them applies back-pressure. Rows already read are
 * always committed before the channel reports cancellation (with the committed count) or an error.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP2"])
internal class Channel(
    private val id: Int,
    private val taskName: String,
    private val batchSize: Int,
    private val queueCapacity: Int,
    private val metrics: MetricsCollector,
    private val token: CancellationToken,
    private val taskLog: Logger
)
{
    private class Batch(val rows: ArrayList<List<Any?>>, val bytes: Long)

    private val queue = ArrayBlockingQueue<Batch>(queueCapacity.coerceAtLeast(1))
    private val writerError = AtomicReference<Throwable?>()

    @Volatile
    private var committed: Long = 0

    /**
     * 用 [writer] 写完 [reader] 读出的全部行，返回已提交行数。writer 由调用方打开，本方法负责关闭。
     * Copy everything [reader] yields through [writer] (opened by the caller, closed here); returns the committed count.
     */
    fun run(reader: SourceReader, writer: BatchWriter): Long
    {
        val writerThread = Thread({ writeLoop(writer) }, "metaforge-writer-${taskName.ifBlank { "task" }}-$id")
        writerThread.isDaemon = true
        writerThread.start()

        var current = ArrayList<List<Any?>>(batchSize)
        var currentBytes = 0L
        var readError: Throwable? = null
        try
        {
            reader.read { row ->
                // 源端还在吐行时检测到取消，立即中断读取
                token.throwIfCancelled(0)
                writerError.get()?.let { throw WriterFailedException(it) }
                val bytes = ValueSize.ofRow(row)
                metrics.recordRead(bytes)
                current.add(row)
                currentBytes += bytes
                if (current.size >= batchSize)
                {
                    handOff(Batch(current, currentBytes))
                    current = ArrayList(batchSize)
                    currentBytes = 0L
                }
            }
        }
        catch (ex: Throwable)
        {
            readError = ex
        }

        // 已读出的行一律交给写线程提交，再放结束标记
        if (current.isNotEmpty())
        {
            handOff(Batch(current, currentBytes))
        }
        handOff(END)
        writerThread.join()

        val failure = writerError.get()
        when
        {
            token.isCancelled -> throw TaskCancelledException(committed)
            failure != null -> throw failure
            readError is WriterFailedException -> throw readError.cause ?: readError
            readError != null -> throw readError
        }
        taskLog.info("Channel {} done: read={} committed={}", id, metrics.rowsReadCount, committed)
        return committed
    }

    /** 放入队列；写线程已失败时直接丢弃（不会再有人消费）/ enqueue; drop once the writer has failed */
    private fun handOff(batch: Batch)
    {
        val started = System.nanoTime()
        try
        {
            while (writerError.get() == null)
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

    // 已送入 writer、尚未确认提交的行数与字节；只在写线程中访问
    // rows and bytes fed to the writer but not yet committed; writer thread only
    private var pendingRows: Long = 0
    private var pendingBytes: Long = 0

    private fun writeLoop(writer: BatchWriter)
    {
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
                val started = System.nanoTime()
                for (row in batch.rows)
                {
                    writer.addRow(row)
                }
                pendingRows += batch.rows.size
                pendingBytes += batch.bytes
                if (writer.writtenCount() > committed)
                {
                    metrics.recordBatch(System.nanoTime() - started)
                }
                settle(writer.writtenCount())
            }
            // close() 提交剩余不足一批的行，这也是一次批次 / close() commits the final partial batch, which counts as a batch
            closed = true
            closeCounted(writer)
        }
        catch (ex: Throwable)
        {
            if (!closed)
            {
                // 出错或取消时仍提交已读行 / rows already read are still committed on failure or cancel
                try
                {
                    closeCounted(writer)
                }
                catch (suppressed: Throwable)
                {
                    ex.addSuppressed(suppressed)
                }
            }
            settle(writer.writtenCount())
            writerError.compareAndSet(null, ex)
            // 让读取方不再阻塞在满队列上 / unblock a reader waiting on a full queue
            queue.clear()
        }
    }

    private fun closeCounted(writer: BatchWriter)
    {
        val before = writer.writtenCount()
        val started = System.nanoTime()
        writer.close()
        if (writer.writtenCount() > before)
        {
            metrics.recordBatch(System.nanoTime() - started)
        }
        settle(writer.writtenCount())
    }

    /**
     * 把新提交的行数与按比例折算的字节计入指标。整批提交时字节是精确的。
     * Credit newly committed rows and their share of the pending bytes (exact for whole batches).
     */
    private fun settle(written: Long)
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
    }

    /** 写线程失败后读取方抛出的包装异常 / wraps a writer failure seen by the reader */
    private class WriterFailedException(cause: Throwable) : RuntimeException(cause)

    companion object
    {
        private const val POLL_MS = 100L
        private val END = Batch(ArrayList(0), 0)
    }
}
