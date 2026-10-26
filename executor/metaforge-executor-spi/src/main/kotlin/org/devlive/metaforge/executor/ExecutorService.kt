package org.devlive.metaforge.executor

import org.devlive.metaforge.executor.configure.ExecutorRequest
import org.devlive.metaforge.executor.configure.ExecutorResponse
import org.devlive.metaforge.plugin.Service

interface ExecutorService : Service
{
    fun start(request: ExecutorRequest): ExecutorResponse

    fun stop(request: ExecutorRequest): ExecutorResponse

    /**
     * 当前 Executor 的短名。默认实现：去掉类名末尾的 "ExecutorService"。
     * 例如 LocalExecutorService -> "Local"，SeatunnelExecutorService -> "Seatunnel"。
     * 大小写转换由调用方按业务需要处理。
     *
     * Short name of this executor. Default: strip "ExecutorService" suffix from the class name.
     * e.g. LocalExecutorService -> "Local", SeatunnelExecutorService -> "Seatunnel".
     * Case conversion is the caller's responsibility.
     */
    fun name(): String = this::class.java.simpleName.removeSuffix("ExecutorService")
}
