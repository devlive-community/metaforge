package org.devlive.metaforge.executor.configure

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.plugin.PluginManager

/**
 * 执行器请求。字段分两类：
 * - 【所有执行器通用】taskName / userName / input / output / workHome / timeout /
 *   transform / pluginManager / progressListener
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
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
data class ExecutorRequest @JvmOverloads constructor(
    var taskName: String,
    var userName: String,
    var input: ExecutorConfigure,
    var output: ExecutorConfigure,
    var workHome: String? = null,
    var timeout: Long = 600,
    var transform: ExecutorConfigure? = null,
    var options: Map<String, String> = emptyMap(),
    var progressListener: ExecutorProgressListener? = null,
    var pluginManager: PluginManager? = null
)
{
    /** 便捷构造：仅指定 workHome 与源/汇，其余用默认值。主要给测试和简单调用使用。 */
    constructor(
        workHome: String?,
        input: ExecutorConfigure,
        output: ExecutorConfigure
    ) : this("", "", input, output, workHome)
}
