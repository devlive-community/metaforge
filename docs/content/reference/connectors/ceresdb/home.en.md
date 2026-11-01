---
title: CeresDB
status: new
---

<img src="/assets/plugin/ceresdb.svg" class="connector-content-logo" />

#### What is CeresDB ?

CeresDB is a high-performance, distributed, cloud native time-series database.

#### Environment

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.9.x`

Support Time: `2023-04-12`

#### Configure

---

!!! note

    If your CeresDB service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |    `5433`     |
    === "Authorization"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |      \-       |
        | `Password` | :x{20,#ff0000}: |      \-       |
    === "Advanced"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Database` | :x{20,#ff0000}: |   `public`    |
:::

- Connects with the PostgreSQL JDBC driver to the PostgreSQL protocol port of CeresDB (HoraeDB), default `5433`, using the simple query protocol (`preferQueryMode=simple`) automatically, instead of HTTP (port `5440`); existing sources need port `5433`.

#### Version (Validation)

---

!!! note

    The plugin's e2e test `CeresDBE2ETest` verifies each version below in a container: the connection check, then tables created, read and dropped and columns added with SQL.

    - [x] CeresDB 1.2
    - [x] HoraeDB 2.1

!!!

#### SQL and concepts

---

| MetaForge | CeresDB | SQL |
|:--|:--|:--|
| Database | The single schema `public` | Can not be created or dropped |
| Table | Table (needs a `TIMESTAMP KEY`) | `CREATE TABLE ... (name string TAG, ts timestamp NOT NULL, TIMESTAMP KEY(ts)) ENGINE=Analytic` / `DROP TABLE`; listed by `SHOW TABLES` |
| Column | Column (tag or field) | Only `ALTER TABLE ... ADD COLUMN`: CeresDB can not change or drop a column; described by `DESCRIBE` |

- Tables keep data for 7 days by default (TTL), so rows with older timestamps expire at once.
