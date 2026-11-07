package org.devlive.metaforge.executor

import org.devlive.metaforge.executor.configure.ExecutorRequest
import org.devlive.metaforge.executor.configure.ExecutorResponse
import org.devlive.metaforge.executor.workflow.WorkflowDeclaration
import org.devlive.metaforge.executor.workflow.WorkflowPlan
import org.devlive.metaforge.plugin.Service
import java.net.URL

interface ExecutorService : Service
{
    fun start(request: ExecutorRequest): ExecutorResponse

    fun stop(request: ExecutorRequest): ExecutorResponse

    /**
     * 请求暂停：执行器提交完已成形的批次后，[start] 以 PAUSED 返回并带上断点（[ExecutorResponse.checkpoint]）。
     * 把断点放进 [ExecutorRequest.checkpoint] 再次 [start] 即可续传。默认不支持。
     *
     * Ask a running task to pause: [start] then returns PAUSED with a checkpoint, which a later
     * [start] resumes from through [ExecutorRequest.checkpoint]. Unsupported by default.
     */
    fun pause(request: ExecutorRequest): ExecutorResponse =
        ExecutorResponse(false, false, null, "Executor ${name()} does not support pausing")

    /**
     * 当前 Executor 的短名。默认实现：去掉类名末尾的 "ExecutorService"。
     * 例如 MetaForgeExecutorService -> "MetaForge"，SeatunnelExecutorService -> "Seatunnel"。
     * 大小写转换由调用方按业务需要处理。
     *
     * Short name of this executor. Default: strip "ExecutorService" suffix from the class name.
     * e.g. MetaForgeExecutorService -> "MetaForge", SeatunnelExecutorService -> "Seatunnel".
     * Case conversion is the caller's responsibility.
     */
    fun name(): String = this::class.java.simpleName.removeSuffix("ExecutorService")

    /**
     * 工作流声明（能力、图限制、作业参数、节点插件）。没有声明的执行器不会出现在工作流中。
     *
     * The executor's workflow declaration: capabilities, graph limits, job settings and node plugins.
     * Executors without one are not offered for workflows.
     */
    fun workflowDeclaration(): URL? = WorkflowDeclaration.locate(this::class.java)

    /**
     * 执行器自身的图校验，补充服务端按声明做的通用校验（必填、类型、数量、连通性）。
     * 此时 [WorkflowPlan.sources] 中的数据源尚未解析。
     *
     * Executor-specific checks on top of the server's declaration-driven validation. Saved data
     * sources are not resolved at this point.
     */
    fun validate(plan: WorkflowPlan): List<String> = emptyList()

    /**
     * 把工作流编译成本执行器的请求，随后交给 [start] 执行。
     *
     * Compiles a workflow into a request for this executor, which is then passed to [start].
     */
    fun compile(plan: WorkflowPlan): ExecutorRequest =
        throw UnsupportedOperationException("Executor ${name()} does not run workflows")
}
