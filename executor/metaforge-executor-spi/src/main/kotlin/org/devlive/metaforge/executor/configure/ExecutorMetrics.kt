package org.devlive.metaforge.executor.configure

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

/**
 * 一次同步任务在某一时刻的指标快照，由执行器周期性上报，也随最终结果一起返回。
 * 计数类字段累计；速率为全程平均值（行/秒、字节/秒）；未知量用 -1 表示。
 *
 * A point-in-time metrics snapshot of a sync task, reported periodically and with the final result.
 * Counters are cumulative, rates are run averages (per second), and unknown values are -1.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
data class ExecutorMetrics(
    /** 从源端读取的行数 / rows read from the source */
    var rowsRead: Long = 0,
    /** 已提交到目标端的行数 / rows committed to the target */
    var rowsWritten: Long = 0,
    /** 因坏数据被跳过的行数 / rows skipped as bad data */
    var rowsSkipped: Long = 0,
    /** 写入失败的行数 / rows that failed to write */
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
    /** 各阶段进度 / per-stage progress */
    var stages: MutableList<StageMetrics> = mutableListOf()
)

enum class TotalSource
{
    /** 源端 COUNT(*) 得到的精确值 / exact, from a pre-count */
    PRECOUNT,

    /** 全量读取后已知的精确值 / exact, the source was fully materialized */
    MATERIALIZED,

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
