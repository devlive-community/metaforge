---
title: 自定义 Notify 通知器
---

MetaForge 支持自定义通知器，使用者可以编写自己的通知器集成到 MetaForge 中。该文档主要讲解如何快速集成一个通知器到 MetaForge 系统中。

该模块我们主要使用到的是 `notify` 模块内的代码，我们本文使用钉钉通知器来做示例。

### 项目配置

---

新建项目后在 `pom.xml` 文件中增加以下内容：

```xml
<dependencies>
    <dependency>
        <groupId>org.devlive.metaforge</groupId>
        <artifactId>metaforge-notify-spi</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```
我们添加 `metaforge-notify-spi` 依赖，这样我们就可以实现集成通知器。

### 模块加载器

---

```kotlin
package org.devlive.metaforge.notify.dingtalk

import com.google.inject.multibindings.Multibinder
import org.devlive.metaforge.notify.Notify
import org.devlive.metaforge.notify.NotifyModule

class DingTalkModule : NotifyModule()
{
    override fun configure()
    {
        Multibinder.newSetBinder(this.binder(), Notify::class.java)
            .addBinding()
            .to(DingTalkNotify::class.java)
    }
}
```

### 通知器

---

```kotlin
package org.devlive.metaforge.notify.dingtalk

import org.devlive.metaforge.notify.Notify
import org.devlive.metaforge.notify.model.NotifyRequest
import org.devlive.metaforge.notify.model.NotifyResponse

class DingTalkNotify : Notify
{
    override fun send(request: NotifyRequest): NotifyResponse
    {
        return DingTalkUtils.send(request)
    }
}
```

在转换器中我们只需要实现以下两个方法：

- `fun send(request: NotifyRequest): NotifyResponse` 用于实现发送器执行逻辑

### `DingTalkUtils` 工具类

---

```kotlin
package org.devlive.metaforge.notify.dingtalk

import org.devlive.metaforge.common.utils.JsonUtils
import org.devlive.metaforge.common.utils.SignUtils
import org.devlive.metaforge.lib.http.HttpClient
import org.devlive.metaforge.lib.http.HttpConfigure
import org.devlive.metaforge.lib.http.HttpMethod
import org.devlive.metaforge.notify.NotifyType
import org.devlive.metaforge.notify.dingtalk.model.ReturnModel
import org.devlive.metaforge.notify.dingtalk.model.TextModel
import org.devlive.metaforge.notify.model.NotifyRequest
import org.devlive.metaforge.notify.model.NotifyResponse
import org.apache.commons.lang3.StringUtils.isNotEmpty
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger

object DingTalkUtils
{
    private val log: Logger = getLogger(DingTalkUtils::class.java)

    @JvmStatic
    fun send(request: NotifyRequest): NotifyResponse
    {
        val configure = HttpConfigure()
        configure.autoConnected = false
        configure.retry = 0
        configure.protocol = "https"
        configure.host = "oapi.dingtalk.com"
        configure.port = 443
        configure.path = "robot/send"
        configure.method = HttpMethod.POST

        val params = mutableMapOf("access_token" to request.access)
        if (isNotEmpty(request.secret))
        {
            val signResponse = SignUtils.sign(request.secret)
            log.info("Sign response: ${JsonUtils.toJSON(signResponse)}")
            params["sign"] = signResponse.sign
            params["timestamp"] = signResponse.timestamp.toString()
        }
        configure.params = params
        log.info("Notify request params: ${JsonUtils.toJSON(params)}")

        val text = TextModel()
        text.content = request.content
        configure.body = JsonUtils.toJSON(mapOf("text" to text, "msgtype" to formatMessageType(request)))
        log.info("Notify request body: ${configure.body}")

        val client = HttpClient(configure)
        val returnModel = JsonUtils.toObject(client.execute(), ReturnModel::class.java)
        val response = NotifyResponse()
        if (returnModel.code == 0)
        {
            response.successful = true
            response.message = null
        }
        else
        {
            response.successful = false
            response.message = returnModel.message
        }
        return response
    }

    private fun formatMessageType(request: NotifyRequest): String
    {
        if (request.type == NotifyType.TEXT)
        {
            return "text"
        }
        else
        {
            return "markdown"
        }
    }
}
```

在工具类中我们主要实现发送钉钉消息操作。

### `Notify SPI` 加载器

---

在 `resources` 源目录下添加 `META-INF` 和 `services` 目录，格式为 `resources/META-INF/services`，创建 `org.devlive.metaforge.fs.FsModule` 文件，内容如下

```kotlin
org.devlive.metaforge.notify.dingtalk.DingTalkModule
```

> 通过以上内容我们实现了 DingTalk 通知器的支持。我们只需要在要使用 DingTalk 通知器器的地方引用该模块即可。比如我们在 server 模块中使用到该模块，则在 server/pom.xml 文件中增加以下内容

```xml
<dependency>
    <groupId>org.devlive.metaforge</groupId>
    <artifactId>metaforge-notify-dingtalk</artifactId>
    <version>${project.version}</version>
</dependency>
```

### `DingTalk Utils` 测试

```kotlin
package org.devlive.metaforge.notify.dingtalk

import org.devlive.metaforge.notify.model.NotifyRequest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DingTalkUtilsTest
{
    private val request: NotifyRequest = NotifyRequest()

    @Before
    fun before()
    {
        request.access = "ACCESS"
        request.content = "Test Message"
        request.secret = "SECRET"
    }

    @Test
    fun testSend()
    {
        Assert.assertFalse(
            DingTalkUtils.send(request)
                .successful
        )
    }
}
```

### `DingTalk Module` 测试

```kotlin
package org.devlive.metaforge.notify.dingtalk

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import org.devlive.metaforge.notify.Notify
import org.devlive.metaforge.notify.NotifyManager
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class DingTalkModuleTest
{
    private val name = "DingTalk"
    private var injector: Injector? = null

    @Before
    fun before()
    {
        injector = Guice.createInjector(NotifyManager())
    }

    @Test
    fun test()
    {
        val notify: Notify? = injector?.getInstance(Key.get(object : TypeLiteral<Set<Notify>>()
        {}))
            ?.first { it.name() == name }
        assertNotNull(notify)
    }
}
```

### `DingTalk SPI` 测试

```kotlin
package org.devlive.metaforge.notify.dingtalk

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import org.devlive.metaforge.notify.Notify
import org.devlive.metaforge.notify.NotifyManager
import org.devlive.metaforge.notify.model.NotifyRequest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class DingTalkNotifyTest
{
    private val name = "DingTalk"
    private var injector: Injector? = null
    private val request: NotifyRequest = NotifyRequest()

    @Before
    fun before()
    {
        injector = Guice.createInjector(NotifyManager())

        request.access = "ACCESS"
        request.content = "Test Message"
        request.secret = "SECRET"
    }

    @Test
    fun test()
    {
        val notify: Notify? = injector?.getInstance(Key.get(object : TypeLiteral<Set<Notify>>()
        {}))
            ?.first { it.name() == name }
        assertNotNull(notify?.send(request))
    }
}
```
