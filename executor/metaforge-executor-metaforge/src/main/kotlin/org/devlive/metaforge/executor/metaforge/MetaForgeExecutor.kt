package org.devlive.metaforge.executor.metaforge

import org.devlive.metaforge.executor.ExecutorPlugin
import org.devlive.metaforge.plugin.configure.RuntimeConfigureField
import org.devlive.metaforge.plugin.configure.RuntimeConfigureFieldType

/**
 * MetaForge 内置执行器插件：在 MetaForge 进程内完成数据同步与迁移。
 * 插件名由 [ExecutorPlugin] 按类名推导为 "MetaForge"（旧名 "Local" 由 PluginManager 别名兼容）。
 *
 * Built-in MetaForge executor for in-process data sync and migration. Its name derives to "MetaForge";
 * the former name "Local" still resolves through the PluginManager alias.
 */
class MetaForgeExecutor : ExecutorPlugin()
{
    override fun runtimeConfigures(): List<RuntimeConfigureField> = listOf(
            RuntimeConfigureField(
                    "concurrency",
                    RuntimeConfigureFieldType.NUMBER,
                    "1",
                    "Parallel read/write channels, each with its own source and target connection (at most 16). " +
                            "More than 1 needs splitting to have any effect; splits are separate queries that do not share a snapshot.",
                    true,
                    GROUP_PARALLEL
            ),
            RuntimeConfigureField(
                    "splitStrategy",
                    RuntimeConfigureFieldType.SELECT,
                    SplitPlan.NONE,
                    "How to split the query: NONE keeps one query; AUTO splits when concurrency is above 1; RANGE always splits. " +
                            "Queries that cannot be split safely (LIMIT, GROUP BY, DISTINCT, UNION and so on) or a column that is not an integer or date fall back to one query.",
                    true,
                    listOf(SplitPlan.NONE, SplitPlan.AUTO, SplitPlan.RANGE),
                    GROUP_PARALLEL
            ),
            RuntimeConfigureField(
                    "splitColumn",
                    RuntimeConfigureFieldType.STRING,
                    "",
                    "Source column to split on; empty uses the dataset's primary key, then its order-by key. Works best on an indexed, evenly spread integer or date column.",
                    true,
                    GROUP_PARALLEL
            ),
            RuntimeConfigureField(
                    "splitCount",
                    RuntimeConfigureFieldType.NUMBER,
                    "0",
                    "Number of ranges (at most 256); 0 uses four per channel, so faster channels pick up more of the work on skewed data.",
                    true,
                    GROUP_PARALLEL
            ),
            RuntimeConfigureField(
                    "fetchSize",
                    RuntimeConfigureFieldType.NUMBER,
                    "1000",
                    "JDBC fetch size for source streaming; MySQL / MariaDB will be overridden to Integer.MIN_VALUE automatically.",
                    true,
                    GROUP_THROUGHPUT
            ),
            RuntimeConfigureField(
                    "batchSize",
                    RuntimeConfigureFieldType.NUMBER,
                    "1000",
                    "Rows per batch flushed to the target via PreparedStatement.executeBatch().",
                    true,
                    GROUP_THROUGHPUT
            ),
            RuntimeConfigureField(
                    "queueCapacity",
                    RuntimeConfigureFieldType.NUMBER,
                    "4",
                    "Batches buffered between the source reader and the target writer, per channel; reading pauses when the buffer is full.",
                    true,
                    GROUP_THROUGHPUT
            ),
            RuntimeConfigureField(
                    "maxRowsPerSecond",
                    RuntimeConfigureFieldType.NUMBER,
                    "0",
                    "Upper limit on rows read per second across all channels, to protect the source. 0 means no limit.",
                    true,
                    GROUP_THROUGHPUT
            ),
            RuntimeConfigureField(
                    "maxBytesPerSecond",
                    RuntimeConfigureFieldType.NUMBER,
                    "0",
                    "Upper limit on estimated bytes read per second across all channels. 0 means no limit.",
                    true,
                    GROUP_THROUGHPUT
            ),
            RuntimeConfigureField(
                    "preCount",
                    RuntimeConfigureFieldType.BOOLEAN,
                    "false",
                    "Run SELECT COUNT(*) over the user query before sync to populate the progress denominator. " +
                            "Default OFF: derived-table wrapping is expensive on MySQL/InnoDB for large tables.",
                    true,
                    GROUP_GENERAL
            ),
            RuntimeConfigureField(
                    "timeout",
                    RuntimeConfigureFieldType.NUMBER,
                    "0",
                    "Whole-task timeout in seconds; when exceeded the sync is cancelled and marked TIMEOUT. " +
                            "Default 0 disables the timeout (sync runs to completion).",
                    true,
                    GROUP_GENERAL
            )
    )

    private companion object
    {
        const val GROUP_PARALLEL = "parallel"
        const val GROUP_THROUGHPUT = "throughput"
        const val GROUP_GENERAL = "general"
    }
}
