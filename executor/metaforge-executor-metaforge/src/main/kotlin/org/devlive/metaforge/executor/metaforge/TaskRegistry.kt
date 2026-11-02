package org.devlive.metaforge.executor.metaforge

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * 进程内活跃任务表 + 取消线程池。
 *
 * 多个并发 sync 共享同一实例（Kotlin object 单例），用 taskName 唯一索引；
 * stop() 通过 taskName 查到 handle 后设置取消标志位。
 * 之前这些是 MetaForgeExecutorService 的 companion static 成员——为了让不同 MetaForgeExecutorService
 * 实例之间的 stop() 仍能互相找到任务，这里必须保持“单例、跨实例共享”的语义，故用 object。
 *
 * Statement.cancel() 可能会阻塞（驱动会新开连接发 KILL），放到独立线程跑避免拖住调用方。
 */
@SuppressFBWarnings(value = ["RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"])
internal object TaskRegistry
{
    private val runningTasks: ConcurrentHashMap<String, TaskHandle> = ConcurrentHashMap()

    private val cancelExecutor: java.util.concurrent.ExecutorService =
            Executors.newCachedThreadPool { r ->
                val t = Thread(r, "metaforge-executor-cancel")
                t.isDaemon = true
                t
            }

    // 单线程调度器：仅负责“到点触发超时取消”，动作本身很轻（设标志位 + 提交 cancel 到 cancelExecutor）
    private val timeoutScheduler: java.util.concurrent.ScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor { r ->
                val t = Thread(r, "metaforge-executor-timeout")
                t.isDaemon = true
                t
            }

    fun register(taskName: String, handle: TaskHandle)
    {
        runningTasks[taskName] = handle
    }

    /** 仅当当前登记的仍是同一个 handle 时才移除，避免误删同名的后续任务 */
    fun unregister(taskName: String, handle: TaskHandle)
    {
        runningTasks.remove(taskName, handle)
    }

    fun find(taskName: String): TaskHandle? = runningTasks[taskName]

    fun submitCancel(block: () -> Unit)
    {
        cancelExecutor.submit(block)
    }

    // 指标定时上报：动作是采样 + 回调监听方，监听方可能落库，所以与超时调度分开
    private val metricsScheduler: java.util.concurrent.ScheduledExecutorService =
            Executors.newScheduledThreadPool(2) { r ->
                val t = Thread(r, "metaforge-executor-metrics")
                t.isDaemon = true
                t
            }

    /** 按固定间隔上报指标；任务结束时取消返回的 future。 */
    fun scheduleMetrics(intervalMs: Long, block: () -> Unit): ScheduledFuture<*> =
            metricsScheduler.scheduleWithFixedDelay(block, intervalMs, intervalMs, TimeUnit.MILLISECONDS)

    /** 安排一次性超时触发；返回的 future 供任务正常结束时取消，避免事后误触发。 */
    fun scheduleTimeout(seconds: Long, block: () -> Unit): ScheduledFuture<*> =
            timeoutScheduler.schedule(block, seconds, TimeUnit.SECONDS)
}
