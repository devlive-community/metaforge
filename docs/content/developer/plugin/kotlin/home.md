---
title: 自定义 Plugin 连接器
---

MetaForge 支持自定义插件，使用者可以编写自己的插件集成到系统中。该文档主要讲解如何快速集成一个插件到 MetaForge 系统中。

!!! note

    本文使用集成基于 JDBC 协议的 StarRocks 数据存储系统来演示。
!!!

### `pom.xml` 依赖

```xml
<dependency>
    <groupId>org.devlive.metaforge</groupId>
    <artifactId>metaforge-spi</artifactId>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.devlive.metaforge</groupId>
    <artifactId>metaforge-common</artifactId>
</dependency>
<dependency>
    <groupId>commons-beanutils</groupId>
    <artifactId>commons-beanutils</artifactId>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

以上配置添加了 `metaforge-spi` 和 `metaforge-common` 模块，其他的是一些辅助依赖。

!!! warning

    需要注意的是如果您是单独开启的项目需要指定各个依赖的版本号。
!!!

### 插件加载器

```kotlin
class StarRocksPluginModule : AbstractPluginModule(), PluginModule {
    override fun getName(): String {
        return "StarRocks"
    }

    override fun getType(): PluginType {
        return PluginType.JDBC
    }

    override fun get(): AbstractPluginModule {
        return this
    }

    override fun configure() {
        val module = Multibinder.newSetBinder(binder(), String::class.java)
        module.addBinding().toInstance(this.javaClass.simpleName)
        val plugin: Multibinder<Plugin> = Multibinder.newSetBinder(binder(), Plugin::class.java)
        plugin.addBinding().to(StarRocksPlugin::class.java)
    }
}
```

加载器需要继承 `AbstractPluginModule` 类，并实现 `PluginModule` 接口，这样系统会在启动时自动将插件加载到系统中。

!!! note

    需要注意的是，需要覆盖父类中的 `configure()` 方法，并将插件绑定到系统中。
!!!

### 插件执行器

```kotlin
class StarRocksPlugin : Plugin {
    private val log = getLogger(StarRocksPlugin::class.java)

    private var jdbcConfigure: JdbcConfigure? = null
    private var jdbcConnection: JdbcConnection? = null
    private var jdbcResponse: Response? = null

    override fun name(): String {
        return "StarRocks"
    }

    override fun description(): String {
        return "Integrate StarRocks data sources"
    }

    override fun type(): PluginType {
        return PluginType.JDBC
    }

    override fun connect(configure: Configure?) {
        try {
            log.info("Connecting to StarRocks")
            jdbcResponse = Response()
            jdbcConfigure = JdbcConfigure()
            BeanUtils.copyProperties(jdbcConfigure, configure)
            jdbcConfigure!!.jdbcDriver = "com.mysql.cj.jdbc.Driver"
            jdbcConfigure!!.jdbcType = "mysql"
            jdbcConnection = object : JdbcConnection(jdbcConfigure, jdbcResponse) {}
        } catch (ex: Exception) {
            jdbcResponse!!.isConnected = false
            jdbcResponse!!.message = ex.message
        }
    }

    override fun execute(content: String?): Response {
        if (ObjectUtils.isNotEmpty(jdbcConnection)) {
            log.info("Execute starrocks plugin logic started")
            jdbcResponse = jdbcConnection?.response
            val processor = JdbcAdapter(jdbcConnection)
            jdbcResponse = processor.handlerExecute(content)
            log.info("Execute starrocks plugin logic end")
        }
        destroy()
        return jdbcResponse!!
    }

    override fun destroy() {
        if (ObjectUtils.isNotEmpty(jdbcConnection)) {
            jdbcConnection?.destroy()
            jdbcConnection = null
        }
    }
}
```

执行器需要实现 `Plugin` 接口，该接口中提供了以下方法

- `name()`: 插件有唯一名称，同名插件只会在第一次加载时生效
- `description()`: 对于该插件的描述
- `type()`: 插件类型
- `connect(Configure configure)`: 插件需要提前连接信息，比如当前插件插件，就是插件的连接阶段（系统预设 HTTP 连接方式直接使用）。
- `execute(String content)`: 具体执行操作逻辑
- `destroy()`:  插件最后的销毁，注意销毁需要包含连接中的信息

### 插件转换器

插件转换器用于对当前插件执行后的结果的转化，将其转换为 MetaForge 中可以使用的逻辑。主要是用于封装 `Response` 返回结果。

本文是基于 JDBC 的插件所以直接继承 `JdbcAdapter` 父类即可实现部分功能。

### SPI 加载器

在 `resources` 源目录下添加 `META-INF` 和 `services` 目录

!!! warning

    `services` 在 `resources` 目录中需要
!!!

创建 `org.devlive.metaforge.spi.PluginModule` 文件，内容如下

```java
org.devlive.metaforge.plugin.jdbc.starrocks.StarRocksPluginModule
```

该文件的内容是我们定义好的插件加载模块。

!!! warning

    插件的单元测试可以参考已经发布的插件进行测试
!!!
