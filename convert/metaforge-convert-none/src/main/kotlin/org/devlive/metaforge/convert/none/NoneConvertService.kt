package org.devlive.metaforge.convert.none

import com.google.common.base.Preconditions.checkState
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.common.utils.DateUtils
import org.devlive.metaforge.convert.ConvertService
import org.devlive.metaforge.convert.FileConvert
import org.devlive.metaforge.convert.model.ConvertRequest
import org.devlive.metaforge.convert.model.ConvertResponse
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory.getLogger
import java.io.IOException
import java.util.Objects.requireNonNull

@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"])
class NoneConvertService : ConvertService
{
    private val log = getLogger(this::class.java)

    override fun format(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        try
        {
            log.info("${name()} format start time [ ${DateUtils.now()} ]")
            response.headers = request.headers
            val columns = mutableListOf<Any>()
            request.columns
                .forEach { columns.add(it) }

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
        try
        {
            requireNonNull("Stream must not be null")

            log.info("${name()} format stream start time [ ${DateUtils.now()} ]")
            response.headers = request.headers
            val columns = mutableListOf<Any>()
            request.columns
                .forEach { columns.add(it) }
            response.columns = columns
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
            val file = FileConvert.formatFile(request, name())
            log.info("${name()} writer file absolute path [ ${file.absolutePath} ]")

            val content = StringBuilder(request.headers.toString())
            request.columns
                .forEach {
                    content.append("\n")
                    content.append(it.toString())
                }
            FileUtils.writeStringToFile(file, content.toString(), Charsets.UTF_8)

            log.info("${name()} writer end time [ ${DateUtils.now()} ]")
            response.path = file.absolutePath
            response.successful = true
        }
        catch (e: Exception)
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
            log.info("${name()} reader origin path [ ${request.path} ]")
            log.info("${name()} reader start time [ ${DateUtils.now()} ]")
            val file = FileConvert.formatFile(request, name())
            log.info("${name()} reader file absolute path [ ${file.absolutePath} ]")

            val lines = FileUtils.readLines(file, Charsets.UTF_8)
            checkState(lines.isNotEmpty(), "The file is empty")
            log.info("${name()} reader file line count [ ${lines.size} ]")

            response.headers = parseLine(lines.first())

            val columns = lines.drop(1)
                .map { parseLine(it) }
            response.columns = columns.toList()

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

    private fun parseLine(line: String): List<String>
    {
        return line.trim()
            .removeSurrounding("[", "]")
            .split(", ")
            .map { it.trim() }
    }
}
