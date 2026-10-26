---
title: 自定义 Fs 文件系统
---

MetaForge 支持自定义文件系统，使用者可以编写自己的文件存储系统集成到 MetaForge 中。该文档主要讲解如何快速集成一个文件存储系统到 MetaForge 系统中。

该模块我们主要使用到的是 `fs` 模块内的代码，我们本文使用本地存储来做示例。

### 项目配置

---

新建项目后在 `pom.xml` 文件中增加以下内容：

```xml
<dependencies>
    <dependency>
        <groupId>org.devlive.metaforge</groupId>
        <artifactId>metaforge-fs-spi</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```
我们添加 `metaforge-fs-spi` 依赖，这样我们就可以实现集成文件系统。

### 模块加载器

---

```kotlin
package org.devlive.metaforge.fs

import com.google.inject.multibindings.Multibinder

class LocalModule : FsModule() {
    override fun configure() {
        Multibinder.newSetBinder(binder(), Fs::class.java)
            .addBinding()
            .to(LocalFs::class.java)
    }
}
```

### 系统转换器

---

```kotlin
package org.devlive.metaforge.fs

import org.slf4j.LoggerFactory
import java.io.File

class LocalFs : Fs {
    private val log = LoggerFactory.getLogger(this.javaClass)

    override fun writer(request: FsRequest): FsResponse {
        log.info("LocalFs writer origin path [ {} ]", request.localPath)
        val targetPath = listOf(request.endpoint, request.bucket, request.fileName).joinToString(File.separator)
        val response = FsResponse.builder()
            .origin(request.localPath)
            .remote(targetPath)
            .successful(true)
            .build()
        log.info("LocalFs writer target path [ {} ]", targetPath)
        try {
            if (request.localPath == null || request.localPath.isEmpty()) {
                IOUtils.copy(request.stream, targetPath, true)
            } else {
                IOUtils.copy(request.localPath, targetPath, true)
            }
            log.info("LocalFs writer [ {} ] successfully", targetPath)
        } catch (e: Exception) {
            log.error("LocalFs writer error", e)
            response.successful = false
            response.message = e.message
        }
        return response
    }

    override fun reader(request: FsRequest): FsResponse {
        val targetPath = listOf(request.endpoint, request.bucket, request.fileName).joinToString(File.separator)
        log.info("LocalFs reader origin path [ {} ]", targetPath)
        val response = FsResponse.builder()
            .remote(targetPath)
            .successful(true)
            .build()
        try {
            response.context = IOUtils.reader(targetPath)
            log.info("LocalFs reader [ {} ] successfully", targetPath)
        } catch (e: Exception) {
            log.error("LocalFs reader error", e)
            response.successful = false
            response.message = e.message
        }
        return response
    }
}
```

在转换器中我们只需要实现以下两个方法：

- `public FsResponse writer(FsRequest request)` 用于写数据到文件系统
- `public FsResponse reader(FsRequest request)` 用于读取文件系统中的数据

### `IOUtils` 转换器

---

```kotlin
package org.devlive.metaforge.fs

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

object IOUtils {
    /**
     * Copies a file from the source path to the target path.
     *
     * @param source the path of the file to be copied
     * @param target the path where the file should be copied to
     * @param createdDir a flag indicating whether the parent directories of the target path should be created if they do not exist
     * @return true if the file was successfully copied, false otherwise
     */
    fun copy(source: String, target: String, createdDir: Boolean): Boolean {
        try {
            val targetPath: Path = Paths.get(target)
            if (createdDir) {
                Files.createDirectories(targetPath.parent)
            }

            Files.copy(Paths.get(source), targetPath, StandardCopyOption.REPLACE_EXISTING)
            return true
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    /**
     * Copies the contents of the input stream to the specified target file path.
     *
     * @param stream the input stream containing the contents to be copied
     * @param target the path of the target file where the contents will be copied to
     * @param createdDir indicates whether the parent directory of the target file has been created
     * @return true if the contents were successfully copied, false otherwise
     */
    fun copy(stream: InputStream, target: String, createdDir: Boolean): Boolean {
        try {
            val targetPath: Path = Paths.get(target)
            if (createdDir) {
                Files.createDirectories(targetPath.parent)
            }

            Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING)
            return true
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    /**
     * This function takes a source file path and returns an InputStream object that can be used to read the contents of the file.
     *
     * @param source the path of the source file to read
     * @return an InputStream object representing the source file
     */
    fun reader(source: String): InputStream {
        try {
            return Files.newInputStream(Paths.get(source), StandardOpenOption.READ)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
```

在转换器中我们主要实现本地数据的读写操作。

### `Fs SPI` 加载器

---

在 `resources` 源目录下添加 `META-INF` 和 `services` 目录，格式为 `resources/META-INF/services`，创建 `org.devlive.metaforge.fs.FsModule` 文件，内容如下

```kotlin
org.devlive.metaforge.fs.LocalModule
```

!!! note

    通过以上内容我们实现了本地数据存储的模块支持。我们只需要在要使用存储的地方引用该模块即可。
!!!

!!! warning

    我们建议添加对模块的测试用例，可参考已经提交的代码进行模拟测试。
!!!