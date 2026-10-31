package org.devlive.metaforge.convert.csv

import com.google.common.base.Preconditions.checkState
import org.devlive.metaforge.common.utils.DateUtils
import org.devlive.metaforge.convert.ConvertService
import org.devlive.metaforge.convert.FileConvert
import org.devlive.metaforge.convert.model.ConvertRequest
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

    override fun writer(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        try
        {
            log.info("${name()} writer start time [ ${DateUtils.now()} ]")
            response.headers = request.headers
            val file = FileConvert.formatFile(request, name())
            FileUtils.writeStringToFile(file, response.headers.joinToString(","), Charsets.UTF_8)
            // The column types follow the header as a second line.
            if (request.includeStructure)
            {
                FileUtils.writeStringToFile(file, "\n" + request.headers.indices.joinToString(",") { request.types.getOrNull(it)?.toString() ?: "" }, Charsets.UTF_8, true)
            }

            val columns = mutableListOf<Any>()
            request.columns
                .forEach {
                    FileUtils.writeStringToFile(file, "\n", Charsets.UTF_8, true)
                    when (it)
                    {
                        is List<*> -> FileUtils.writeStringToFile(file, it.joinToString(separator = ",") { value -> if (value == null && request.excludeNull) "" else value.toString() }, Charsets.UTF_8, true)
                        else -> FileUtils.writeStringToFile(file, it.toString(), Charsets.UTF_8, true)
                    }
                }
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
