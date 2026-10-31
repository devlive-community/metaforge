package org.devlive.metaforge.convert.json

import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.common.utils.DateUtils
import org.devlive.metaforge.convert.ConvertService
import org.devlive.metaforge.convert.FileConvert.formatFile
import org.devlive.metaforge.convert.model.ConvertRequest
import org.devlive.metaforge.convert.model.ConvertResponse
import org.slf4j.LoggerFactory.getLogger
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Objects.requireNonNull

@SuppressFBWarnings(value = ["REC_CATCH_EXCEPTION"])
class JsonConvertService : ConvertService
{
    private val log = getLogger(this::class.java)

    override fun format(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        try
        {
            log.info("${name()} format start time [ ${DateUtils.now()} ]")
            log.info("${name()} format headers start")
            response.headers = request.headers
            log.info("${name()} format headers end")

            log.info("${name()} format columns start")
            val mapper = ObjectMapper()
            val columns = mutableListOf<Any>()
            request.columns
                .forEach { column ->
                    val jsonNode = mapper.createObjectNode()
                    for (headerIndex in request.headers.indices)
                    {
                        val header = request.headers[headerIndex] as String
                        when (column)
                        {
                            is List<*> ->
                            {
                                val value = column[headerIndex]
                                when (value)
                                {
                                    is LocalDateTime ->
                                    {
                                        val timeString = value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                        jsonNode.put(header, timeString)
                                    }

                                    else -> jsonNode.putPOJO(header, value)
                                }
                            }

                            else -> jsonNode.putPOJO(header, column)
                        }
                    }
                    columns.add(jsonNode)
                }
            response.columns = columns
            log.info("${name()} format columns end")

            log.info("${name()} format end time [ ${DateUtils.now()} ]")
            response.successful = true
        }
        catch (e: IOException)
        {
            response.successful = false
            response.message = e.message
        }
        return response
    }

    override fun formatStream(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        try
        {
            requireNonNull("Stream must not be null")

            log.info("${name()} format stream start time [ ${DateUtils.now()} ]")
            val mapper = ObjectMapper()
            request.stream
                ?.let {
                    BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader ->
                        val jsonNode: JsonNode = mapper.readTree(reader)
                        log.info("${name()} format stream json node count [ ${jsonNode.size()} ]")

                        val headers = mutableListOf<Any>()
                        if (jsonNode.isArray && jsonNode.size() > 0)
                        {
                            jsonNode[0].fieldNames()
                                .forEachRemaining { headers.add(it) }
                        }
                        response.headers = headers

                        val columns = mutableListOf<Any>()
                        if (jsonNode.isArray)
                        {
                            jsonNode.elements()
                                .forEachRemaining { node ->
                                    val column = mutableMapOf<String, Any?>()
                                    node.fields()
                                        .forEachRemaining { field ->
                                            column[field.key] = field.value
                                        }
                                    columns.add(column)
                                }
                        }
                        response.columns = columns
                        it.close()
                    }
                }
            log.info("${name()} format stream end time [ ${DateUtils.now()} ]")
            response.successful = true
        }
        catch (e: IOException)
        {
            response.successful = false
            response.message = e.message
        }
        return response
    }

    override fun writer(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        try
        {
            log.info("${name()} writer origin path [ ${request.path} ]")
            log.info("${name()} writer start time [ ${DateUtils.now()} ]")
            val file = formatFile(request, name())
            log.info("${name()} writer file absolute path [ ${file.absolutePath} ]")

            val factory = JsonFactory()
            factory.createGenerator(file, JsonEncoding.UTF8).use { generator ->
                generator.codec = ObjectMapper()
                if (request.includeStructure)
                {
                    generator.writeStartObject()
                    generator.writeArrayFieldStart("columns")
                    request.headers.forEachIndexed { index, header ->
                        generator.writeStartObject()
                        generator.writeStringField("name", header.toString())
                        generator.writeStringField("type", request.types.getOrNull(index)?.toString())
                        generator.writeEndObject()
                    }
                    generator.writeEndArray()
                    generator.writeFieldName("rows")
                }
                generator.writeStartArray()
                request.columns.forEach { column ->
                    generator.writeStartObject()
                    for (headerIndex in request.headers.indices)
                    {
                        val header = request.headers[headerIndex] as String
                        when (column)
                        {
                            is List<*> ->
                            {
                                val value = column[headerIndex]
                                if (value == null && request.excludeNull)
                                {
                                    continue
                                }
                                if (value is LocalDateTime)
                                {
                                    generator.writeStringField(header, value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                                }
                                else
                                {
                                    generator.writeObjectField(header, value)
                                }
                            }

                            is ObjectNode ->
                            {
                                val value = column.get(header)
                                if ((value == null || value.isNull) && request.excludeNull)
                                {
                                    continue
                                }
                                if (value != null && isDateTime(value.asText()))
                                {
                                    generator.writeStringField(header, value.asText())
                                }
                                else
                                {
                                    generator.writeObjectField(header, value)
                                }
                            }

                            is LocalDateTime ->
                            {
                                generator.writeStringField(header, column.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                            }

                            else ->
                            {
                                generator.writeObjectField(header, column)
                            }
                        }
                    }
                    generator.writeEndObject()
                }
                generator.writeEndArray()
                if (request.includeStructure)
                {
                    generator.writeEndObject()
                }
            }

            log.info("${name()} writer end time [ ${DateUtils.now()} ]")
            response.path = file.absolutePath
            response.successful = true
        }
        catch (e: Exception)
        {
            log.error("Write JSON file failed", e)
            response.successful = false
            response.message = e.message
        }
        return response
    }

    private fun isDateTime(text: String): Boolean
    {
        return try
        {
            LocalDateTime.parse(text)
            true
        }
        catch (e: DateTimeParseException)
        {
            false
        }
    }

    override fun reader(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        try
        {
            log.info("${name()} reader origin path [ ${request.path} ]")
            log.info("${name()} reader start time [ ${DateUtils.now()} ]")
            val file = formatFile(request, name())
            log.info("${name()} reader file absolute path [ ${file.absolutePath} ]")

            val mapper = ObjectMapper()
            val jsonNode: JsonNode = mapper.readTree(file)
            log.info("${name()} reader file json node count [ ${jsonNode.size()} ]")

            log.info("${name()} reader file headers start")
            val headers = mutableListOf<Any>()
            if (jsonNode.isArray && jsonNode.size() > 0)
            {
                jsonNode[0].fieldNames()
                    .forEachRemaining { headers.add(it) }
            }
            response.headers = headers
            log.info("${name()} reader file headers end")

            log.info("${name()} reader file columns start")
            val columns = mutableListOf<Any>()
            if (jsonNode.isArray)
            {
                jsonNode.elements()
                    .forEachRemaining { node ->
                        val column = mutableMapOf<String, Any?>()
                        node.fields()
                            .forEachRemaining { field ->
                                column[field.key] = field.value
                            }
                        columns.add(column)
                    }
            }
            response.columns = columns
            log.info("${name()} reader file columns end")
            response.successful = true
        }
        catch (e: Exception)
        {
            response.successful = false
            response.message = e.message
        }
        return response
    }
}
