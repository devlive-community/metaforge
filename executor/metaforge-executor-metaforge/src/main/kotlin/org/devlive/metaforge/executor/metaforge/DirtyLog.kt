package org.devlive.metaforge.executor.metaforge

import com.fasterxml.jackson.databind.ObjectMapper
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.executor.configure.DirtyRow
import org.slf4j.Logger
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * 记录被目标端拒绝的行：全部追加到工作目录的 `<任务名>.dirty.jsonl`，前 [MAX_SAMPLES] 条留作指标样本。
 * 多条通道会同时写入，因此加锁。文件写不了只记日志，不影响同步。
 *
 * Records rejected rows: all of them in `<task>.dirty.jsonl` under the work home, the first few as
 * metric samples. Channels write concurrently, hence the lock; a file problem is only logged.
 */
@SuppressFBWarnings(value = ["EI_EXPOSE_REP2", "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
internal class DirtyLog(workHome: String?, taskName: String, private val columns: List<String>, private val taskLog: Logger)
{
    private val file: File? = if (workHome.isNullOrBlank() || taskName.isBlank()) null else File(workHome, "$taskName.dirty.jsonl")
    private val mapper = ObjectMapper()
    private val samples = ArrayList<DirtyRow>()
    private var count = 0L

    @Synchronized
    fun samples(): MutableList<DirtyRow> = samples.map { it.copy(row = LinkedHashMap(it.row)) }.toMutableList()

    @Synchronized
    fun record(split: Int, row: List<Any?>, error: Throwable)
    {
        val message = rootMessage(error)
        val values = linkedMapOf<String, Any?>()
        columns.forEachIndexed { index, column -> values[column] = plain(row.getOrNull(index)) }
        val dirty = DirtyRow(split, message, values)
        if (samples.size < MAX_SAMPLES)
        {
            samples.add(dirty.copy(row = LinkedHashMap(values.mapValues { (_, value) -> (value as? String)?.take(MAX_SAMPLE_VALUE) ?: value })))
        }
        count ++
        taskLog.warn("Skipped row {} of split {}: {}", count, split, message)
        if (file != null)
        {
            try
            {
                file.parentFile?.mkdirs()
                Files.write(
                    file.toPath(), (mapper.writeValueAsString(dirty) + "\n").toByteArray(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
                )
            }
            catch (ex: Exception)
            {
                taskLog.warn("Write dirty row to {} failed: {}", file, ex.message)
            }
        }
    }

    private fun plain(value: Any?): Any? = when (value)
    {
        null, is Number, is Boolean, is String -> value
        is ByteArray -> "[binary ${value.size} bytes]"
        else -> value.toString()
    }

    private fun rootMessage(error: Throwable): String
    {
        var current = error
        var depth = 0
        while (current.cause != null && depth < 16)
        {
            current = current.cause!!
            depth ++
        }
        return (current as? java.sql.SQLException)?.nextException?.message ?: current.message ?: error.toString()
    }

    private companion object
    {
        const val MAX_SAMPLES = 20
        const val MAX_SAMPLE_VALUE = 256
    }
}
