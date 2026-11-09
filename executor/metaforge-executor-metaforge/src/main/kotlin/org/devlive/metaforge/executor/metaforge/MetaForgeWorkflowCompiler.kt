package org.devlive.metaforge.executor.metaforge

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.configure.ExecutorRequest
import org.devlive.metaforge.executor.configure.OriginColumn
import org.devlive.metaforge.executor.workflow.WorkflowPlan

/**
 * 把“已保存数据源读取 → 批量写入”的工作流编译成 MetaForge 执行请求。
 * 执行器约定目标库/表、查询和字段映射都放在 input 上（与数据集同步一致）。
 *
 * Compiles a "saved source query → batch write" workflow into a MetaForge request. The executor
 * expects the target database/table, query and column mapping on the input, as dataset sync does.
 */
// BC_BAD_CAST_TO_ABSTRACT_COLLECTION: Kotlin's inlined filter()/mapTo() return their List/Set destinations.
@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
internal object MetaForgeWorkflowCompiler
{
    /** 输出样本行数上限 / maximum number of written rows returned as the output sample */
    const val PREVIEW_ROWS = "50"

    /** 可按工作流覆盖的执行器配置 / executor options a workflow may override */
    private val SETTINGS = setOf(
        "fetchSize", "batchSize", "queueCapacity", "preCount",
        "concurrency", "splitStrategy", "splitColumn", "splitCount", "maxRowsPerSecond", "maxBytesPerSecond",
        "maxErrorRows", "writeRetries", "writeRetryBackoffMs"
    )

    fun compile(plan: WorkflowPlan): ExecutorRequest
    {
        if (plan.transforms.isNotEmpty())
        {
            throw IllegalArgumentException("The MetaForge executor does not support transforms")
        }
        val sourceNode = plan.source()
        val sinkNode = plan.sink()
        val input = sourceNode.source("source")
        val output = sinkNode.source("source")
        val target = output.plugin ?: throw IllegalArgumentException("${sinkNode.label}: data source plugin is unavailable")
        if (!target.supportsStreaming())
        {
            throw IllegalArgumentException("${sinkNode.label}: the target data source does not support batch writing")
        }
        input.query = sourceNode.require("query")
        input.database = sinkNode.require("database")
        input.table = sinkNode.require("table")
        input.originColumns = columns(sinkNode.values["columns"], sinkNode.label)

        val options = LinkedHashMap(plan.options)
        plan.settings.filterKeys { it in SETTINGS }.forEach { (key, value) ->
            value?.toString()?.takeIf { it.isNotBlank() }?.let { options[key] = it }
        }
        // Workflows count the source first unless the workflow turns it off, so the total and progress
        // are known before transferring; the executor-wide default stays off for dataset syncs.
        if (plan.settings["preCount"] == null || plan.settings["preCount"].toString().isBlank())
        {
            options["preCount"] = "true"
        }
        options["previewRows"] = PREVIEW_ROWS

        val request = ExecutorRequest(plan.taskName, plan.userName, input, output)
        request.workHome = plan.workHome
        request.timeout = plan.timeout
        request.options = options
        return request
    }

    /** source=target 映射，保持用户填写顺序 / source=target mapping in the order the user wrote it */
    private fun columns(mapping: Any?, label: String): Set<OriginColumn>
    {
        val entries = (mapping as? Map<*, *>)?.entries?.filter { it.key != null && it.value != null }
        if (entries.isNullOrEmpty())
        {
            throw IllegalArgumentException("$label: add source-to-target column mappings")
        }
        return entries.mapTo(LinkedHashSet()) { OriginColumn(it.value.toString(), it.key.toString()) }
    }
}
