package org.devlive.metaforge.executor.common

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
    INTERRUPTED
}
