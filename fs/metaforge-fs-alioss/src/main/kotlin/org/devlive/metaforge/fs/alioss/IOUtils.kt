package org.devlive.metaforge.fs.alioss

import com.aliyun.oss.OSSClientBuilder
import com.aliyun.oss.model.ObjectMetadata
import org.devlive.metaforge.fs.FsRequest
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*

class IOUtils
{
    companion object
    {
        @JvmStatic
        fun copy(request: FsRequest, stream: InputStream, fileName: String): String?
        {
            val client = OSSClientBuilder().build(request.endpoint, request.access, request.secret)
            try
            {
                client.putObject(request.bucket, fileName, stream, ObjectMetadata())
                val expiration = Date(Date().time + 3600L * 1000 * 24 * 365 * 10)
                return client.generatePresignedUrl(request.bucket, fileName, expiration).toString()
            }
            catch (e: Exception)
            {
                throw RuntimeException(e)
            }
            finally
            {
                client?.shutdown()
            }
        }

        @JvmStatic
        fun reader(request: FsRequest): InputStream
        {
            val client = OSSClientBuilder().build(request.endpoint, request.access, request.secret)
            try
            {
                val obj = client.getObject(request.bucket, request.fileName)
                val byteArray = obj.objectContent.readBytes()
                return ByteArrayInputStream(byteArray)
            }
            catch (e: Exception)
            {
                throw RuntimeException(e)
            }
            finally
            {
                client?.shutdown()
            }
        }

        @JvmStatic
        fun delete(request: FsRequest): Boolean
        {
            val client = OSSClientBuilder().build(request.endpoint, request.access, request.secret)
            try
            {
                var fileName = request.fileName
                if (fileName.startsWith("/"))
                {
                    fileName = fileName.substring(1)
                }

                client.deleteObject(request.bucket, fileName)
                return true
            }
            catch (e: Exception)
            {
                throw RuntimeException(e)
            }
            finally
            {
                client?.shutdown()
            }
        }
    }
}
