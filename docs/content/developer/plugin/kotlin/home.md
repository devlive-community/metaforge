---
title: Kotlin 实现
---

插件也可以用 Kotlin 编写（例如 `plugin/metaforge-plugin-mongo-community`、`plugin/metaforge-plugin-doris`）。规则、目录结构、SPI 注册、管理能力声明、e2e 测试和文档要求与 [Java 实现](../java/home.md) 完全相同，本文只说明 Kotlin 的写法。

!!! warning

    所有插件只通过 JDBC 访问数据源，并使用同一套元数据管理（顶部选库、表树、列结构）。详见 [Java 实现](../java/home.md)。
!!!

### `pom.xml` 依赖

在 Java 插件的依赖之外加入 Kotlin 反射库，源码放在 `src/main/kotlin`（Kotlin 编译插件已在父 `pom.xml` 中配置）：

```xml
<dependency>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-reflect</artifactId>
</dependency>
```

### 插件类

```kotlin
class MongoCommunityPlugin : Plugin()
{
    override fun describe(): PluginConfigure
    {
        val configure = PluginConfigure.of("MongoCommunity", listOf(
            PluginConfigureField.builder().field(PluginConfigureFieldName.name).type(PluginConfigureFieldType.String).required(true).build(),
            PluginConfigureField.builder().field(PluginConfigureFieldName.host).type(PluginConfigureFieldType.String).required(true).value("127.0.0.1").build(),
            PluginConfigureField.builder().field(PluginConfigureFieldName.port).type(PluginConfigureFieldType.Number).required(true).value(27017).build(),
            PluginConfigureField.builder().field(PluginConfigureFieldName.database).type(PluginConfigureFieldType.String).value("admin")
                .group(PluginConfigureFieldGroup.advanced).build()), null)
        configure.category = PluginConfigureCategory.NOSQL
        return configure
    }
}
```

### 服务类

```kotlin
class MongoCommunityService : PluginService
{
    override fun connectType(): String = "mongodb"

    override fun driver(): String = "org.devlive.metaforge.MongoDriver"

    // 库、集合、字段来自驱动的 JDBC 元数据
    override fun getDatabases(configure: RuntimeConfigure): Response = metadataDatabases(configure)

    override fun getTables(configure: RuntimeConfigure, database: String): Response = metadataTables(configure, database)

    override fun getColumns(configure: RuntimeConfigure, database: String, table: String): Response = metadataColumns(configure, database, table)
}
```

用 SQL 提供元数据时，结果列名与 [Java 实现](../java/home.md) 中的表格一致。

### SPI 注册、能力声明与测试

- `src/main/resources/META-INF/services/org.devlive.metaforge.plugin.Plugin` 与 `org.devlive.metaforge.plugin.Service` 分别写入插件类和服务类的全名。
- `src/main/resources/org/devlive/metaforge/plugin/capabilities.json` 声明能力，至少包含 `query:execute`、`database:list`、`table:list`、`column:read`。
- e2e 测试可以用 Java 编写（`src/test/java`），参考 `MongoE2ETest`。
