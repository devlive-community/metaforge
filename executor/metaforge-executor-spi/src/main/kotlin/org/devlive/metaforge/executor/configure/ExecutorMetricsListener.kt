package org.devlive.metaforge.executor.configure

/**
 * 执行器指标回调。执行器按自身节奏调用，实现方应尽快返回，避免拖慢读取。
 * Metrics callback invoked by the executor; implementations should return quickly.
 */
fun interface ExecutorMetricsListener
{
    fun onMetrics(metrics: ExecutorMetrics)
}
