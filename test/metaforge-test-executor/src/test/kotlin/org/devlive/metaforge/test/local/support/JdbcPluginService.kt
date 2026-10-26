package org.devlive.metaforge.test.local.support

import com.fasterxml.jackson.databind.ObjectMapper
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.PluginType
import org.devlive.metaforge.spi.adapter.BatchWriter
import org.devlive.metaforge.spi.adapter.RowCallback
import org.devlive.metaforge.spi.model.Configure
import org.devlive.metaforge.spi.model.Response
import java.math.BigDecimal
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

/**
 * 直连真实 JDBC 数据库的 PluginService，用于对真实容器库（Testcontainers）做端到端同步测试。
 *
 * 与 [FakePluginService] 不同：这里 executeStream / openBatchWriter / execute 都打真实 JDBC——
 * 真实的流式 ResultSet 读取、真实的 PreparedStatement 批量写、真实的 Statement.cancel()。
 * 它绕开 metaforge 自己的 JdbcConnection/PluginClassLoader 封装（那部分由 driver/plugin 测试模块覆盖），
 * 从而稳定地验证 LocalExecutorService 在真实源/目标库之间的搬运正确性。
 */
@SuppressFBWarnings(value = [
    "BC_BAD_CAST_TO_ABSTRACT_COLLECTION",
    "OBL_UNSATISFIED_OBLIGATION",
    "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE",
    "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING"
])
class JdbcPluginService(
    private val url: String,
    private val user: String,
    private val password: String,
    private val defaultFetchSize: Int = 1000
) : PluginService
{
    override fun supportsStreaming(): Boolean = true

    override fun type(): PluginType = PluginType.JDBC

    private fun open(): Connection
    {
        val conn = DriverManager.getConnection(url, user, password)
        conn.autoCommit = false
        return conn
    }

    override fun executeStream(configure: Configure, content: String, fetchSize: Int, callback: RowCallback)
    {
        val conn = open()
        try
        {
            conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { st ->
                st.fetchSize = if (fetchSize > 0) fetchSize else defaultFetchSize
                callback.onStatement(st)
                st.executeQuery(content).use { rs ->
                    val md = rs.metaData
                    val n = md.columnCount
                    val headers = (1..n).map { i -> md.getColumnLabel(i) ?: md.getColumnName(i) }
                    val types = (1..n).map { i -> md.getColumnTypeName(i) }
                    callback.onSchema(headers, types)
                    while (rs.next())
                    {
                        val row = ArrayList<Any?>(n)
                        for (i in 1..n)
                        {
                            row.add(rs.getObject(i))
                        }
                        callback.onRow(row)
                    }
                }
            }
        }
        finally
        {
            try
            {
                conn.close()
            }
            catch (ignore: Exception)
            {
            }
        }
    }

    override fun openBatchWriter(
        configure: Configure,
        database: String,
        table: String,
        columns: List<String>,
        batchSize: Int
    ): BatchWriter
    {
        val conn = open()
        val cols = columns.joinToString(", ") { "\"$it\"" }
        val placeholders = columns.joinToString(", ") { "?" }
        val target = if (database.isNotEmpty()) "\"$database\".\"$table\"" else "\"$table\""
        val sql = "INSERT INTO $target ($cols) VALUES ($placeholders)"
        val ps = conn.prepareStatement(sql)
        return RealJdbcBatchWriter(conn, ps, columns.size, if (batchSize > 0) batchSize else 1000)
    }

    override fun execute(configure: Configure, content: String): Response
    {
        return try
        {
            open().use { conn ->
                conn.createStatement().use { st ->
                    val isResultSet = st.execute(content)
                    if (!isResultSet)
                    {
                        conn.commit()
                        return Response.builder().isSuccessful(true).build()
                    }
                    st.resultSet.use { rs ->
                        val md = rs.metaData
                        val n = md.columnCount
                        val mapper = ObjectMapper()
                        val rows = ArrayList<Any>()
                        while (rs.next())
                        {
                            val node = mapper.createObjectNode()
                            for (i in 1..n)
                            {
                                val name = md.getColumnLabel(i) ?: md.getColumnName(i)
                                when (val v = rs.getObject(i))
                                {
                                    null -> node.putNull(name)
                                    is Int -> node.put(name, v)
                                    is Long -> node.put(name, v)
                                    is Boolean -> node.put(name, v)
                                    is Double -> node.put(name, v)
                                    is Float -> node.put(name, v.toDouble())
                                    is BigDecimal -> node.put(name, v)
                                    else -> node.put(name, v.toString())
                                }
                            }
                            rows.add(node)
                        }
                        Response.builder().isSuccessful(true).columns(rows).build()
                    }
                }
            }
        }
        catch (ex: Exception)
        {
            Response.builder().isSuccessful(false).message(ex.message).build()
        }
    }

    private class RealJdbcBatchWriter(
        private val conn: Connection,
        private val ps: java.sql.PreparedStatement,
        private val columnCount: Int,
        private val batchSize: Int
    ) : BatchWriter
    {
        private var pending = 0
        private var committed = 0L

        override fun addRow(row: List<*>)
        {
            for (i in 0 until columnCount)
            {
                ps.setObject(i + 1, row[i])
            }
            ps.addBatch()
            pending++
            if (pending >= batchSize)
            {
                flush()
            }
        }

        override fun writtenCount(): Long = committed

        override fun close()
        {
            try
            {
                if (pending > 0)
                {
                    flush()
                }
            }
            finally
            {
                try
                {
                    ps.close()
                }
                catch (ignore: Exception)
                {
                }
                try
                {
                    conn.close()
                }
                catch (ignore: Exception)
                {
                }
            }
        }

        private fun flush()
        {
            ps.executeBatch()
            conn.commit()
            committed += pending
            pending = 0
            ps.clearBatch()
        }
    }
}
