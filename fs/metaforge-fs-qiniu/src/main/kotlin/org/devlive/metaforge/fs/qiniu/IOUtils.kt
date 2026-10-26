package org.devlive.metaforge.fs.qiniu

import com.qiniu.storage.*
import com.qiniu.storage.model.DefaultPutRet
import com.qiniu.util.Auth
import org.devlive.metaforge.common.utils.JsonUtils
import org.devlive.metaforge.fs.FsRequest
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.URL

class IOUtils
{
    companion object
    {
        private fun getRemoteUrlAsStream(urlString: String): InputStream
        {
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            return BufferedInputStream(connection.getInputStream())
        }

        @JvmStatic
        fun copy(request: FsRequest, stream: InputStream, fileName: String): String?
        {
            try
            {
                val conf = Configuration(Region.autoRegion())
                conf.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2
                val manager = UploadManager(conf)
                val auth = Auth.create(request.access, request.secret)
                val token = auth.uploadToken(request.bucket, fileName)

                val response = manager.put(stream, fileName, token, null, null)
                val putRet = JsonUtils.toObject(response.bodyString(), DefaultPutRet::class.java)
                return putRet.key
            }
            catch (e: Exception)
            {
                throw RuntimeException(e)
            }
        }

        @JvmStatic
        fun reader(request: FsRequest): InputStream
        {
            val downloadUrl = DownloadUrl(request.endpoint, false, request.fileName)
            return getRemoteUrlAsStream(downloadUrl.buildURL())
        }

        @JvmStatic
        fun delete(request: FsRequest): Boolean
        {
            try
            {
                var fileName = request.fileName
                if (fileName.startsWith("/"))
                {
                    fileName = fileName.substring(1)
                }

                val conf = Configuration(Region.autoRegion())
                val auth = Auth.create(request.access, request.secret)

                val manager = BucketManager(auth, conf)
                manager.delete(request.bucket, fileName)
                return true
            }
            catch (e: Exception)
            {
                throw RuntimeException(e)
            }
        }
    }
}
