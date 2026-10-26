package org.devlive.metaforge.fs.cos

import org.devlive.metaforge.fs.FsRequest
import org.devlive.metaforge.fs.FsResponse
import org.devlive.metaforge.fs.FsService
import org.devlive.metaforge.fs.cos.TencentCosUtils.Companion.copy
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.lang.String.join

class TencentCosFsService : FsService
{
    private val log = getLogger(TencentCosFsService::class.java)

    override fun writer(request: FsRequest?): FsResponse
    {
        requireNotNull(request) { "request must not be null" }

        log.info("TencentCos writer origin path [ {} ]", request.fileName)
        val targetPath = join(File.separator, request.endpoint, request.bucket, request.fileName)
        val response = FsResponse.builder()
            .origin(request.fileName)
            .remote(targetPath)
            .successful(true)
            .build()
        log.info("TencentCos writer target path [ {} ]", request.fileName)
        try
        {
            val key = copy(request, request.stream, request.fileName)
            response.remote = key
            log.info("TencentCos writer [ {} ] successfully", key)
        }
        catch (e: Exception)
        {
            log.error("TencentCos writer error", e)
            response.isSuccessful = false
            response.message = e.message
        }
        return response
    }

    override fun reader(request: FsRequest?): FsResponse
    {
        requireNotNull(request) { "request must not be null" }

        log.info("TencentCos reader origin path [ {} ]", request.fileName)
        val response = FsResponse.builder()
            .remote(request.fileName)
            .successful(true)
            .build()
        try
        {
            response.context = TencentCosUtils.reader(request)
            log.info("TencentCos reader [ {} ] successfully", request.fileName)
        }
        catch (e: java.lang.Exception)
        {
            log.error("TencentCos reader error", e)
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
            val status = TencentCosUtils.delete(request)
            log.info("TencentCos delete [ {} ] successfully", request.fileName)
            return FsResponse.builder()
                .successful(status)
                .build()
        }
        catch (e: java.lang.Exception)
        {
            log.error("TencentCos delete error", e)
            return FsResponse.builder()
                .successful(false)
                .message(e.message)
                .build()
        }
    }
}
