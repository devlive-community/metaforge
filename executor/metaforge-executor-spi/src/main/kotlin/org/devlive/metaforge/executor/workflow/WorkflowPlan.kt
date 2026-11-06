package org.devlive.metaforge.executor.workflow

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.configure.ExecutorConfigure

/**
 * 工作流中的一个节点，已按执行器声明解析过类型。
 * - [type]：节点插件标识（声明文件中的 id，例如 jdbc-source）
 * - [key]：节点插件在执行器内部的名字（例如 SeaTunnel 连接器名 Jdbc）
 * - [values]：用户填写的参数（未填写的可选参数不会出现）
 * - [sources]：参数类型为 `source` 的字段，由服务端按当前用户解析成已保存数据源；
 *   校验阶段为空，只有编译执行时才会提供，凭据不会离开服务端。
 *
 * One node of a workflow, typed against the executor's declaration. Fields of type `source` are
 * resolved by the server into saved data sources and handed over in [sources] for compilation only.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
data class WorkflowNode(
    val id: String,
    val type: String,
    val key: String,
    val category: WorkflowCategory,
    val label: String,
    val values: Map<String, Any?> = emptyMap(),
    val sources: Map<String, ExecutorConfigure> = emptyMap()
)
{
    fun string(field: String): String? = values[field]?.toString()?.takeIf { it.isNotBlank() }

    fun require(field: String): String = string(field) ?: throw IllegalArgumentException("$label: $field is required")

    fun source(field: String): ExecutorConfigure = sources[field] ?: throw IllegalArgumentException("$label: data source is unavailable")
}

/**
 * 交给执行器编译的工作流：节点已按“输入 → 转换… → 输出”的执行顺序排好。
 * [settings] 是执行器声明的作业参数（每个工作流独立填写），[options] 是执行器的全局运行配置；
 * 两者如何合并由执行器在编译时决定。
 *
 * A workflow handed to an executor for compilation, with nodes ordered input → transforms → output.
 * [settings] holds the executor-declared job settings of this workflow and [options] the executor's
 * runtime configuration; the executor decides how to combine them when compiling.
 */
// BC_BAD_CAST_TO_ABSTRACT_COLLECTION: Kotlin's inlined filter() returns its List destination.
@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
data class WorkflowPlan(
    val taskName: String,
    val userName: String,
    val workHome: String?,
    val timeout: Long,
    val nodes: List<WorkflowNode>,
    val settings: Map<String, Any?> = emptyMap(),
    val options: Map<String, String> = emptyMap()
)
{
    val sources: List<WorkflowNode> get() = nodes.filter { it.category == WorkflowCategory.SOURCE }

    val transforms: List<WorkflowNode> get() = nodes.filter { it.category == WorkflowCategory.TRANSFORM }

    val sinks: List<WorkflowNode> get() = nodes.filter { it.category == WorkflowCategory.SINK }

    fun source(): WorkflowNode = sources.singleOrNull() ?: throw IllegalArgumentException("The workflow requires exactly one input")

    fun sink(): WorkflowNode = sinks.singleOrNull() ?: throw IllegalArgumentException("The workflow requires exactly one output")
}

enum class WorkflowCategory(val value: String)
{
    SOURCE("source"),
    TRANSFORM("transform"),
    SINK("sink");

    companion object
    {
        @JvmStatic
        fun of(value: String?): WorkflowCategory? = values().firstOrNull { it.value == value }
    }
}
