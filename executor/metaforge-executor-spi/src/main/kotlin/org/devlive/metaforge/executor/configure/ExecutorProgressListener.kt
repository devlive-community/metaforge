package org.devlive.metaforge.executor.configure

/**
 * 执行器进度上报回调。执行器按一定行数节奏调用。
 * Progress listener called by the executor at a fixed row-count cadence.
 *
 * @param processed 已成功写入目标的行数 / rows already written to the target
 * @param total     源端总行数；未知时传 -1 / source total rows; pass -1 when unknown
 */
fun interface ExecutorProgressListener
{
    fun onProgress(processed: Long, total: Long)
}
