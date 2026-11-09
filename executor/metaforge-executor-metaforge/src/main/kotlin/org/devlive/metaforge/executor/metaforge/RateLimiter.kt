package org.devlive.metaforge.executor.metaforge

/**
 * 所有通道共享的限流器：行/秒与字节/秒两个上限，0 表示不限。按“下一次可用时间”记账：
 * 本次占用算到之后的调用头上，长期速率等于上限，空闲时间不积累额度，不会出现突发。
 *
 * Rate limit shared by all channels, in rows and bytes per second (0 means unlimited). Each call pays
 * for the previous ones, so the long-run rate matches the limit and idle time earns no burst.
 *
 * @param clock 纳秒时钟，测试可注入 / nanosecond clock, injectable for tests
 */
class RateLimiter(
    private val maxRowsPerSecond: Long,
    private val maxBytesPerSecond: Long,
    private val clock: () -> Long = System::nanoTime
)
{
    private var rowsFreeAt = Long.MIN_VALUE
    private var bytesFreeAt = Long.MIN_VALUE

    val enabled: Boolean
        get() = maxRowsPerSecond > 0 || maxBytesPerSecond > 0

    /** 占用额度，返回调用方需要等待的纳秒数 / reserve capacity and return the nanoseconds to wait */
    @Synchronized
    fun reserve(rows: Long, bytes: Long): Long
    {
        val now = clock()
        var wait = 0L
        if (maxRowsPerSecond > 0)
        {
            val start = maxOf(rowsFreeAt, now)
            wait = maxOf(wait, start - now)
            rowsFreeAt = start + (rows * NANOS / maxRowsPerSecond.toDouble()).toLong()
        }
        if (maxBytesPerSecond > 0)
        {
            val start = maxOf(bytesFreeAt, now)
            wait = maxOf(wait, start - now)
            bytesFreeAt = start + (bytes * NANOS / maxBytesPerSecond.toDouble()).toLong()
        }
        return wait
    }

    /**
     * 占用额度并等待，等待期间每隔一小段检查 [stopped] 以便及时响应取消；返回实际等待的纳秒数。
     * Reserve and wait, checking [stopped] between short sleeps; returns the nanoseconds waited.
     */
    fun acquire(rows: Long, bytes: Long, stopped: () -> Boolean): Long
    {
        val wait = reserve(rows, bytes)
        if (wait <= 0)
        {
            return 0
        }
        val started = clock()
        val until = started + wait
        while (!stopped())
        {
            val left = until - clock()
            if (left <= 0)
            {
                break
            }
            Thread.sleep(minOf(left / 1_000_000 + 1, SLICE_MS))
        }
        return clock() - started
    }

    /** 每次占用的行数，约 1/10 秒的量，让读取平滑 / rows per reservation, about a tenth of a second's worth */
    fun chunkRows(channels: Int): Int =
        if (maxRowsPerSecond > 0) (maxRowsPerSecond / (10L * channels.coerceAtLeast(1))).coerceIn(1, MAX_CHUNK).toInt() else MAX_CHUNK.toInt()

    private companion object
    {
        const val NANOS = 1_000_000_000.0
        const val SLICE_MS = 100L
        const val MAX_CHUNK = 256L
    }
}
