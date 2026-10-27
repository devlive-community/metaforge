package org.devlive.metaforge.executor

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginType

/**
 * 所有 Executor 插件的统一基类。
 * - 类型固定为 EXECUTOR
 * - 名称自动从子类类名末尾剥掉 "Executor" 后缀。
 *   例如 SeatunnelExecutor -> "Seatunnel"，LocalExecutor -> "Local"。
 * - 可配置字段通过 Plugin.runtimeConfigures() 声明（继承自 metaforge-plugin 通用机制）。
 *
 * Common base for all executor plugins.
 * - Type is fixed to EXECUTOR.
 * - Name auto-derives from the subclass simple name by stripping the trailing "Executor".
 * - Configurable fields are declared via Plugin.runtimeConfigures() (inherited from metaforge-plugin).
 */
abstract class ExecutorPlugin : Plugin()
{
    override fun getType(): PluginType = PluginType.EXECUTOR

    override fun getName(): String = this::class.java.simpleName.removeSuffix("Executor")
}
