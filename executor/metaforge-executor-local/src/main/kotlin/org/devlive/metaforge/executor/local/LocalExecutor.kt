package org.devlive.metaforge.executor.local

import org.devlive.metaforge.executor.ExecutorPlugin
import org.devlive.metaforge.plugin.configure.PluginConfigureField
import org.devlive.metaforge.plugin.configure.PluginFieldType

class LocalExecutor : ExecutorPlugin()
{
    override fun configures(): List<PluginConfigureField> = listOf(
            PluginConfigureField(
                    "fetchSize",
                    PluginFieldType.NUMBER,
                    "1000",
                    "JDBC fetch size for source streaming; MySQL / MariaDB will be overridden to Integer.MIN_VALUE automatically.",
                    true
            ),
            PluginConfigureField(
                    "batchSize",
                    PluginFieldType.NUMBER,
                    "1000",
                    "Rows per batch flushed to the target via PreparedStatement.executeBatch().",
                    true
            ),
            PluginConfigureField(
                    "preCount",
                    PluginFieldType.BOOLEAN,
                    "false",
                    "Run SELECT COUNT(*) over the user query before sync to populate the progress denominator. " +
                            "Default OFF: derived-table wrapping is expensive on MySQL/InnoDB for large tables.",
                    true
            ),
            PluginConfigureField(
                    "timeout",
                    PluginFieldType.NUMBER,
                    "0",
                    "Whole-task timeout in seconds; when exceeded the sync is cancelled and marked TIMEOUT. " +
                            "Default 0 disables the timeout (sync runs to completion).",
                    true
            )
    )
}
