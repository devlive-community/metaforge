---
title: Java 实现
---

MetaForge 支持自定义插件。本文以仓库中的 QuestDB 插件（`plugin/metaforge-plugin-questdb`）为例，说明如何开发一个数据源插件。

### 必须遵守的规则

!!! warning

    - **只通过 JDBC 访问数据源**：使用数据源官方或第三方的 JDBC 驱动；没有 JDBC 驱动的数据源，基于 `metaforge-driver-sql`（SQL 解析器 `metaforge-parser`）在 `driver/` 下编写专属驱动，类名为 `org.devlive.metaforge.<Name>Driver`。插件名不带 `Jdbc`、`Http` 等协议词。
    - **所有插件使用同一套元数据管理**：与 MySQL 相同的结构——顶部选择数据库、左侧表树、表的列结构，以及按能力显示的标签页和按钮。不允许为某个数据源编写独立的工作区、侧边栏或树。非关系型数据源也映射为库、表、列，例如 Redis（库 `0..n` / 键 / `key`、`type`、`value`）、MongoDB（库 / 集合 / 文档字段）、Neo4j（库 / 标签 / 属性）。
    - **能力声明即契约**：只在 `capabilities.json` 中声明真正实现的能力，界面和服务端都按声明开放功能。
    - **必须有 e2e 测试**：用容器验证至少两个版本，并在连接器文档中列出验证过的版本。
!!!

### 目录结构

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

### `pom.xml` 依赖

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

构建时 `maven-dependency-plugin` 把运行依赖复制到 `target/dependencies`，插件运行时从那里加载驱动（参考已有插件的 `pom.xml`）。

### 插件类：配置字段

插件类继承 `org.devlive.metaforge.plugin.Plugin`，在 `describe()` 中声明数据源的配置字段和介绍：

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

### 服务类：连接与元数据

服务类实现 `org.devlive.metaforge.spi.PluginService`。查询通过 `driver()` 指定的 JDBC 驱动执行，连接地址默认是 `jdbc:<connectType>://host:port/database`。

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

库、表、列有两种提供方式，二选一：

1. **JDBC 元数据（推荐）**：驱动实现了 `DatabaseMetaData`（`getSchemas`/`getCatalogs`、`getTables`、`getColumns`）时，直接返回 `metadataDatabases(configure)`、`metadataTables(configure, database)`、`metadataColumns(configure, database, table)`。数据库按 JDBC schema 读取；驱动只报告 catalog 时，库列表回退到 catalog。
2. **SQL**：用数据源自己的系统表或函数查询，结果列名必须是下面这些：

| 方法 | 结果列 |
|:--|:--|
| `getDatabases` | `object_name`、`object_type`、`object_comment` |
| `getTables` | `type_name`（`table`/`view`）、`object_name`、`object_comment` |
| `getColumns` | `type_name`（`column`）、`object_name`、`object_data_type`、`object_nullable`、`object_default_value`、`object_comment`、`object_position`、`object_definition`、`object_length`、`object_autoincrement`、`object_scale` |

### 自建 JDBC 驱动

数据源没有 JDBC 驱动时，在 `driver/metaforge-driver-<name>` 下编写驱动（参考 ZooKeeper、Kafka、HDFS、Redis、MongoDB 驱动）：

- 依赖 `metaforge-driver-sql`，实现 `SqlSource`（`databases()`、`tables(database)`、`columns(database, table)`、`rows(...)`），`Connection.getMetaData()` 返回 `new SqlSourceDatabaseMetaData(new SqlSourceConnection(source, url, database, user))`，插件即可走 JDBC 元数据。
- `Statement.execute(sql)` 必须把查询结果留在 `getResultSet()` 并返回 `true`、`getUpdateCount()` 返回 `-1`：MetaForge 只通过 `execute()` 运行语句，返回 `false` 会让所有查询变成更新数 0。
- 空结果也必须有 `ResultSetMetaData`（列名可为空）。
- 配置的数据库必须生效：从连接属性 `database` 或 URL 路径（`jdbc:<type>://host:port/<database>`）读取。
- 在 `META-INF/services/java.sql.Driver` 中登记驱动类。

### SPI 注册

在 `src/main/resources/META-INF/services` 下创建两个文件：

`org.devlive.metaforge.plugin.Plugin`

```
org.devlive.metaforge.plugin.QuestDBPlugin
```

`org.devlive.metaforge.plugin.Service`

```
org.devlive.metaforge.plugin.QuestDBService
```

### 管理能力声明

数据源管理（控制台、数据导入、数据导出、数据对比、ER 图）中的每个页面、标签页、弹窗、抽屉、侧边栏和按钮都由一个管理能力控制。插件在 `src/main/resources/org/devlive/metaforge/plugin/capabilities.json` 中声明自己实现了哪些能力（每个插件都放在这个路径，读取时只读插件自己 jar 里的这一份）：

```json
{
  "kind": "relational",
  "capabilities": ["query:execute", "database:list", "table:list", "column:read", "tab:structure", "tab:sql"]
}
```

- `kind` 描述数据源类型：`relational`、`key-value`、`document`、`storage`、`graph`、`unknown`。所有类型使用同一套界面，界面只由能力决定。
- 每个插件至少声明 `query:execute`、`database:list`、`table:list`、`column:read`，让顶部选库、表树和列结构可用。
- 没有声明文件的插件不提供数据管理：数据源列表隐藏「数据管理」入口，相关页面也无法打开。
- 声明的每个能力都必须真正实现；服务端同样按声明拒绝未声明的操作。
- 声明未知的能力或 `kind` 会在加载插件时报错；子类所在的 jar 没有声明文件时沿用父类 jar 里的声明。
- 新增管理功能时，先在 `ManagementCapability` 中登记能力，再在需要的插件中声明，并让该功能的每个界面入口都检查这个能力。
- `namespace:list`, `key:list`, `key:read`, `object:list`, `object:read`, `object:download`, `tab:keys`, `tab:objects` 属于已废弃的独立浏览器，插件不得声明。

| 能力 | 控制的界面 |
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

### e2e 测试

每个插件都要有 e2e 测试，用 Testcontainers 在容器中验证每个支持的版本（参考 `QuestDBE2ETest`、`MySQLE2ETest`、`RedisE2ETest`）：

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

测试至少覆盖：服务端版本、`getDatabases`、`getTables`、`getColumns`，以及读回其他客户端写入的数据。`PluginE2E`（`test/metaforge-test-e2e`）提供 `assumeDocker`、`configure`、`ok`、`column`、`first`。

### 本地调试与文档

- 在 `configure/etc/conf/plugins.properties` 中登记插件模块（`metaforge-plugin-questdb=plugin/metaforge-plugin-questdb/pom.xml`），执行 `./mvnw install -pl plugin/metaforge-plugin-questdb` 后重启服务，开发模式会从模块的 `target/classes` 和 `target/dependencies` 加载插件。
- 在 `docs/content/reference/connectors/<name>/home.md` 编写连接器文档，「版本（验证）」中列出 e2e 验证过的版本，并在 `docs/pageforge.yaml` 中登记。
