---
title: Java 实现
---

MetaForge 支持自定义插件，使用者可以编写自己的插件集成到系统中。该文档主要讲解如何快速集成一个插件到 MetaForge 系统中。

!!! note

    本文使用集成基于 HTTP 协议的 QuestDB 数据存储系统来演示。
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

```java
public class QuestDBPluginModule
        extends AbstractPluginModule
        implements PluginModule
{
    @Override
    public String getName()
    {
        return "QuestDB";
    }

    @Override
    public PluginType getType()
    {
        return PluginType.HTTP;
    }

    @Override
    public AbstractPluginModule get()
    {
        return this;
    }

    protected void configure()
    {
        Multibinder<Plugin> plugin = Multibinder.newSetBinder(this.binder(), Plugin.class);
        plugin.addBinding().to(QuestDBPlugin.class);
    }
}
```

加载器需要继承 `AbstractPluginModule` 类，并实现 `PluginModule` 接口，这样系统会在启动时自动将插件加载到系统中。

!!! note

    需要注意的是，需要覆盖父类中的 `configure()` 方法，并将插件绑定到系统中。
!!!

### 插件执行器

```java
@Slf4j
public class QuestDBPlugin
        implements Plugin
{
    private HttpConfigure configure;
    private HttpConnection connection;
    private Response response;

    @Override
    public String name()
    {
        return "QuestDB";
    }

    @Override
    public String description()
    {
        return "Integrate QuestDB data sources";
    }

    @Override
    public PluginType type()
    {
        return PluginType.HTTP;
    }

    @Override
    public void connect(Configure configure)
    {
        try {
            this.response = new Response();
            this.configure = new HttpConfigure();
            BeanUtils.copyProperties(this.configure, configure);
            this.connection = new HttpConnection(this.configure, this.response);
        }
        catch (Exception ex) {
            this.response.setIsConnected(Boolean.FALSE);
            this.response.setMessage(ex.getMessage());
        }
    }

    @Override
    public Response execute(String content)
    {
        if (ObjectUtils.isNotEmpty(this.connection)) {
            log.info("Execute questdb plugin logic started");
            this.response = this.connection.getResponse();
            QuestDBAdapter processor = new QuestDBAdapter(this.connection);
            this.response = processor.handlerExecute(content);
            log.info("Execute questdb plugin logic end");
        }
        this.destroy();
        return this.response;
    }

    @Override
    public void destroy()
    {
        if (ObjectUtils.isNotEmpty(this.connection)) {
            this.connection.destroy();
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

```java
@Slf4j
public class QuestDBAdapter
        extends HttpAdapter
{
    public QuestDBAdapter(HttpConnection connection)
    {
        super(connection);
    }

    @Override
    public Response handlerExecute(String content)
    {
        Time processorTime = new Time();
        processorTime.setStart(new Date().getTime());
        Response response = this.httpConnection.getResponse();
        HttpConfigure configure = new HttpConfigure();
        if (response.getIsConnected()) {
            List<String> headers = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<Object> columns = new ArrayList<>();
            try {
                BeanUtils.copyProperties(configure, this.httpConnection.getConfigure());
                configure.setAutoConnected(Boolean.FALSE);
                configure.setRetry(0);
                configure.setMethod(HttpMethod.GET);
                configure.setPath("exec");
                Map<String, String> parameters = Maps.newHashMap();
                parameters.put("query", content);
                configure.setParams(parameters);
                configure.setDecoded(true);
                HttpConnection httpConnection = new HttpConnection(configure, new Response());
                HttpClient httpClient = HttpClient.getInstance(configure, httpConnection);
                String body = httpClient.execute();
                QuestDBResponse requestResponse = JSON.objectmapper.readValue(body, QuestDBResponse.class);
                if (ObjectUtils.isNotEmpty(requestResponse.getQuery())) {
                    response.setIsSuccessful(true);
                    if (ObjectUtils.isNotEmpty(requestResponse.getColumns())) {
                        requestResponse.getColumns()
                                .forEach(schema -> {
                                    headers.add(schema.getName());
                                    types.add(schema.getType());
                                });
                    }
                    requestResponse.getDataset()
                            .forEach(record -> columns.add(handlerFormatter(configure.getFormat(), headers, record)));
                }
                else {
                    response.setIsSuccessful(Boolean.FALSE);
                    response.setMessage(requestResponse.getError());
                }
            }
            catch (Exception ex) {
                log.error("Execute content failed content {} exception ", content, ex);
                response.setIsSuccessful(Boolean.FALSE);
                response.setMessage(ex.getMessage());
            }
            finally {
                response.setHeaders(headers);
                response.setTypes(types);
                response.setColumns(columns);
            }
        }
        processorTime.setEnd(new Date().getTime());
        response.setProcessor(processorTime);
        return response;
    }
}
```

插件转换器用于对当前插件执行后的结果的转化，将其转换为 MetaForge 中可以使用的逻辑。主要是用于封装 `Response` 返回结果。

本文是基于 JDBC 的插件所以直接继承 `HttpAdapter` 父类即可实现部分功能。

### SPI 加载器

在 `resources` 源目录下添加 `META-INF` 和 `services` 目录

!!! warning

    `services` 在 `resources` 目录中需要
!!!

创建 `org.devlive.metaforge.spi.PluginModule` 文件，内容如下

```java
org.devlive.metaforge.plugin.http.questdb.QuestDBPluginModule
```

该文件的内容是我们定义好的插件加载模块。

!!! warning

    插件的单元测试可以参考已经发布的插件进行测试
!!!
