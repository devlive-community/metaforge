package org.devlive.metaforge.fs.s3

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import org.devlive.metaforge.fs.FsRequest
import java.io.InputStream

class AmazonS3Utils
{
    companion object
    {
        private fun getRegion(endpoint: String): Regions
        {
            return Regions.valueOf(
                endpoint.replace('-', '_')
                    .uppercase()
            )
        }

        private fun getEndpoint(bucket: String, endpoint: String): String
        {
            // Format: metaforge-bucket.s3.us-east-2.amazonaws.com
            return String.format("%s.s3.%s.amazonaws.com", bucket, endpoint)
        }

        private fun getClient(request: FsRequest): AmazonS3
        {
            val config = ClientConfiguration()
            config.maxConnections = 200
            val endpointConfig = AwsClientBuilder.EndpointConfiguration(getEndpoint(request.bucket, request.endpoint), getRegion(request.endpoint).getName())

            val awsCredentials: AWSCredentials = BasicAWSCredentials(request.access, request.secret)
            val awsCredentialsProvider: AWSCredentialsProvider = AWSStaticCredentialsProvider(awsCredentials)

            return AmazonS3Client.builder()
                .withEndpointConfiguration(endpointConfig)
                .withClientConfiguration(config.withSignerOverride("AWSS3V4SignerType"))
                .withCredentials(awsCredentialsProvider)
                .disableChunkedEncoding()
                .withPathStyleAccessEnabled(true)
                .build()
        }

        @JvmStatic
        fun copy(request: FsRequest, stream: InputStream, fileName: String): String?
        {
            val client = getClient(request)

            try
            {
                val objectMetadata = ObjectMetadata()
                objectMetadata.contentLength = stream.available().toLong()
                objectMetadata.cacheControl = "no-cache"
                val putObject = PutObjectRequest(request.bucket, request.fileName, stream, objectMetadata)

                val response = client.putObject(putObject)
                return response.contentMd5
            }
            catch (e: Exception)
            {
                throw RuntimeException(e)
            }
            finally
            {
                client.shutdown()
            }
        }

        @JvmStatic
        fun reader(request: FsRequest): InputStream
        {
            val client = getClient(request)
            try
            {
                val getObjectRequest = GetObjectRequest(request.bucket, request.fileName)
                val s3Object = client.getObject(getObjectRequest)
                val originalStream = s3Object.objectContent

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
                        finally
                        {
                            client.shutdown()
                        }
                    }
                }
            }
            catch (e: Exception)
            {
                client.shutdown()
                throw RuntimeException(e)
            }
        }

        @JvmStatic
        fun delete(request: FsRequest): Boolean
        {
            val client = getClient(request)

            try
            {
                client.deleteObject(request.bucket, request.fileName)
                return true
            }
            catch (e: Exception)
            {
                throw RuntimeException(e)
            }
            finally
            {
                client.shutdown()
            }
        }
    }
}
