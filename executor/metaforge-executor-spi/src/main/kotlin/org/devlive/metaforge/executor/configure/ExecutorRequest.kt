package org.devlive.metaforge.executor.configure

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.plugin.PluginManager

/**
 * 执行器请求。字段分两类：
 * - 【所有执行器通用】taskName / userName / input / output / workHome / timeout /
 *   transform / pluginManager / metricsListener
 * - 【各执行器专属】统一放进 [options]（字符串 KV），由各执行器自行解析，例如：
 *     Local:     fetchSize / batchSize / preCount
 *     Seatunnel: home / startScript / way / mode / engine
 *
 * 这样新增一个执行器无需改动本类，只需读取自己关心的 option。调用方通常把
 * “该执行器的 effective 配置 Map” 直接塞进 [options]。
 *
 * Executor request split into executor-agnostic fields and an [options] bag of executor-specific
 * string settings, so adding an executor no longer requires touching this class.
 */
// BC_BAD_CAST_TO_ABSTRACT_COLLECTION: Kotlin's inlined listOfNotNull()/ifEmpty() return List values.
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
data class ExecutorRequest @JvmOverloads constructor(
    var taskName: String,
    var userName: String,
    var input: ExecutorConfigure,
    var output: ExecutorConfigure,
    var workHome: String? = null,
    var timeout: Long = 600,
    var transform: ExecutorConfigure? = null,
    var options: Map<String, String> = emptyMap(),
    var metricsListener: ExecutorMetricsListener? = null,
    var pluginManager: PluginManager? = null
)
{
    /**
     * 按执行顺序排列的转换链。为保持已编译插件的二进制兼容，作为附加属性而非构造参数；
     * 只设置了 [transform] 的旧调用方视为单个转换。
     *
     * Ordered transform chain. Additive property so compiled plugins keep the constructor signature;
     * callers that only set [transform] are treated as a single-transform chain.
     */
    var transforms: List<ExecutorConfigure> = emptyList()

    /**
     * 上次运行留下的断点，非空时从断点续传。内容由执行器定义。附加属性，保持构造签名兼容。
     * Checkpoint of an earlier run to resume from; its content belongs to the executor. Additive property.
     */
    var checkpoint: String? = null

    fun transformChain(): List<ExecutorConfigure> = transforms.ifEmpty { listOfNotNull(transform) }

    /** 便捷构造：仅指定 workHome 与源/汇，其余用默认值。主要给测试和简单调用使用。 */
    constructor(
        workHome: String?,
        input: ExecutorConfigure,
        output: ExecutorConfigure
    ) : this("", "", input, output, workHome)
}
