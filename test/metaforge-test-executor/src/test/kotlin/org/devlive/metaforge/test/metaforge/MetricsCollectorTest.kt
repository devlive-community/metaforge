package org.devlive.metaforge.test.metaforge

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.configure.Bottleneck
import org.devlive.metaforge.executor.configure.TotalSource
import org.devlive.metaforge.executor.metaforge.MetricsCollector
import org.devlive.metaforge.executor.metaforge.ValueSize
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
class MetricsCollectorTest
{
    private var now = 0L
    private val second = 1_000_000_000L

    private fun collector(): MetricsCollector
    {
        now = 0L
        return MetricsCollector { now }
    }

    private fun MetricsCollector.read(rows: Int, bytesEach: Long = 10)
    {
        repeat(rows) { recordRead(bytesEach) }
    }

    @Test
    fun countsProgressAndEtaFollowTheWrittenRows()
    {
        val metrics = collector()
        metrics.setTotal(1000, TotalSource.PRECOUNT)
        metrics.read(400)
        metrics.recordWrite(200, 2000)
        now += 2 * second

        val snapshot = metrics.sample()

        assertEquals(400L, snapshot.rowsRead)
        assertEquals(200L, snapshot.rowsWritten)
        assertEquals(4000L, snapshot.bytesRead)
        assertEquals(2000L, snapshot.bytesWritten)
        assertEquals(20.0, snapshot.progress, 1e-9)
        assertEquals(TotalSource.PRECOUNT, snapshot.totalSource)
        assertEquals(2000L, snapshot.elapsedMs)
        assertEquals(100.0, snapshot.avgWriteRowsPerSecond, 1e-9)
        // 800 行剩余 / 100 行每秒
        assertEquals(8L, snapshot.etaSeconds)

        val finished = metrics.sample(finished = true)
        assertEquals(100.0, finished.progress, 1e-9)
        assertEquals(0L, finished.etaSeconds)
    }

    @Test
    fun unknownTotalsLeaveProgressAndEtaUnknown()
    {
        val metrics = collector()
        metrics.recordWrite(50, 0)
        now += second
        val snapshot = metrics.sample()
        assertEquals(-1L, snapshot.totalRows)
        assertEquals(-1.0, snapshot.progress, 1e-9)
        assertEquals(-1L, snapshot.etaSeconds)
        assertEquals(TotalSource.UNKNOWN, snapshot.totalSource)
    }

    @Test
    fun currentRatesCoverTheLastWindowWhileAveragesCoverTheRun()
    {
        val metrics = collector()
        // 前 10 秒每秒 100 行，之后每秒 1000 行
        repeat(10) {
            metrics.recordWrite(100, 0)
            now += second
            metrics.sample()
        }
        var last = metrics.sample()
        repeat(6) {
            metrics.recordWrite(1000, 0)
            now += second
            last = metrics.sample()
        }
        assertEquals(1000.0, last.writeRowsPerSecond, 1e-6)
        assertEquals(7000.0 / 16, last.avgWriteRowsPerSecond, 1e-6)
        assertEquals(1000.0, last.peakWriteRowsPerSecond, 1e-6)
    }

    @Test
    fun theSeriesKeepsTheMostRecentSamples()
    {
        val metrics = collector()
        repeat(MetricsCollector.SERIES_SIZE + 30) {
            now += second
            metrics.sample()
        }
        val series = metrics.sample().series
        assertEquals(MetricsCollector.SERIES_SIZE, series.size)
        assertEquals((MetricsCollector.SERIES_SIZE + 30) * 1000L, series.last().at)
    }

    @Test
    fun waitingSidesNameTheBottleneck()
    {
        val metrics = collector()
        metrics.sample()
        now += 2 * second
        metrics.recordReaderWait(3 * second / 2)
        assertEquals(Bottleneck.TARGET, metrics.sample().bottleneck)

        now += 2 * second
        metrics.recordWriterWait(3 * second / 2)
        // 窗口 4 秒内两边各等了 1.5 秒，均不过半
        assertEquals(Bottleneck.NONE, metrics.sample().bottleneck)

        now += 5 * second
        metrics.recordWriterWait(4 * second)
        assertEquals(Bottleneck.SOURCE, metrics.sample().bottleneck)
    }

    @Test
    fun stagesRecordStateAndDuration()
    {
        val metrics = collector()
        metrics.beginStage("PREPARE")
        now += second / 2
        metrics.endStage("PREPARE", "DONE")
        metrics.skipStage("PRECOUNT")
        metrics.beginStage("TRANSFER")
        now += second
        metrics.endStage("TRANSFER", "FAILED")

        val stages = metrics.sample().stages
        assertEquals(listOf("PREPARE", "PRECOUNT", "TRANSFER"), stages.map { it.name })
        assertEquals(listOf("DONE", "SKIPPED", "FAILED"), stages.map { it.state })
        assertEquals(listOf(500L, 0L, 1000L), stages.map { it.elapsedMs })
    }

    @Test
    fun valueSizesFollowUtf8AndFixedWidths()
    {
        assertEquals(0L, ValueSize.of(null))
        assertEquals(5L, ValueSize.of("hello"))
        assertEquals(6L, ValueSize.of("数据"))
        assertEquals(4L, ValueSize.of("😀"))
        assertEquals(4L, ValueSize.of(42))
        assertEquals(8L, ValueSize.of(42L))
        assertEquals(8L, ValueSize.of(1.5))
        assertEquals(1L, ValueSize.of(true))
        assertEquals(3L, ValueSize.of(byteArrayOf(1, 2, 3)))
        assertEquals(6L, ValueSize.of(BigDecimal("12.34")))
        assertEquals(8L, ValueSize.of(java.sql.Timestamp(0)))
        assertEquals(9L, ValueSize.ofRow(listOf("hello", 7, null)))
    }
}
