package org.devlive.metaforge.convert.xml

import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.dataformat.xml.XmlFactory
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.devlive.metaforge.common.utils.DateUtils
import org.devlive.metaforge.convert.ConvertService
import org.devlive.metaforge.convert.FileConvert.formatFile
import org.devlive.metaforge.convert.model.ConvertOption
import org.devlive.metaforge.convert.model.ConvertRequest
import org.devlive.metaforge.convert.model.choice
import org.devlive.metaforge.convert.model.ConvertResponse
import org.slf4j.LoggerFactory.getLogger
import java.io.ByteArrayOutputStream
import java.io.IOException

class XmlConvertService : ConvertService
{
    private val log = getLogger(this::class.java)
    private val root = "Root"
    private val node = "Node"

    override fun format(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        try
        {
            log.info("${name()} format start time [ ${DateUtils.now()} ]")
            response.headers = request.headers
            val factory = XmlFactory()
            val outputStream = ByteArrayOutputStream()
            factory.createGenerator(outputStream, JsonEncoding.UTF8)
                .use { generator ->
                    generator.codec = XmlMapper()
                    val staxWriter = generator.staxWriter
                    staxWriter.writeStartElement(root)
                    request.columns
                        .forEach { column ->
                            staxWriter.writeStartElement(node)
                            for (headerIndex in request.headers.indices)
                            {
                                when (column)
                                {
                                    is List<*> -> staxWriter.writeAttribute(request.headers[headerIndex].toString(), column[headerIndex].toString())
                                    else -> throw UnsupportedOperationException("Unsupported column type")
                                }
                            }
                            staxWriter.writeEndElement()
                        }
                    staxWriter.writeEndElement()
                }
            val xmlString = outputStream.toString(Charsets.UTF_8)
            response.columns = listOf(xmlString)
            log.info("${name()} format end time [ ${DateUtils.now()} ]")
            response.successful = true
        }
        catch (e: IOException)
        {
            e.printStackTrace()
            response.successful = false
            response.message = e.message
        }
        return response
    }

    override fun formatStream(request: ConvertRequest): ConvertResponse
    {
        TODO("Not yet implemented")
    }

    override fun options(): List<ConvertOption>
    {
        return listOf(
            ConvertOption("rootElement", mapOf("zh" to "根元素名", "en" to "Root element"), root),
            ConvertOption("rowElement", mapOf("zh" to "行元素名", "en" to "Row element"), node),
            ConvertOption("nulls", mapOf("zh" to "NULL 值处理", "en" to "NULL values"), "keep",
                listOf(choice("keep", "写为 null", "Write null"), choice("omit", "省略属性", "Leave the attribute out")))
        )
    }

    override fun writer(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        try
        {
            log.info("${name()} writer start time [ ${DateUtils.now()} ]")
            val file = formatFile(request, name())
            log.info("${name()} writer file absolute path [ ${file.absolutePath} ]")

            val factory = XmlFactory()
            factory.createGenerator(file, JsonEncoding.UTF8)
                .use { generator ->
                    generator.codec = XmlMapper()
                    val staxWriter = generator.staxWriter
                    val rowElement = request.option("rowElement", node)
                    val omitNull = request.excludeNull || request.option("nulls", "keep") == "omit"
                    staxWriter.writeStartElement(request.option("rootElement", root))
                    if (request.includeStructure)
                    {
                        staxWriter.writeStartElement("Columns")
                        request.headers.forEachIndexed { index, header ->
                            staxWriter.writeStartElement("Column")
                            staxWriter.writeAttribute("name", header.toString())
                            staxWriter.writeAttribute("type", request.types.getOrNull(index)?.toString() ?: "")
                            staxWriter.writeEndElement()
                        }
                        staxWriter.writeEndElement()
                    }
                    request.columns
                        .forEach { column ->
                            staxWriter.writeStartElement(rowElement)
                            for (headerIndex in request.headers.indices)
                            {
                                when (column)
                                {
                                    is List<*> ->
                                    {
                                        val value = column[headerIndex]
                                        if (value != null || ! omitNull)
                                        {
                                            staxWriter.writeAttribute(request.headers[headerIndex].toString(), value.toString())
                                        }
                                    }
                                    else -> throw UnsupportedOperationException("Unsupported column type")
                                }
                            }
                            staxWriter.writeEndElement()
                        }
                    staxWriter.writeEndElement()
                }
            log.info("${name()} writer end time [ ${DateUtils.now()} ]")
            response.path = file.absolutePath
            response.successful = true
        }
        catch (e: IOException)
        {
            response.successful = false
            response.message = e.message
        }
        return response
    }

    override fun reader(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        try
        {
            log.info("${name()} reader start time [ ${DateUtils.now()} ]")
            val file = formatFile(request, name())
            log.info("${name()} reader file absolute path [ ${file.absolutePath} ]")

            val mapper = XmlMapper()
            val rootNode: Map<String, List<Map<String, Any>>> = mapper.readValue(file, Map::class.java) as Map<String, List<Map<String, Any>>>
            if (rootNode.isNotEmpty() && rootNode.containsKey(node))
            {
                val headers = mutableListOf<Any>()
                val columns = mutableListOf<Any>()
                when (val xmlElement = rootNode[node])
                {
                    is List<*> ->
                    {
                        xmlElement[0].keys
                            .forEach { headers.add(it) }

                        xmlElement.forEach { columns.add(it.values) }
                    }

                    else -> throw UnsupportedOperationException("Unsupported column type")
                }
                response.headers = headers
                response.columns = columns
            }
            log.info("${name()} reader end time [ ${DateUtils.now()} ]")
            response.successful = true
        }
        catch (e: IOException)
        {
            response.successful = false
            response.message = e.message
        }
        return response
    }
}
