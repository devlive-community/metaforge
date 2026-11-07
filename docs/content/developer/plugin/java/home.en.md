---
title: Java Implementation
---

MetaForge supports custom plugins. This page uses the QuestDB plugin in the repository (`plugin/metaforge-plugin-questdb`) to show how a source plugin is built.

### Rules every plugin follows

!!! warning

    - **Sources are reached only through JDBC**: use the source's official or a third-party JDBC driver. A source without one gets its own driver under `driver/`, built on `metaforge-driver-sql` (the `metaforge-parser` SQL parser) and named `org.devlive.metaforge.<Name>Driver`. Plugin names carry no protocol words such as `Jdbc` or `Http`.
    - **Every plugin uses the same metadata management**: the MySQL structure — a database picker at the top, the table tree, the columns of a table, and tabs and buttons shown by capability. No source gets its own workspace, sidebar or tree. Non-relational sources map onto databases, tables and columns too, e.g. Redis (databases `0..n` / keys / `key`, `type`, `value`), MongoDB (databases / collections / document fields), Neo4j (database / labels / properties).
    - **Capabilities are a contract**: declare in `capabilities.json` only what the plugin implements; the UI and the server open features by the declaration.
    - **An e2e test is required**: verify at least two versions in containers and list the verified versions in the connector page.
!!!

### Layout

```
plugin/metaforge-plugin-questdb
├── pom.xml
└── src
    ├── main
    │   ├── java/org/devlive/metaforge/plugin
    │   │   ├── QuestDBPlugin.java        # 配置字段与介绍
    │   │   └── QuestDBService.java       # 驱动、连接与元数据
    │   └── resources
    │       ├── META-INF/services
    │       │   ├── org.devlive.metaforge.plugin.Plugin
    │       │   └── org.devlive.metaforge.plugin.Service
    │       └── org/devlive/metaforge/plugin/capabilities.json
    └── test/java/org/devlive/metaforge/plugin/QuestDBE2ETest.java
```

### `pom.xml` dependencies

```xml
<dependency>
    <groupId>org.devlive.metaforge</groupId>
    <artifactId>metaforge-spi</artifactId>
    <scope>provided</scope>
</dependency>
<!-- 数据源的 JDBC 驱动，或 driver/ 下的自建驱动 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>${metaforge.pgsql.version}</version>
</dependency>
<dependency>
    <groupId>org.devlive.metaforge</groupId>
    <artifactId>metaforge-test-e2e</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
```

At build time `maven-dependency-plugin` copies the runtime dependencies to `target/dependencies`, where the plugin loads its driver from (see the `pom.xml` of an existing plugin).

### Plugin class: configuration fields

The plugin class extends `org.devlive.metaforge.plugin.Plugin` and declares the source's configuration fields and descriptions in `describe()`:

```java
public class QuestDBPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        PluginConfigure configure = PluginConfigure.of("QuestDB", Arrays.asList(
                PluginConfigureField.builder().field(PluginConfigureFieldName.name).type(PluginConfigureFieldType.String).required(true).build(),
                PluginConfigureField.builder().field(PluginConfigureFieldName.host).type(PluginConfigureFieldType.String).required(true).value("127.0.0.1").build(),
                PluginConfigureField.builder().field(PluginConfigureFieldName.port).type(PluginConfigureFieldType.Number).required(true).value(8812).build(),
                PluginConfigureField.builder().field(PluginConfigureFieldName.username).type(PluginConfigureFieldType.String)
                        .group(PluginConfigureFieldGroup.authorization).build(),
                PluginConfigureField.builder().field(PluginConfigureFieldName.password).type(PluginConfigureFieldType.String)
                        .group(PluginConfigureFieldGroup.authorization).build(),
                PluginConfigureField.builder().field(PluginConfigureFieldName.database).type(PluginConfigureFieldType.String).value("qdb")
                        .group(PluginConfigureFieldGroup.advanced).build()),
                null);
        configure.setDescriptions(Map.of("zh", "QuestDB 是高性能时序数据库。", "en", "QuestDB is a high-performance time-series database."));
        configure.setCategory(PluginConfigureCategory.NOSQL);
        return configure;
    }
}
```

### Service class: connection and metadata

The service class implements `org.devlive.metaforge.spi.PluginService`. Queries run through the JDBC driver named by `driver()`; the connection URL defaults to `jdbc:<connectType>://host:port/database`.

```java
public class QuestDBService
        implements PluginService
{
    @Override
    public String driver()
    {
        return "org.postgresql.Driver";
    }

    @Override
    public String connectType()
    {
        return "postgresql";
    }

    /** 连接测试执行的语句，第一列返回服务端版本。 */
    @Override
    public String validator()
    {
        return "SELECT build() AS version";
    }

    @Override
    public Response getDatabases(RuntimeConfigure configure)
    {
        return this.execute(configure, "SELECT current_database() AS object_name, 'DATABASE' AS object_type, '' AS object_comment");
    }

    @Override
    public Response getTables(RuntimeConfigure configure, String database)
    {
        return this.execute(configure, "SELECT 'table' AS type_name, table_name AS object_name, '' AS object_comment FROM tables()");
    }

    @Override
    public Response getColumns(RuntimeConfigure configure, String database, String table)
    {
        return this.execute(configure, "SELECT 'column' AS type_name, \"column\" AS object_name, type AS object_data_type FROM table_columns('" + table + "')");
    }
}
```

Databases, tables and columns come one of two ways:

1. **JDBC metadata (preferred)**: when the driver implements `DatabaseMetaData` (`getSchemas`/`getCatalogs`, `getTables`, `getColumns`), return `metadataDatabases(configure)`, `metadataTables(configure, database)` and `metadataColumns(configure, database, table)`. Databases are read as JDBC schemas; a driver that reports only catalogs falls back to them for the database list.
2. **SQL**: query the source's own system tables or functions, with these result column names:

| Method | Result columns |
|:--|:--|
| `getDatabases` | `object_name`, `object_type`, `object_comment` |
| `getTables` | `type_name` (`table`/`view`), `object_name`, `object_comment` |
| `getColumns` | `type_name` (`column`), `object_name`, `object_data_type`, `object_nullable`, `object_default_value`, `object_comment`, `object_position`, `object_definition`, `object_length`, `object_autoincrement`, `object_scale` |

### Writing a JDBC driver

A source without a JDBC driver gets one under `driver/metaforge-driver-<name>` (see the ZooKeeper, Kafka, HDFS, Redis and MongoDB drivers):

- Depend on `metaforge-driver-sql` and implement `SqlSource` (`databases()`, `tables(database)`, `columns(database, table)`, `rows(...)`); `Connection.getMetaData()` returns `new SqlSourceDatabaseMetaData(new SqlSourceConnection(source, url, database, user))`, so the plugin can use the JDBC metadata route.
- `Statement.execute(sql)` must leave a query's rows in `getResultSet()`, return `true`, and `getUpdateCount()` must return `-1`: MetaForge runs every statement through `execute()`, and `false` turns every query into an update count of 0.
- An empty result still needs `ResultSetMetaData` (with no columns).
- The configured database must take effect: read it from the `database` property or the URL path (`jdbc:<type>://host:port/<database>`).
- Register the driver class in `META-INF/services/java.sql.Driver`.

### SPI registration

Create two files under `src/main/resources/META-INF/services`:

`org.devlive.metaforge.plugin.Plugin`

```
org.devlive.metaforge.plugin.QuestDBPlugin
```

`org.devlive.metaforge.plugin.Service`

```
org.devlive.metaforge.plugin.QuestDBService
```

### Management capabilities

Every page, tab, dialog, drawer, side panel and button of source management (workbench, data import, data export, data compare, ER diagram) is governed by one management capability. A plugin declares the capabilities it implements in `src/main/resources/org/devlive/metaforge/plugin/capabilities.json` (every plugin keeps it at that path; only the copy in the plugin's own jar is read):

```json
{
  "kind": "relational",
  "capabilities": ["query:execute", "database:list", "table:list", "column:read", "tab:structure", "tab:sql"]
}
```

- `kind` describes the source: `relational`, `key-value`, `document`, `storage`, `graph`, `unknown`. Every kind uses the same UI; only the capabilities decide what it shows.
- Every plugin declares at least `query:execute`, `database:list`, `table:list` and `column:read`, so the database picker, the table tree and the columns work.
- A plugin without the file offers no management: the source list hides "Data management" and its pages can not be opened.
- Every declared capability must be implemented; the server also refuses operations that were not declared.
- An unknown capability or `kind` fails the plugin when it loads; a subclass whose jar has no file inherits its parent jar's declaration.
- A new management feature registers its capability in `ManagementCapability` first, then plugins declare it, and every UI entry of the feature checks it.
- `namespace:list`, `key:list`, `key:read`, `object:list`, `object:read`, `object:download`, `tab:keys`, `tab:objects` belong to the retired separate browsers and must not be declared.

| Capability | UI it governs |
|---|---|
| `query:execute` | Run SQL in the query console and the SQL tab |
| `database:list` | Database selector of the workbench header and the database list of the side tree |
| `database:info` | Database overview page |
| `database:alter` | Edit database properties button and drawer |
| `engine:list` | Engine select of the create table dialog and engine choices of the table properties drawer |
| `datatype:list` | Type selects of the create table dialog and the column editors (free text otherwise) |
| `table:list` | Table tree of the side panel and its search |
| `table:info` | Table header facts and the table properties tab |
| `table:read` | Data preview tab, data compare and the sample data DDL |
| `table:statement` | DDL tab, the DDL quick menu and structure sync SQL |
| `table:create` | New table entries (side tree, more actions, database overview) and new tables of an import |
| `table:alter` | Edit table properties button and drawer |
| `table:drop` | Drop table entries and dialog |
| `table:truncate` | Truncate table entries and dialog, and empty-then-import |
| `table:export` | Export data entries, dialog and the data export page |
| `table:auto-increment` | Auto increment start of the create table dialog and the auto increment switches of the column editors |
| `table:options` | Charset, collation and row format of the create table dialog |
| `column:read` | Column structure, structure compare and the data compare entry |
| `column:create` | Add and insert column buttons and the column drawer |
| `column:modify` | Edit column button and drawer |
| `column:drop` | Drop column button and dialog |
| `column:reorder` | Column drag sorting and the order dialog |
| `index:read` | Index list |
| `index:create` | New index button and drawer, and the index step of the create table dialog |
| `index:drop` | Drop index button and dialog |
| `foreign-key:read` | Foreign key list |
| `foreign-key:create` | New foreign key button and drawer, and the foreign key tab of the create table dialog |
| `foreign-key:drop` | Drop foreign key button and dialog |
| `trigger:read` | Trigger list |
| `trigger:create` | New and copy trigger buttons and the trigger drawer |
| `trigger:drop` | Drop trigger button and dialog |
| `data:insert` | Add row in the data preview and the data import page |
| `data:modify` | Edit row in the data preview |
| `data:delete` | Delete rows in the data preview |
| `tab:structure` | Column structure tab |
| `tab:data` | Data preview tab |
| `tab:sql` | SQL tab |
| `tab:indexes` | Indexes tab |
| `tab:foreignkeys` | Foreign keys tab |
| `tab:triggers` | Triggers tab |
| `tab:info` | Table properties tab |
| `tab:statement` | DDL tab |
| `tab:er-diagram` | Table ER tab and the database ER page |

### e2e test

Every plugin has an e2e test that verifies each supported version in a container with Testcontainers (see `QuestDBE2ETest`, `MySQLE2ETest`, `RedisE2ETest`):

```java
@RunWith(Parameterized.class)
public class QuestDBE2ETest
{
    @Parameterized.Parameters(name = "QuestDB {0}")
    public static List<String> versions()
    {
        return List.of("8.3", "9.1");
    }

    @Test
    public void tablesAndColumnsAreManaged()
    {
        assumeDocker();
        try (GenericContainer<?> container = new GenericContainer<>("questdb/questdb:" + version).withExposedPorts(8812)) {
            container.start();
            PluginService service = new QuestDBService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(8812), "admin", "quest", "qdb");

            Assert.assertTrue(first(ok("validator", service.execute(configure, service.validator()))).startsWith(version + "."));
            ok("create", service.execute(configure, "CREATE TABLE trades (ts TIMESTAMP, price DOUBLE) TIMESTAMP(ts)"));
            Assert.assertTrue(column(ok("databases", service.getDatabases(configure)), "object_name").contains("qdb"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "qdb")), "object_name").contains("trades"));
            Assert.assertTrue(column(ok("columns", service.getColumns(configure, "qdb", "trades")), "object_name").contains("price"));
        }
    }
}
```

The test covers at least the server version, `getDatabases`, `getTables`, `getColumns`, and reading back data another client wrote. `PluginE2E` (`test/metaforge-test-e2e`) provides `assumeDocker`, `configure`, `ok`, `column` and `first`.

### Local development and docs

- Register the plugin module in `configure/etc/conf/plugins.properties` (`metaforge-plugin-questdb=plugin/metaforge-plugin-questdb/pom.xml`), run `./mvnw install -pl plugin/metaforge-plugin-questdb` and restart the server; in development it loads the plugin from the module's `target/classes` and `target/dependencies`.
- Write the connector page at `docs/content/reference/connectors/<name>/home.md`, list the e2e-verified versions under "Version (Validation)", and register it in `docs/pageforge.yaml`.
