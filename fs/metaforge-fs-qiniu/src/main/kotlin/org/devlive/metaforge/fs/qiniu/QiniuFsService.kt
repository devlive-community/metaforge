package org.devlive.metaforge.fs.qiniu

import org.devlive.metaforge.fs.FsRequest
import org.devlive.metaforge.fs.FsResponse
import org.devlive.metaforge.fs.FsService
import org.devlive.metaforge.fs.qiniu.IOUtils.Companion.copy
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.lang.String.join

class QiniuFsService : FsService
{
    private val log = getLogger(QiniuFsService::class.java)

    override fun writer(request: FsRequest?): FsResponse
    {
        requireNotNull(request) { "request must not be null" }

        log.info("QiniuFs writer origin path [ {} ]", request.fileName)
        val targetPath = join(File.separator, request.endpoint, request.bucket, request.fileName)
        val response = FsResponse.builder()
            .origin(request.fileName)
            .remote(targetPath)
            .successful(true)
            .build()
        log.info("QiniuFs writer target path [ {} ]", request.fileName)
        try
        {
            val key = copy(request, request.stream, request.fileName)
            response.remote = key
            log.info("QiniuFs writer [ {} ] successfully", key)
        }
        catch (e: Exception)
        {
            log.error("QiniuFs writer error", e)
            response.isSuccessful = false
            response.message = e.message
        }
        return response
    }

    override fun reader(request: FsRequest?): FsResponse
    {
        requireNotNull(request) { "request must not be null" }

        log.info("QiniuFs reader origin path [ {} ]", request.fileName)
        val response = FsResponse.builder()
            .remote(request.fileName)
            .successful(true)
            .build()
        try
        {
            response.context = IOUtils.reader(request)
            log.info("QiniuFs reader [ {} ] successfully", request.fileName)
        }
        catch (e: java.lang.Exception)
        {
            log.error("QiniuFs reader error", e)
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
            val status = IOUtils.delete(request)
            log.info("QiniuFs delete [ {} ] successfully", request.fileName)
            return FsResponse.builder()
                .successful(status)
                .build()
        }
        catch (e: java.lang.Exception)
        {
            log.error("QiniuFs delete error", e)
            return FsResponse.builder()
                .successful(false)
                .message(e.message)
                .build()
        }
    }
}
