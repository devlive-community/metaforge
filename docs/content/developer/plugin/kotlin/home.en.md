---
title: Kotlin Implementation
---

Plugins can also be written in Kotlin (e.g. `plugin/metaforge-plugin-mongo-community`, `plugin/metaforge-plugin-doris`). The rules, layout, SPI registration, capability declaration, e2e test and docs requirements are the same as in the [Java Implementation](../java/home.md); this page only shows the Kotlin way.

!!! warning

    Every plugin reaches its source only through JDBC and uses the same metadata management (database picker, table tree, columns). See the [Java Implementation](../java/home.md).
!!!

### `pom.xml` dependencies

Add the Kotlin reflection library to the Java plugin dependencies and keep sources in `src/main/kotlin` (the Kotlin compiler plugin is configured in the parent `pom.xml`):

```xml
<dependency>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-reflect</artifactId>
</dependency>
```

### Plugin class

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

### Service class

```kotlin
class MongoCommunityService : PluginService
{
    override fun connectType(): String = "mongodb"

    override fun driver(): String = "org.devlive.metaforge.MongoDriver"

    // Databases, collections and fields come from the driver's JDBC metadata
    override fun getDatabases(configure: RuntimeConfigure): Response = metadataDatabases(configure)

    override fun getTables(configure: RuntimeConfigure, database: String): Response = metadataTables(configure, database)

    override fun getColumns(configure: RuntimeConfigure, database: String, table: String): Response = metadataColumns(configure, database, table)
}
```

When metadata comes from SQL, the result column names are those in the table of the [Java Implementation](../java/home.md).

### SPI registration, capabilities and tests

- `src/main/resources/META-INF/services/org.devlive.metaforge.plugin.Plugin` and `org.devlive.metaforge.plugin.Service` hold the full names of the plugin and service classes.
- `src/main/resources/org/devlive/metaforge/plugin/capabilities.json` declares the capabilities, at least `query:execute`, `database:list`, `table:list` and `column:read`.
- The e2e test can be written in Java (`src/test/java`); see `MongoE2ETest`.
