package org.devlive.metaforge.executor.common

/**
 * 同步任务状态。字符串值直接落库，已有值不可改名。合法转换见 [RunStateMachine]。
 * Sync task states, stored by name; transitions are defined in [RunStateMachine].
 */
enum class RunState {
    CREATED,
    TIMEOUT,
    QUEUE,
    RUNNING,
    FAILURE,
    SUCCESS,
    STOPPING,
    STOPPED,

    /**
     * 服务异常终止后留下的未完结任务在重启时被标记为此状态。
     * Task was alive when the server died; recovery on next startup marks it as such.
     */
    INTERRUPTED,

    /** 已请求暂停，正在提交在途批次 / Pause requested, in-flight batches are being committed */
    PAUSING,

    /** 已暂停，可继续 / Paused and resumable */
    PAUSED,

    /** 可重试的失败后等待下一次尝试 / Waiting for the next attempt after a retryable failure */
    RETRYING
}
