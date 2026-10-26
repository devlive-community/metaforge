---
title: 自定义 File 转换器
---

MetaForge 支持自定义 File 转换器，使用者可以编写自己的文件转换器集成到 MetaForge 中。该文档主要讲解如何快速集成一个文件转换器到 MetaForge 系统中。

该模块我们主要使用到的是 `file` 模块内的代码，我们本文使用 `json` 来做示例。

### 模块基本配置

---

新建项目后在 `pom.xml` 文件中增加以下内容：

```xml
<dependencies>
    <dependency>
        <groupId>org.jetbrains.kotlin</groupId>
        <artifactId>kotlin-reflect</artifactId>
    </dependency>
    <dependency>
        <groupId>com.google.inject</groupId>
        <artifactId>guice</artifactId>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
    </dependency>
    <dependency>
        <groupId>org.devlive.metaforge</groupId>
        <artifactId>metaforge-file-spi</artifactId>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-core</artifactId>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.jetbrains.dokka</groupId>
            <artifactId>dokka-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

我们添加 `metaforge-file-spi` 依赖，这样我们就可以实现集成文件转换器。

### `Json Module` 加载器

---

```kotlin
package org.devlive.metaforge.convert.json

import com.google.inject.multibindings.Multibinder
import org.devlive.metaforge.convert.File
import org.devlive.metaforge.convert.FileModule

class JsonModule : FileModule()
{
    override fun configure()
    {
        Multibinder.newSetBinder(this.binder(), File::class.java)
            .addBinding()
            .to(JsonFile::class.java)
    }
}
```

### `Json File` 转换器

---

```kotlin
package org.devlive.metaforge.convert.json

import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerationException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.devlive.metaforge.common.utils.DateUtils
import org.devlive.metaforge.convert.File
import org.devlive.metaforge.convert.FileConvert.formatFile
import org.devlive.metaforge.convert.model.FileRequest
import org.devlive.metaforge.convert.model.FileResponse
import org.slf4j.LoggerFactory.getLogger
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Objects.requireNonNull

class JsonFile : File
{
    private val log = getLogger(this::class.java)

    override fun format(request: FileRequest): FileResponse
    {
        val response = FileResponse()
        try
        {
            log.info("${name()} format start time [ ${DateUtils.now()} ]")
            log.info("${name()} format headers start")
            response.headers = request.headers
            log.info("${name()} format headers end")

            log.info("${name()} format columns start")
            val mapper = ObjectMapper()
            val columns = mutableListOf<Any>()
            request.columns
                .forEach { column ->
                    val jsonNode = mapper.createObjectNode()
                    for (headerIndex in request.headers.indices)
                    {
                        val header = request.headers[headerIndex] as String
                        when (column)
                        {
                            is List<*> -> jsonNode.putPOJO(header, column[headerIndex])
                            else -> jsonNode.putPOJO(header, column)
                        }
                    }
                    columns.add(jsonNode)
                }
            response.columns = columns
            log.info("${name()} format columns end")

            log.info("${name()} format end time [ ${DateUtils.now()} ]")
            response.successful = true
        }
        catch (e: IOException)
        {
            response.successful = false
            response.message = e.message
        }
        return response
    }

    override fun formatStream(request: FileRequest): FileResponse
    {
        val response = FileResponse()
        try
        {
            requireNonNull("Stream must not be null")

            log.info("${name()} format stream start time [ ${DateUtils.now()} ]")
            val mapper = ObjectMapper()
            request.stream
                ?.let {
                    BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader ->
                        val jsonNode: JsonNode = mapper.readTree(reader)
                        log.info("${name()} format stream json node count [ ${jsonNode.size()} ]")

                        val headers = mutableListOf<Any>()
                        if (jsonNode.isArray && jsonNode.size() > 0)
                        {
                            jsonNode[0].fieldNames()
                                .forEachRemaining { headers.add(it) }
                        }
                        response.headers = headers

                        val columns = mutableListOf<Any>()
                        if (jsonNode.isArray)
                        {
                            jsonNode.elements()
                                .forEachRemaining { node ->
                                    val column = mutableMapOf<String, Any?>()
                                    node.fields()
                                        .forEachRemaining { field ->
                                            column[field.key] = field.value
                                        }
                                    columns.add(column)
                                }
                        }
                        response.columns = columns
                        it.close()
                    }
                }
            log.info("${name()} format stream end time [ ${DateUtils.now()} ]")
            response.successful = true
        }
        catch (e: IOException)
        {
            response.successful = false
            response.message = e.message
        }
        return response
    }

    override fun writer(request: FileRequest): FileResponse
    {
        val response = FileResponse()
        try
        {
            log.info("${name()} writer origin path [ ${request.path} ]")
            log.info("${name()} writer start time [ ${DateUtils.now()} ]")
            val file = formatFile(request, name())
            log.info("${name()} writer file absolute path [ ${file.absolutePath} ]")

            val factory = JsonFactory()
            factory.createGenerator(file, JsonEncoding.UTF8)
                .use { generator ->
                    generator.writeStartArray()
                    request.columns
                        .forEach { column ->
                            generator.writeStartObject()
                            for (headerIndex in request.headers.indices)
                            {
                                when (column)
                                {
                                    is List<*> -> generator.writeObjectField(request.headers[headerIndex] as String, column[headerIndex])
                                    is ObjectNode ->
                                    {
                                        generator.codec = ObjectMapper()
                                        val header = request.headers[headerIndex] as String
                                        generator.writeObjectField(header, column.get(header))
                                    }

                                    else -> generator.writeObjectField(request.headers[headerIndex] as String, column)
                                }
                            }
                            generator.writeEndObject()
                        }
                    generator.writeEndArray()
                }

            log.info("${name()} writer end time [ ${DateUtils.now()} ]")
            response.path = file.absolutePath
            response.successful = true
        }
        catch (e: IOException)
        {
            response.successful = false
            response.message = e.message
        }
        catch (e: JsonGenerationException)
        {
            response.successful = false
            response.message = e.message
        }
        return response
    }

    override fun reader(request: FileRequest): FileResponse
    {
        val response = FileResponse()
        try
        {
            log.info("${name()} reader origin path [ ${request.path} ]")
            log.info("${name()} reader start time [ ${DateUtils.now()} ]")
            val file = formatFile(request, name())
            log.info("${name()} reader file absolute path [ ${file.absolutePath} ]")

            val mapper = ObjectMapper()
            val jsonNode: JsonNode = mapper.readTree(file)
            log.info("${name()} reader file json node count [ ${jsonNode.size()} ]")

            log.info("${name()} reader file headers start")
            val headers = mutableListOf<Any>()
            if (jsonNode.isArray && jsonNode.size() > 0)
            {
                jsonNode[0].fieldNames()
                    .forEachRemaining { headers.add(it) }
            }
            response.headers = headers
            log.info("${name()} reader file headers end")

            log.info("${name()} reader file columns start")
            val columns = mutableListOf<Any>()
            if (jsonNode.isArray)
            {
                jsonNode.elements()
                    .forEachRemaining { node ->
                        val column = mutableMapOf<String, Any?>()
                        node.fields()
                            .forEachRemaining { field ->
                                column[field.key] = field.value
                            }
                        columns.add(column)
                    }
            }
            response.columns = columns
            log.info("${name()} reader file columns end")
            response.successful = true
        }
        catch (e: Exception)
        {
            response.successful = false
            response.message = e.message
        }
        return response
    }
}
```

### `File SPI` 加载器

---

在 `resources` 源目录下添加 `META-INF` 和 `services` 目录，格式为 `resources/META-INF/services`，创建 `org.devlive.metaforge.convert.FileModule` 文件，内容如下

```kotlin
org.devlive.metaforge.convert.json.JsonModule
```

> 通过以上内容我们实现了 Json 文件转换器的支持。我们只需要在要使用 Json 文件转换器的地方引用该模块即可。比如我们在 `server` 模块中使用到该模块，则在 `server/pom.xml` 文件中增加以下内容

```xml
<dependency>
    <groupId>org.devlive.metaforge</groupId>
    <artifactId>metaforge-file-json</artifactId>
    <version>${project.version}</version>
</dependency>
```

### `Json Module` 测试

---

```kotlin
package org.devlive.metaforge.convert.json

import com.google.inject.Guice.createInjector
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import org.devlive.metaforge.convert.File
import org.devlive.metaforge.convert.FileManager
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonModuleTest
{
    private val injector: Injector = createInjector(FileManager())

    @Test
    fun test()
    {
        injector.getInstance(Key.get(object : TypeLiteral<Set<File>>()
        {}))
            .stream()
            .findFirst()
            .ifPresent {
                assertEquals("Json", it.name())
            }
    }
}
```

### `Json SPI` 测试

---

```kotlin
package org.devlive.metaforge.convert.json

import com.google.inject.Guice.createInjector
import com.google.inject.Injector
import org.devlive.metaforge.convert.FileFilter
import org.devlive.metaforge.convert.FileManager
import org.devlive.metaforge.convert.model.FileRequest
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertTrue

class JsonFileTest
{
    private val log = getLogger(this::class.java)
    private val name = "Json"
    private var injector: Injector? = null
    private val request: FileRequest = FileRequest()

    @Before
    fun before()
    {
        injector = createInjector(FileManager())

        request.name = "test"
        request.path = System.getProperty("user.dir")
        request.headers = listOf("name", "age")

        val l1 = listOf("Test", 12)
        val l2 = listOf("Test1", 121)
        request.columns = listOf(l1, l2)
    }

    @Test
    fun testFormat()
    {
        injector?.let { injector ->
            FileFilter.filter(injector, name)
                .ifPresent { file ->
                    val response = file.format(request)
                    log.info("headers: [ ${response.headers} ]")
                    response.columns
                        .let { columns ->
                            columns.forEachIndexed { index, line ->
                                log.info("index: [ $index ], line: [ $line ]")
                            }
                        }

                    assertTrue {
                        response.successful == true
                    }
                }
        }
    }

    @Test
    fun testFormatStream()
    {
        injector?.let { injector ->
            FileFilter.filter(injector, name)
                .ifPresent { file ->
                    request.stream = FileInputStream(File("${System.getProperty("user.dir")}/${request.name}.json"))
                    val response = file.formatStream(request)
                    log.info("headers: [ ${response.headers} ]")
                    response.columns
                        .let { columns ->
                            columns.forEachIndexed { index, line ->
                                log.info("index: [ $index ], line: [ $line ]")
                            }
                        }
                    assertTrue {
                        response.successful == true
                    }
                }
        }
    }

    @Test
    fun testWriter()
    {
        injector?.let { injector ->
            FileFilter.filter(injector, name)
                .ifPresent { file ->
                    assertTrue {
                        file.writer(request)
                            .successful == true
                    }
                }
        }
    }

    @Test
    fun testReader()
    {
        injector?.let { injector ->
            FileFilter.filter(injector, name)
                .ifPresent { file ->
                    val response = file.reader(request)
                    log.info("headers: ${response.headers}")
                    response.columns
                        .forEach {
                            log.info("columns: $it")
                        }
                    assertTrue {
                        response.successful == true
                    }
                }
        }
    }
}
```
