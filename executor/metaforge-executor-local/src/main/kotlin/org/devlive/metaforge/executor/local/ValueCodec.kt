package org.devlive.metaforge.executor.local

import com.fasterxml.jackson.databind.JsonNode

/**
 * JsonNode -> JDBC 值的转换工具。
 * 从 LocalExecutorService 原样抽出，纯函数、无状态，供 legacy BatchWriter 路径按列取真实类型值。
 *
 * Extracted from LocalExecutorService: a pure, stateless JsonNode -> JDBC value conversion used by
 * the legacy BatchWriter path.
 */
internal object ValueCodec
{
    /**
     * 把 JsonNode 转成 JDBC 可识别的真实类型；保留 NULL 语义。
     */
    fun jsonNodeToJdbcValue(node: JsonNode?): Any?
    {
        if (node == null || node.isNull) return null
        return when
        {
            node.isBoolean -> node.asBoolean()
            node.isInt -> node.asInt()
            node.isLong -> node.asLong()
            node.isBigInteger -> node.bigIntegerValue()
            node.isFloat || node.isDouble -> node.asDouble()
            node.isBigDecimal -> node.decimalValue()
            node.isBinary ->
            {
                try
                {
                    node.binaryValue()
                }
                catch (e: Exception)
                {
                    node.asText()
                }
            }

            else -> node.asText()
        }
    }
}
