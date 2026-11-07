package org.devlive.metaforge.executor.metaforge

import java.io.IOException
import java.sql.SQLException
import java.sql.SQLNonTransientConnectionException
import java.sql.SQLRecoverableException
import java.sql.SQLTransientException

/**
 * 判断一次读写错误的性质，决定重试、定位坏行还是直接失败：
 * - [Kind.TRANSIENT]：断连、死锁、服务关闭等，再试可能成功
 * - [Kind.DATA]：这批数据里有目标端不接受的值
 * - [Kind.FATAL]：语法、权限、对象不存在等，重试或跳行都没有意义
 *
 * Classifies a read or write error to choose between retrying, isolating bad rows and failing.
 */
internal object SyncErrors
{
    enum class Kind
    {
        TRANSIENT, DATA, FATAL
    }

    private val TRANSIENT_STATES = listOf("08", "40", "53", "57P", "HYT")
    private val FATAL_STATES = listOf("42", "28", "3D", "3F", "0A", "25")
    private val TRANSIENT_TEXT = listOf(
        "connection refused", "connection reset", "connection closed", "broken pipe", "communications link failure",
        "lost connection", "connection is closed", "this connection has been closed", "server closed", "read timed out",
        "failed to respond", "no route to host", "deadlock", "open jdbc connection failed",
        "no operations allowed after", "connection has been closed"
    )

    fun classify(error: Throwable): Kind
    {
        var sawSql = false
        var fatal = false
        for (cause in chain(error))
        {
            if (cause is SQLTransientException || cause is SQLRecoverableException || cause is SQLNonTransientConnectionException || cause is IOException)
            {
                return Kind.TRANSIENT
            }
            if (cause is SQLException)
            {
                sawSql = true
                val state = cause.sqlState.orEmpty().uppercase()
                if (TRANSIENT_STATES.any { state.startsWith(it) })
                {
                    return Kind.TRANSIENT
                }
                if (FATAL_STATES.any { state.startsWith(it) })
                {
                    fatal = true
                }
            }
            val message = cause.message.orEmpty().lowercase()
            if (TRANSIENT_TEXT.any { message.contains(it) })
            {
                return Kind.TRANSIENT
            }
        }
        return when
        {
            fatal || !sawSql -> Kind.FATAL
            else -> Kind.DATA
        }
    }

    /** 异常及其原因，JDBC 批量错误还要看 next exception / the error, its causes and JDBC next exceptions */
    private fun chain(error: Throwable): List<Throwable>
    {
        val seen = LinkedHashSet<Throwable>()
        val pending = ArrayDeque<Throwable>()
        pending.add(error)
        while (pending.isNotEmpty() && seen.size < 32)
        {
            val current = pending.removeFirst()
            if (seen.add(current))
            {
                current.cause?.let { pending.add(it) }
                (current as? SQLException)?.nextException?.let { pending.add(it) }
            }
        }
        return seen.toList()
    }
}
