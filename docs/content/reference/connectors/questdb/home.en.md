---
title: QuestDB
status: new
---

<img src="/assets/plugin/questdb.svg" class="connector-content-logo" />

#### What is QuestDB ?

QuestDB is an open-source time-series database for high throughput ingestion and fast SQL queries with operational simplicity. It supports schema-agnostic ingestion using the InfluxDB line protocol, PostgreSQL wire protocol, and a REST API for bulk imports and exports.

#### Environment

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.9.x`

Support Time: `2023-04-17`

#### Configure

---

!!! note

    If your QuestDB service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |    `8812`     |
    === "Authorization"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |   `admin`     |
        | `Password` | :x{20,#ff0000}: |      \-       |
    === "Advanced"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Database` | :x{20,#ff0000}: |    `qdb`      |
:::

- Connects with the PostgreSQL JDBC driver to QuestDB's PostgreSQL wire port (default `8812`, default account `admin` / `quest`) instead of HTTP (port `9000`); existing sources need port `8812` and the account.

#### Version (Validation)

---

!!! note

    The plugin's e2e test `QuestDBE2ETest` verifies each version below in a container: the version check, then tables and columns created, read, changed and dropped with SQL.

    - [x] 8.3
    - [x] 9.1

!!!

#### SQL and concepts

---

| MetaForge | QuestDB | SQL |
|:--|:--|:--|
| Database | The server's single database `qdb` | Can not be created or dropped |
| Table | Table | `CREATE TABLE ... TIMESTAMP(ts) PARTITION BY DAY` / `DROP TABLE`; listed by `tables()` |
| Column | Column | `ALTER TABLE ... ADD COLUMN` / `RENAME COLUMN` / `DROP COLUMN`; described by `table_columns()` |

- WAL tables (the default for partitioned tables) apply schema changes in the background; refresh shortly after a change to see it.
