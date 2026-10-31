---
title: GreptimeDB
status: new
---

<img src="/assets/plugin/greptimedb.svg" class="connector-content-logo" />

#### What is GreptimeDB ?

An open-source, cloud-native, distributed time-series database with PromQL/SQL/Python supported.

#### Environment

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.9.x`

Support Time: `2023-04-14`

#### Configure

---

!!! note

    If your GreptimeDB service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |    `4002`     |
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

- Connects with the MySQL JDBC driver to GreptimeDB's MySQL protocol port (default `4002`) instead of HTTP (port `4000`); existing sources need port `4002`.

#### Version (Validation)

---

!!! note

    The plugin's e2e test `GreptimeDBE2ETest` verifies each version below in a container: the version check, then databases, tables and columns created, read, changed and dropped with SQL.

    - [x] v0.15
    - [x] v0.17

!!!

#### SQL and concepts

---

| MetaForge | GreptimeDB | SQL |
|:--|:--|:--|
| Database | Database | `CREATE DATABASE` / `DROP DATABASE` |
| Table | Table (needs one `TIME INDEX` column) | `CREATE TABLE ... (host STRING PRIMARY KEY, ts TIMESTAMP TIME INDEX, ...)` / `DROP TABLE` |
| Column | Column (tag, field or timestamp) | `ALTER TABLE ... ADD / MODIFY / DROP COLUMN` |
