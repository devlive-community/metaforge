package org.devlive.metaforge.fs.minio

import org.devlive.metaforge.fs.FsRequest
import org.devlive.metaforge.fs.FsResponse
import org.devlive.metaforge.fs.FsService
import org.devlive.metaforge.fs.minio.MinIOUtils.Companion.copy
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.lang.String.join

class MinIOFsService : FsService
{
    private val log = getLogger(this::class.java)

    override fun writer(request: FsRequest?): FsResponse
    {
        requireNotNull(request) { "request must not be null" }

        log.info("MinIOFs writer origin path [ {} ]", request.fileName)
        val targetPath = join(File.separator, request.endpoint, request.bucket, request.fileName)
        val response = FsResponse.builder()
            .origin(request.fileName)
            .remote(targetPath)
            .successful(true)
            .build()
        log.info("MinIOFs writer target path [ {} ]", request.fileName)
        try
        {
            val key = copy(request, request.stream, request.fileName)
            response.remote = key
            log.info("MinIOFs writer [ {} ] successfully", key)
        }
        catch (e: Exception)
        {
            log.error("MinIOFs writer error", e)
            response.isSuccessful = false
            response.message = e.message
        }
        return response
    }

    override fun reader(request: FsRequest?): FsResponse
    {
        requireNotNull(request) { "request must not be null" }

        log.info("MinIOFs reader origin path [ {} ]", request.fileName)
        val response = FsResponse.builder()
            .remote(request.fileName)
            .successful(true)
            .build()
        try
        {
            response.context = MinIOUtils.reader(request)
            log.info("MinIOFs reader [ {} ] successfully", request.fileName)
        }
        catch (e: java.lang.Exception)
        {
            log.error("MinIOFs reader error", e)
            response.isSuccessful = false
            response.message = e.message
        }
        return response
    }

    override fun delete(request: FsRequest?): FsResponse
    {
        requireNotNull(request) { "request must not be null" }

        try
        {
            val status = MinIOUtils.delete(request)
            log.info("MinIOFs delete [ {} ] successfully", request.fileName)
            return FsResponse.builder()
                .successful(status)
                .build()
        }
        catch (e: java.lang.Exception)
        {
            log.error("MinIOFs delete error", e)
            return FsResponse.builder()
                .successful(false)
                .message(e.message)
                .build()
        }
    }
}
