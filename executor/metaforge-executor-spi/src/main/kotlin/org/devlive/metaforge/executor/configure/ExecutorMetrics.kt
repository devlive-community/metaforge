package org.devlive.metaforge.executor.configure

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

/**
 * 一次同步任务在某一时刻的指标快照，由执行器周期性上报，也随最终结果一起返回。
 * 计数类字段累计；无前缀的速率是最近几秒的瞬时值，avg 前缀为全程平均；未知量用 -1 表示。
 *
 * A point-in-time metrics snapshot of a sync task, reported periodically and with the final result.
 * Counters are cumulative; plain rates cover the last few seconds and avg rates the whole run;
 * unknown values are -1.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
data class ExecutorMetrics(
    /** 从源端读取的行数 / rows read from the source */
    var rowsRead: Long = 0,
    /** 已提交到目标端的行数 / rows committed to the target */
    var rowsWritten: Long = 0,
    /** 被目标端拒绝、在容错上限内跳过的行数，见 [dirtySamples] / rows the target rejected and were skipped within the limit */
    var rowsSkipped: Long = 0,
    /** 任务失败时未能写入的行数 / rows that could not be written when the task failed */
    var rowsFailed: Long = 0,
    /** 读取的字节数（按值估算）/ estimated bytes read */
    var bytesRead: Long = 0,
    /** 写出的字节数（按值估算）/ estimated bytes written */
    var bytesWritten: Long = 0,
    /** 源端总行数，未知为 -1 / source total rows, -1 when unknown */
    var totalRows: Long = -1,
    /** 总行数的来源 / where [totalRows] comes from */
    var totalSource: TotalSource = TotalSource.UNKNOWN,
    /** 进度百分比 0-100，总量未知为 -1 / progress percent, -1 when the total is unknown */
    var progress: Double = -1.0,
    /** 已运行毫秒数 / milliseconds since the task started */
    var elapsedMs: Long = 0,
    /** 预计剩余秒数，无法估算为 -1 / estimated seconds left, -1 when unknown */
    var etaSeconds: Long = -1,
    var readRowsPerSecond: Double = 0.0,
    var writeRowsPerSecond: Double = 0.0,
    var readBytesPerSecond: Double = 0.0,
    var writeBytesPerSecond: Double = 0.0,
    var avgReadRowsPerSecond: Double = 0.0,
    var avgWriteRowsPerSecond: Double = 0.0,
    /** 写入速率峰值（按采样窗口）/ peak write rate over the sampling windows */
    var peakWriteRowsPerSecond: Double = 0.0,
    /** 暂时性错误后重试的批次次数 / batch retries after transient errors */
    var batchRetries: Long = 0,
    /** 已提交的批次数 / committed batches */
    var batchesCommitted: Long = 0,
    /** 每批写入平均耗时（毫秒）/ average write time per batch in ms */
    var avgBatchMs: Double = 0.0,
    /** 并行通道数 / parallel channels */
    var channels: Int = 1,
    /** 读写之间排队的批次数与容量 / batches waiting between reader and writer, and the capacity */
    var queueDepth: Int = 0,
    var queueCapacity: Int = 0,
    /** 读取方因队列满而等待的累计毫秒（目标端慢）/ ms readers waited on a full queue (slow target) */
    var readerWaitMs: Long = 0,
    /** 写入方因队列空而等待的累计毫秒（源端慢）/ ms writers waited on an empty queue (slow source) */
    var writerWaitMs: Long = 0,
    /** 读取方被限流等待的累计毫秒 / ms readers were held back by the rate limit */
    var throttleWaitMs: Long = 0,
    /** 最近窗口的瓶颈 / the bottleneck over the last window */
    var bottleneck: Bottleneck = Bottleneck.NONE,
    /** 能否暂停并精确续传 / whether the task can pause and resume exactly */
    var resumable: Boolean = false,
    /** 实际采用的分片方式 NONE / RANGE / the split strategy actually used */
    var splitStrategy: String = "NONE",
    /** 分片列，未分片为 null / the split column, null without splitting */
    var splitColumn: String? = null,
    /** 各分片进度 / per-split progress */
    var splits: MutableList<SplitMetrics> = mutableListOf(),
    /** 被跳过的行的样本，最多 20 条 / samples of the skipped rows, at most 20 */
    var dirtySamples: MutableList<DirtyRow> = mutableListOf(),
    /** 各阶段进度 / per-stage progress */
    var stages: MutableList<StageMetrics> = mutableListOf(),
    /** 最近的速率采样，按时间先后 / recent rate samples, oldest first */
    var series: MutableList<MetricsSample> = mutableListOf()
)

enum class Bottleneck
{
    NONE,

    /** 写入方在等数据：源端读取慢 / writers wait for rows: the source is the limit */
    SOURCE,

    /** 读取方在等队列：目标端写入慢 / readers wait for room: the target is the limit */
    TARGET,

    /** 限流生效中 / the rate limit is holding readers back */
    THROTTLE
}

/**
 * 单个分片的进度。state 取 PENDING / RUNNING / DONE / FAILED / CANCELLED。
 * Progress of one split; state is PENDING / RUNNING / DONE / FAILED / CANCELLED.
 */
data class SplitMetrics(
    var id: Int = 0,
    /** 分片范围的可读描述 / readable description of the split's range */
    var range: String = "",
    var state: String = "PENDING",
    /** 处理它的通道，尚未开始为 0 / the channel handling it, 0 before it starts */
    var channel: Int = 0,
    var rowsRead: Long = 0,
    var rowsWritten: Long = 0,
    var elapsedMs: Long = 0,
    /** 暂时性错误后重试的批次次数 / batch retries after transient errors */
    var retries: Long = 0,
    /** 已提交到的分片键，续传由此继续 / the split key committed so far, where a resume continues */
    var watermark: String? = null
)

/** 一条被目标端拒绝的行 / one row the target rejected */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
data class DirtyRow(
    var split: Int = 0,
    /** 目标端的错误信息 / the target's error */
    var error: String = "",
    /** 目标列名到值，长值已截断 / target column to value, long values truncated */
    var row: MutableMap<String, Any?> = linkedMapOf()
)

/** 一个速率采样点 / one rate sample */
data class MetricsSample(
    /** 距任务开始的毫秒数 / ms since the task started */
    var at: Long = 0,
    var readRowsPerSecond: Double = 0.0,
    var writeRowsPerSecond: Double = 0.0
)

enum class TotalSource
{
    /** 源端 COUNT(*) 得到的精确值 / exact, from a pre-count */
    PRECOUNT,

    /** 全量读取后已知的精确值 / exact, the source was fully materialized */
    MATERIALIZED,

    /** 总行数未知，进度按已完成分片数估算 / total unknown, progress is estimated from finished splits */
    SPLITS,

    /** 任务成功结束、源端已读完时的精确值 / exact, the run finished after reading the whole source */
    COMPLETED,

    UNKNOWN
}

/**
 * 单个阶段的进度。state 取 PENDING / RUNNING / DONE / FAILED / SKIPPED。
 * Progress of one stage; state is PENDING / RUNNING / DONE / FAILED / SKIPPED.
 */
data class StageMetrics(
    var name: String = "",
    var state: String = "PENDING",
    var startedAt: Long = 0,
    var elapsedMs: Long = 0
)
