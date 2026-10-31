---
title: CrateDB
---

<img src="/assets/plugin/cratedb.svg" class="connector-content-logo" />

#### What is CrateDB ?

CrateDB is a distributed SQL database for real-time analytics that speaks the PostgreSQL protocol.

#### Configure

---

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |    `5432`     |
    === "Authorization"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |   `crate`     |
        | `Password` | :x{20,#ff0000}: |      \-       |
    === "Advanced"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Database` | :x{20,#ff0000}: |      \-       |
:::

- Connects with the CrateDB JDBC driver (PostgreSQL protocol, default port `5432`); the HTTP way (port `4200`) is gone.
- The former "CrateDB HTTP" plugin is merged into this one: existing sources switch to it on upgrade, and sources set to port `4200` need `5432` instead.

#### Version (Validation)

---

!!! note

    The plugin's e2e test `CrateDBE2ETest` verifies each version below in a container: the version check, then schemas, tables and columns created, read, changed and dropped with SQL.

    - [x] 5.10
    - [x] 6.1

!!!

#### SQL and concepts

---

| MetaForge | CrateDB | SQL |
|:--|:--|:--|
| Database | Schema | Created with its first table (`CREATE TABLE schema.table ...`) and gone with its last; CrateDB has no `CREATE SCHEMA` |
| Table | Table | `CREATE TABLE` / `DROP TABLE`; `REFRESH TABLE` makes written rows visible to queries at once |
| Column | Column | `ALTER TABLE ... ADD COLUMN` / `RENAME COLUMN` / `DROP COLUMN` (a column's type can not change) |
