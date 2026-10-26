package org.devlive.metaforge.fs.minio

import org.devlive.metaforge.fs.FsRequest
import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import java.io.InputStream

class MinIOUtils
{
    companion object
    {
        private fun getClient(request: FsRequest): MinioClient
        {
            return MinioClient.builder()
                .endpoint(request.endpoint)
                .credentials(request.access, request.secret)
                .build()
        }

        @JvmStatic
        fun copy(request: FsRequest, stream: InputStream, fileName: String): String?
        {
            try
            {
                val client = getClient(request)
                val response = client.putObject(
                    PutObjectArgs.builder()
                        .stream(stream, stream.available().toLong(), - 1)
                        .bucket(request.bucket)
                        .`object`(request.fileName)
                        .build()
                )
                return response.versionId()
            }
            catch (e: Exception)
            {
                throw RuntimeException(e)
            }
        }

        @JvmStatic
        fun reader(request: FsRequest): InputStream
        {
            try
            {
                val client = getClient(request)
                val originalStream = client.getObject(
                    GetObjectArgs.builder()
                        .`object`(request.fileName)
                        .bucket(request.bucket)
                        .build()
                )

                return object : InputStream()
                {
                    override fun read(): Int
                    {
                        return originalStream.read()
                    }

                    override fun read(b: ByteArray): Int
                    {
                        return originalStream.read(b)
                    }

                    override fun read(b: ByteArray, off: Int, len: Int): Int
                    {
                        return originalStream.read(b, off, len)
                    }

                    override fun close()
                    {
                        try
                        {
                            originalStream.close()
                        }
                        catch (e: Exception)
                        {
                            throw RuntimeException(e)
                        }
                    }
                }
            }
            catch (e: Exception)
            {
                throw RuntimeException(e)
            }
        }

        @JvmStatic
        fun delete(request: FsRequest): Boolean
        {
            try
            {
                val client = getClient(request)
                client.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(request.bucket)
                        .`object`(request.fileName)
                        .build()
                )
                return true
            }
            catch (e: Exception)
            {
                throw RuntimeException(e)
            }
        }
    }
}
