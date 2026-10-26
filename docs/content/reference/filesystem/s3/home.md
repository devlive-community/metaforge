---
title: 亚马逊云 S3
---

在 MetaForge 中目前已经支持亚马逊云 S3，支持功能如下：

- 上传文件
- 预览&下载文件
- 删除文件

### MetaForge 使用配置

---

在 MetaForge 中文件系统用于配置系统的缓冲以及临时文件的存储。修改 `application.properties` 文件如下内容：

```properties
metaforge.fs.type=AmazonS3
metaforge.fs.access=
metaforge.fs.secret=
metaforge.fs.endpoint=
metaforge.fs.bucket=
```

- `metaforge.fs.type`: 文件系统的类型只能是 **AmazonS3**
- `metaforge.fs.access`: 亚马逊云的 AccessKey
- `metaforge.fs.secret`: 亚马逊云的 SecretKey
- `metaforge.fs.endpoint`: 亚马逊云的 Endpoint，如 `cn-north-1`，不需要输入整个的地址
- `metaforge.fs.bucket`: 亚马逊云的存储桶名称，如 `metaforge`

### 依赖使用方式

---

该插件支持第三方依赖方式引入，引入依赖如下：

```xml
<dependency>
    <groupId>org.devlive.metaforge</groupId>
    <artifactId>metaforge-fs-amazon-s3</artifactId>
    <version>${VERSION}</version>
</dependency>
```

使用代码如下：

```kotlin
package org.devlive.metaforge.fs.s3

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import org.devlive.metaforge.fs.FsService
import org.devlive.metaforge.fs.FsManager
import org.devlive.metaforge.fs.FsRequest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class AmazonS3FsTest
{
    private val log = LoggerFactory.getLogger(AmazonS3FsTest::class.java)
    private val name = "AmazonS3"
    private var request = FsRequest()
    private var injector: Injector? = null

    @Before
    fun before()
    {
        request.access = System.getProperty("access")
        request.secret = System.getProperty("secret")
        request.bucket = System.getProperty("bucket")
        request.fileName = "TencentCosFsTest.kt"
        request.endpoint = System.getProperty("endpoint")

        injector = Guice.createInjector(FsManager())
    }

    @Test
    fun writer()
    {
        val plugins: Set<Fs?>? = injector?.getInstance(Key.get(object : TypeLiteral<Set<Fs?>?>()
        {}))
        val plugin: Fs? = plugins?.first { v -> v?.name().equals(name) }

        val stream = FileInputStream("src/test/kotlin/org/devlive/metaforge/fs/s3/AmazonS3FsTest.kt")
        request.stream = stream
        val response = plugin !!.writer(request)
        assertTrue(response.isSuccessful)
    }

    @Test
    fun reader()
    {
        val plugins: Set<Fs?>? = injector?.getInstance(Key.get(object : TypeLiteral<Set<Fs?>?>()
        {}))
        val plugin: Fs? = plugins?.first { v -> v?.name().equals(name) }
        val response = plugin !!.reader(request)
        assertTrue(response.isSuccessful)

        try
        {
            BufferedReader(InputStreamReader(response.context, StandardCharsets.UTF_8)).use { reader ->
                var line: String?
                while ((reader.readLine().also { line = it }) != null)
                {
                    log.info(line)
                }
            }
        }
        catch (e: IOException)
        {
            log.error("Reader error", e)
        }
    }

    @Test
    fun delete()
    {
        val plugins: Set<Fs?>? = injector?.getInstance(Key.get(object : TypeLiteral<Set<Fs?>?>()
        {}))
        val plugin: Fs? = plugins?.first { v -> v?.name().equals(name) }
        val response = plugin !!.delete(request)
        assertTrue(response.isSuccessful)
    }
}
```
