package org.devlive.metaforge.fs.s3

import org.devlive.metaforge.fs.FsRequest
import org.devlive.metaforge.fs.FsResponse
import org.devlive.metaforge.fs.FsService
import org.devlive.metaforge.fs.s3.AmazonS3Utils.Companion.copy
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.lang.String.join

class AmazonS3FsService : FsService
{
    private val log = getLogger(AmazonS3FsService::class.java)

    override fun writer(request: FsRequest?): FsResponse
    {
        requireNotNull(request) { "request must not be null" }

        log.info("AmazonS3Fs writer origin path [ {} ]", request.fileName)
        val targetPath = join(File.separator, request.endpoint, request.bucket, request.fileName)
        val response = FsResponse.builder()
            .origin(request.fileName)
            .remote(targetPath)
            .successful(true)
            .build()
        log.info("AmazonS3Fs writer target path [ {} ]", request.fileName)
        try
        {
            val key = copy(request, request.stream, request.fileName)
            response.remote = key
            log.info("AmazonS3Fs writer [ {} ] successfully", key)
        }
        catch (e: Exception)
        {
            log.error("AmazonS3Fs writer error", e)
            response.isSuccessful = false
            response.message = e.message
        }
        return response
    }

    override fun reader(request: FsRequest?): FsResponse
    {
        requireNotNull(request) { "request must not be null" }

        log.info("AmazonS3Fs reader origin path [ {} ]", request.fileName)
        val response = FsResponse.builder()
            .remote(request.fileName)
            .successful(true)
            .build()
        try
        {
            response.context = AmazonS3Utils.reader(request)
            log.info("AmazonS3Fs reader [ {} ] successfully", request.fileName)
        }
        catch (e: java.lang.Exception)
        {
            log.error("AmazonS3Fs reader error", e)
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
            val status = AmazonS3Utils.delete(request)
            log.info("AmazonS3Fs delete [ {} ] successfully", request.fileName)
            return FsResponse.builder()
                .successful(status)
                .build()
        }
        catch (e: java.lang.Exception)
        {
            log.error("AmazonS3Fs delete error", e)
            return FsResponse.builder()
                .successful(false)
                .message(e.message)
                .build()
        }
    }
}
