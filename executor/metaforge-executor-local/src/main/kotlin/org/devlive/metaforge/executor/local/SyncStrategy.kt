package org.devlive.metaforge.executor.local

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.configure.ExecutorProgressListener
import org.devlive.metaforge.executor.configure.OriginColumn
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.adapter.BatchWriter
import org.devlive.metaforge.spi.model.Configure
import org.slf4j.Logger

/** 每处理多少行上报一次进度 / 打一次日志。行为与重构前保持一致。 */
internal const val PROGRESS_INTERVAL = 1_000L

/**
 * 用 BatchWriter 跑一段写入循环，并把【所有】取消路径统一收敛成
 * `TaskCancelledException(committed)`（committed = writer.writtenCount()）：
 *
 *  1. 循环内 onRow 主动抛的 TaskCancelledException（源端还在吐行时检测到取消）
 *  2. 循环正常结束、但取消标志已置位（某些驱动 cancel 后 rs.next() 直接返回 false，不抛异常）
 *  3. 取消导致底层 fetch 抛出的任意异常（如 Statement.cancel() 引发的 SQLException）
 *
 * 无论走哪条，writer 都已 use{} 关闭并 flush，故 writtenCount() 是准确的已落库行数。
 * 非取消导致的异常原样抛出，交由上层判为 FAILURE。
 *
 * SA_LOCAL_SELF_ASSIGNMENT 是 Kotlin `use{}` 内联展开产生的字节码假阳性，非真实自赋值。
 */
@SuppressFBWarnings(value = ["SA_LOCAL_SELF_ASSIGNMENT"])
internal fun <T> BatchWriter.runCancelable(token: CancellationToken, block: (BatchWriter) -> T): T
{
    return try
    {
        val result = this.use { block(it) }
        // 路径 2：循环没抛异常，但期间收到了停止请求
        token.throwIfCancelled(this.writtenCount())
        result
    }
    catch (ex: TaskCancelledException)
    {
        // 路径 1：统一用已提交行数覆盖 processed
        throw TaskCancelledException(this.writtenCount())
    }
    catch (ex: Exception)
    {
        // 路径 3：取消引发的底层异常 -> 归一为取消；否则是真失败，原样抛出
        if (token.isCancelled)
        {
            throw TaskCancelledException(this.writtenCount())
        }
        throw ex
    }
}

/**
 * 一次同步任务所需的全部上下文。由 LocalExecutorService.start() 组装，交给具体 [SyncStrategy]。
 * 把原先 runStreaming / runLegacy 的一长串形参收敛到这里，行为不变，只是不再逐个透传。
 *
 * targetColumns / sourceKeys 是从 [originColumns] 预先派生的（name / original），
 * 避免各策略重复计算。
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
internal class SyncContext(
    val inputPlugin: PluginService,
    val inputConfigure: Configure,
    val query: String,
    val fetchSize: Int,
    val outputPlugin: PluginService,
    val outputConfigure: Configure,
    val database: String,
    val table: String,
    val originColumns: List<OriginColumn>,
    val targetColumns: List<String>,
    val sourceKeys: List<String>,
    val batchSize: Int,
    val taskLog: Logger,
    val totalCount: Long,
    val progressListener: ExecutorProgressListener?,
    val handle: TaskHandle
)

/**
 * 同步策略：把“源 -> 汇”的一次搬运抽象出来，返回已处理（读入）行数。
 * 取消时应抛出 [TaskCancelledException]（携带已处理行数），由 start() 统一转成 STOPPED。
 *
 * A single "source -> sink" copy. Returns the number of rows read. On cancellation it throws
 * [TaskCancelledException] carrying the processed count, which start() maps to STOPPED.
 */
internal interface SyncStrategy
{
    fun sync(context: SyncContext): Long
}
