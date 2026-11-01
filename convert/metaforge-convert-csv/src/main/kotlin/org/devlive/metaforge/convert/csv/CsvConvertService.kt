package org.devlive.metaforge.convert.csv

import com.google.common.base.Preconditions.checkState
import org.devlive.metaforge.common.utils.DateUtils
import org.devlive.metaforge.convert.ConvertService
import org.devlive.metaforge.convert.FileConvert
import org.devlive.metaforge.convert.model.ConvertOption
import org.devlive.metaforge.convert.model.ConvertRequest
import org.devlive.metaforge.convert.model.choice
import org.devlive.metaforge.convert.model.ConvertResponse
import org.apache.commons.csv.CSVFormat
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory.getLogger
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Objects.requireNonNull

class CsvConvertService : ConvertService
{
    private val log = getLogger(this::class.java)

    override fun format(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        try
        {
            val csvFormat = CSVFormat.newFormat(',')
            log.info("${name()} format start time [ ${DateUtils.now()} ]")
            response.headers = request.headers

            val columns = mutableListOf<Any>()
            request.columns
                .forEach { columns.add(csvFormat.format(it)) }
            response.columns = columns
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
        val delimiter = ","
        try
        {
            requireNonNull("Stream must not be null")

            log.info("${name()} format stream start time [ ${DateUtils.now()} ]")
            request.stream
                ?.let {
                    BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader ->
                        response.headers = reader.readLine()
                            .split(delimiter)

                        val columns = mutableListOf<Any>()
                        reader.readLines()
                            .forEach { line -> columns.add(line.split(delimiter)) }
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

    override fun options(): List<ConvertOption>
    {
        return listOf(
            ConvertOption("delimiter", mapOf("zh" to "分隔符", "en" to "Delimiter"), ",",
                listOf(choice(",", "逗号 ,", "Comma ,"), choice(";", "分号 ;", "Semicolon ;"), choice("\t", "制表符", "Tab"), choice("|", "竖线 |", "Pipe |"))),
            ConvertOption("header", mapOf("zh" to "首行字段名", "en" to "Header row"), "true",
                listOf(choice("true", "包含", "Included"), choice("false", "不包含", "Left out"))),
            ConvertOption("nulls", mapOf("zh" to "NULL 值处理", "en" to "NULL values"), "empty",
                listOf(choice("empty", "空单元格", "Empty cell"), choice("NULL", "写为 NULL", "Write NULL")))
        )
    }

    override fun writer(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        try
        {
            log.info("${name()} writer start time [ ${DateUtils.now()} ]")
            response.headers = request.headers
            val file = FileConvert.formatFile(request, name())
            val delimiter = request.option("delimiter", ",").replace("\\t", "\t")
            val nullText = if (request.excludeNull) "" else request.option("nulls", "empty").let { if (it == "NULL") "NULL" else "" }
            val lines = mutableListOf<String>()
            if (request.option("header", "true") != "false")
            {
                lines.add(response.headers.joinToString(delimiter) { csvCell(it.toString(), delimiter) })
            }
            // The column types follow the header as a second line.
            if (request.includeStructure)
            {
                lines.add(request.headers.indices.joinToString(delimiter) { csvCell(request.types.getOrNull(it)?.toString() ?: "", delimiter) })
            }

            val columns = mutableListOf<Any>()
            request.columns
                .forEach {
                    when (it)
                    {
                        is List<*> -> lines.add(it.joinToString(separator = delimiter) { value -> if (value == null) nullText else csvCell(value.toString(), delimiter) })
                        else -> lines.add(it.toString())
                    }
                }
            FileUtils.writeStringToFile(file, lines.joinToString("\n"), Charsets.UTF_8)
            response.columns = columns
            log.info("${name()} writer end time [ ${DateUtils.now()} ]")
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
        val delimiter = ","
        try
        {
            log.info("${name()} reader origin path [ ${request.path} ]")
            val file = FileConvert.formatFile(request, name())

            val lines = FileUtils.readLines(file, Charsets.UTF_8)
            checkState(lines.isNotEmpty(), "The file is empty")
            log.info("${name()} reader file line count [ ${lines.size} ]")
            response.headers = lines.first()
                .split(delimiter)

            val columns = mutableListOf<Any>()
            lines.drop(1)
                .forEach {
                    columns.add(it.split(delimiter))
                }
            response.columns = columns

            log.info("${name()} reader end time [ ${DateUtils.now()} ]")
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

/** A cell quoted when it holds the delimiter, a quote or a line break, quotes doubled (RFC 4180). */
internal fun csvCell(value: String, delimiter: String): String
{
    return if (value.contains(delimiter) || value.contains('"') || value.contains('\n') || value.contains('\r'))
    {
        "\"" + value.replace("\"", "\"\"") + "\""
    }
    else value
}
