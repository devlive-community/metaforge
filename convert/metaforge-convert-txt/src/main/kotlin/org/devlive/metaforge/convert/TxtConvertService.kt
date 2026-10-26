package org.devlive.metaforge.convert

import com.google.common.base.Preconditions.checkArgument
import com.google.common.base.Preconditions.checkState
import org.devlive.metaforge.common.utils.DateUtils
import org.devlive.metaforge.convert.model.ConvertRequest
import org.devlive.metaforge.convert.model.ConvertResponse
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.ObjectUtils.isNotEmpty
import org.slf4j.LoggerFactory.getLogger
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Objects.requireNonNull

class TxtConvertService : ConvertService
{
    private val log = getLogger(this::class.java)

    override fun format(request: ConvertRequest): ConvertResponse
    {
        val response = ConvertResponse()
        try
        {
            checkArgument(isNotEmpty(request.delimiter), "Delimiter must not be empty")

            log.info("${name()} format start time [ ${DateUtils.now()} ]")
            request.delimiter
                ?.let { delimiter ->
                    log.info("${name()} format headers start")
                    val headers = mutableListOf<Any>()
                    request.headers
                        .let { line ->
                            headers.add(line.joinToString(separator = delimiter))
                        }
                    response.headers = headers
                    log.info("${name()} format headers end")

                    log.info("${name()} format columns start")
                    val columns = mutableListOf<Any>()
                    request.columns
                        .forEach { line ->
                            when (line)
                            {
                                is List<*> -> columns.add(line.joinToString(separator = delimiter))
                                else -> columns.add(line)
                            }
                        }
                    response.columns = columns
                    log.info("${name()} format columns end")
                }

            log.info("${name()} format end time [ ${DateUtils.now()} ]")
            response.successful = true
        }
        catch (e: Exception)
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
            request.delimiter
                ?.let { delimiter ->
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
            checkArgument(isNotEmpty(request.delimiter), "Delimiter must not be empty")

            log.info("${name()} writer origin path [ ${request.path} ]")
            log.info("${name()} writer start time [ ${DateUtils.now()} ]")
            val file = FileConvert.formatFile(request, name())
            log.info("${name()} writer file absolute path [ ${file.absolutePath} ]")

            request.delimiter
                ?.let { delimiter ->
                    log.info("${name()} writer file headers start")
                    request.headers
                        .let { headers ->
                            FileUtils.writeStringToFile(file, headers.joinToString(separator = delimiter), Charsets.UTF_8)
                        }
                    log.info("${name()} writer file headers end")

                    log.info("${name()} writer file columns start")
                    request.columns
                        .forEach {
                            FileUtils.writeStringToFile(file, "\n", Charsets.UTF_8, true)
                            when (it)
                            {
                                is List<*> -> FileUtils.writeStringToFile(file, it.joinToString(separator = delimiter), Charsets.UTF_8, true)
                                else -> FileUtils.writeStringToFile(file, it.toString(), Charsets.UTF_8, true)
                            }
                        }
                    log.info("${name()} writer file columns end")
                }

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
            checkArgument(isNotEmpty(request.delimiter), "Delimiter must not be empty")

            log.info("${name()} reader origin path [ ${request.path} ]")
            log.info("${name()} reader start time [ ${DateUtils.now()} ]")
            val file = FileConvert.formatFile(request, name())
            log.info("${name()} reader file absolute path [ ${file.absolutePath} ]")

            request.delimiter
                ?.let { delimiter ->
                    val lines = FileUtils.readLines(file, Charsets.UTF_8)
                    checkState(lines.isNotEmpty(), "The file is empty")
                    log.info("${name()} reader file line count [ ${lines.size} ]")

                    log.info("${name()} reader file headers start")
                    response.headers = lines.first()
                        .split(delimiter)
                    log.info("${name()} reader file headers end")

                    log.info("${name()} reader file columns start")
                    val columns = mutableListOf<Any>()
                    lines.drop(1)
                        .forEach {
                            columns.add(it.split(delimiter))
                        }
                    response.columns = columns
                    log.info("${name()} reader file columns end")
                }

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
