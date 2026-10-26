---
title: SDK 集成
---

本文章主要用来介绍如何集成 metaforge 提供的 plugin 插件快速访问对应数据源。在本文中我们使用 `metaforge-jdbc-mysql` SDK 来访问 MySQL 数据源。

### 准备工作

---

引入插件依赖

- Apache Maven

```xml
<dependency>
    <groupId>org.devlive.metaforge</groupId>
    <artifactId>metaforge-jdbc-mysql</artifactId>
    <version>VERSION</version>
</dependency>
```

- Gradle

```xml
implementation group: 'org.devlive.metaforge', name: 'metaforge-jdbc-mysql', version: 'VERSION'
```

!!! danger

    请将 `VERSION` 替换为对应的版本号
!!!

### 使用插件

---

```java
String sql = "SHOW TABLES";

// 构建连接配置
Configure configure = new Configure();
configure.setHost("localhost");
configure.setPort(3306);
configure.setUsername(Optional.of("root"));
configure.setPassword(Optional.of("12345678"));

// 初始化 MySQL 插件
MySQLPlugin plugin = new MySQLPlugin();
plugin.connect(configure);

// 获取执行结果
Response response = plugin.execute(sql);
response.columns.forEach(item -> {
    log.info(item);
});
```

!!! note

    请注意 `metaforge-jdbc-mysql` 插件仅支持 MySQL 数据源，如果需要其他 SDK 请使用其他的插件。
!!!
