package org.devlive.metaforge.executor.local

import org.devlive.metaforge.executor.ExecutorPlugin
import org.devlive.metaforge.plugin.configure.RuntimeConfigureField
import org.devlive.metaforge.plugin.configure.RuntimeConfigureFieldType

class LocalExecutor : ExecutorPlugin()
{
    override fun runtimeConfigures(): List<RuntimeConfigureField> = listOf(
            RuntimeConfigureField(
                    "fetchSize",
                    RuntimeConfigureFieldType.NUMBER,
                    "1000",
                    "JDBC fetch size for source streaming; MySQL / MariaDB will be overridden to Integer.MIN_VALUE automatically.",
                    true
            ),
            RuntimeConfigureField(
                    "batchSize",
                    RuntimeConfigureFieldType.NUMBER,
                    "1000",
                    "Rows per batch flushed to the target via PreparedStatement.executeBatch().",
                    true
            ),
            RuntimeConfigureField(
                    "preCount",
                    RuntimeConfigureFieldType.BOOLEAN,
                    "false",
                    "Run SELECT COUNT(*) over the user query before sync to populate the progress denominator. " +
                            "Default OFF: derived-table wrapping is expensive on MySQL/InnoDB for large tables.",
                    true
            ),
            RuntimeConfigureField(
                    "timeout",
                    RuntimeConfigureFieldType.NUMBER,
                    "0",
                    "Whole-task timeout in seconds; when exceeded the sync is cancelled and marked TIMEOUT. " +
                            "Default 0 disables the timeout (sync runs to completion).",
                    true
            )
    )
}
