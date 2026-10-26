---
title: Java Implementation
---

MetaForge supports custom plug-ins, and users can write their own plug-ins and integrate them into the system. This document mainly explains how to quickly integrate a plug-in into the MetaForge system.
!!! note

    This article demonstrates by integrating the QuestDB data storage system based on the HTTP protocol.
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

The loader needs to inherit the `AbstractPluginModule` class and implement the `PluginModule` interface, so that the system will automatically load the plug-in into the system when it starts.

!!! note

    It should be noted that you need to override the `configure()` method in the parent class and bind the plugin to the system.
!!!

### Plugin Executor

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

The executor needs to implement the `Plugin` interface, which provides the following methods

- `name()`: Plugins have unique names, and plugins with the same name will only take effect the first time they are loaded
- `description()`: A description of the plugin
- `type()`: Plugin type
- `connect(Configure configure)`: Plug-ins need connection information in advance, such as the current plug-in plug-in, which is the connection phase of the plug-in (the system presets the HTTP connection method to be used directly).
- `execute(String content)`: Execute the operation logic
- `destroy()`:  The final destruction of the plug-in, note that the destruction needs to contain the information in the connection

### Plugin Converter

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

The plug-in converter is used to convert the result after the current plug-in is executed, and convert it into logic that can be used in MetaForge. It is mainly used to encapsulate `Response` to return the result.

This article is a JDBC-based plug-in, so some functions can be realized by directly inheriting the `HttpAdapter` parent class.

### SPI loader

Add `META-INF` and `services` directory under `resources` source directory

!!! warning

    `services` needs to be in the `resources` directory
!!!

Create the `org.devlive.metaforge.spi.PluginModule` file, as follows

```java
org.devlive.metaforge.plugin.http.questdb.QuestDBPluginModule
```

The content of this file is the plugin loading module we defined.

!!! warning

    The unit test of the plug-in can refer to the published plug-in for testing
!!!
