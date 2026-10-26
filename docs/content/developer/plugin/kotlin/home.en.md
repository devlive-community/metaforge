---
title: Kotlin Implementation
---

MetaForge supports custom plug-ins, and users can write their own plug-ins and integrate them into the system. This document mainly explains how to quickly integrate a plug-in into the MetaForge system.
!!! note

    This article demonstrates by integrating the StarRocks data storage system based on the JDBC protocol.
!!!

### `pom.xml` Depend

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

The above configuration adds `metaforge-spi` and `metaforge-common` modules, and others are some auxiliary dependencies.

!!! warning

    It should be noted that if you open the project separately, you need to specify the version number of each dependency.
!!!

### Plugin Loader

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

The loader needs to inherit the `AbstractPluginModule` class and implement the `PluginModule` interface, so that the system will automatically load the plug-in into the system when it starts.

!!! note

    It should be noted that you need to override the `configure()` method in the parent class and bind the plugin to the system.
!!!

### Plugin Executor

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

The executor needs to implement the `Plugin` interface, which provides the following methods

- `name()`: Plugins have unique names, and plugins with the same name will only take effect the first time they are loaded
- `description()`: A description of the plugin
- `type()`: Plugin type
- `connect(Configure configure)`: Plug-ins need connection information in advance, such as the current plug-in plug-in, which is the connection phase of the plug-in (the system presets the HTTP connection method to be used directly).
- `execute(String content)`: Execute the operation logic
- `destroy()`:  The final destruction of the plug-in, note that the destruction needs to contain the information in the connection

### Plugin Converter

The plug-in converter is used to convert the result after the current plug-in is executed, and convert it into logic that can be used in MetaForge. It is mainly used to encapsulate `Response` to return the result.

This article is a JDBC-based plug-in, so some functions can be realized by directly inheriting the `JdbcAdapter` parent class.

### SPI loader

Add `META-INF` and `services` directory under `resources` source directory

!!! warning

    `services` needs to be in the `resources` directory
!!!

Create the `org.devlive.metaforge.spi.PluginModule` file, as follows

```java
org.devlive.metaforge.plugin.jdbc.starrocks.StarRocksPluginModule
```

The content of this file is the plugin loading module we defined.

!!! warning

    The unit test of the plug-in can refer to the published plug-in for testing
!!!
