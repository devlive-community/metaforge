---
title: 七牛云存储
---

在 MetaForge 中目前已经支持七牛云，支持功能如下：

- 上传文件
- 预览&下载文件
- 删除文件

### MetaForge 使用配置

---

在 MetaForge 中文件系统用于配置系统的缓冲以及临时文件的存储。修改 `application.properties` 文件如下内容：

```properties
metaforge.fs.type=Qiniu
metaforge.fs.access=
metaforge.fs.secret=
metaforge.fs.endpoint=
metaforge.fs.bucket=
```

- `metaforge.fs.type`: 文件系统的类型只能是 **Qiniu**
- `metaforge.fs.access`: 七牛云的 AccessKey
- `metaforge.fs.secret`: 七牛云的 SecretKey
- `metaforge.fs.endpoint`: 七牛云的 Endpoint
- `metaforge.fs.bucket`: 七牛云的存储桶名称，如 `metaforge`

### 依赖使用方式

---

该插件支持第三方依赖方式引入，引入依赖如下：

```xml
<dependency>
    <groupId>org.devlive.metaforge</groupId>
    <artifactId>metaforge-fs-qiniu</artifactId>
    <version>${VERSION}</version>
</dependency>
```

使用代码如下：

```kotlin
package org.devlive.metaforge.fs.qiniu

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import org.devlive.metaforge.fs.FsService
import org.devlive.metaforge.fs.FsManager
import org.devlive.metaforge.fs.FsRequest
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlin.test.assertTrue

class QiniuFsTest {
    private val log = LoggerFactory.getLogger(QiniuFsTest::class.java)
    private val name = "Qiniu"
    private var request = FsRequest()
    private var injector: Injector? = null

    @Before
    fun before() {
        request.access = System.getProperty("access")
        request.secret = System.getProperty("secret")
        request.bucket = System.getProperty("bucket")
        request.fileName = "IOUtilsTest.kt"
        request.endpoint = System.getProperty("endpoint")

        injector = Guice.createInjector(FsManager())
    }

    @Test
    fun writer() {
        val plugins: Set<Fs?>? = injector?.getInstance(Key.get(object : TypeLiteral<Set<Fs?>?>() {}))
        val plugin: Fs? = plugins?.first { v -> v?.name().equals(name) }

        val stream = FileInputStream("src/test/kotlin/org/devlive/metaforge/fs/qiniu/IOUtilsTest.kt")
        request.stream = stream
        val response = plugin !!.writer(request)
        assertTrue(response.isSuccessful)
    }

    @Test
    fun reader() {
        val plugins: Set<Fs?>? = injector?.getInstance(Key.get(object : TypeLiteral<Set<Fs?>?>() {}))
        val plugin: Fs? = plugins?.first { v -> v?.name().equals(name) }
        val response = plugin !!.reader(request)
        assertTrue(response.isSuccessful)

        try {
            BufferedReader(InputStreamReader(response.context, StandardCharsets.UTF_8)).use { reader ->
                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    log.info(line)
                }
            }
        }
        catch (e: IOException) {
            log.error("Reader error", e)
        }
    }

    @Test
    fun testDelete() {
        val plugins: Set<Fs?>? = injector?.getInstance(Key.get(object : TypeLiteral<Set<Fs?>?>() {}))
        val plugin: Fs? = plugins?.first { v -> v?.name().equals(name) }
        val response = plugin !!.delete(request)
        assertTrue(response.isSuccessful)
    }
}
```
