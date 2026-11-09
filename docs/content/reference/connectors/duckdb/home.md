---
title: DuckDB
---

#### What is DuckDB ?

DuckDB is an in-process SQL OLAP database management system.

#### Environment

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.6.x`

!!!

Support Time: `2023-02-20`

#### Configure

---

MetaForge declares the configuration fields inside the plugin class by default (see `DuckdbPlugin#describe`)

!!! note

    If your DuckDB service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

!!!

::: tabs
    === "Configure"
        | Field  |             Required              | Default Value |
        |:------:|:---------------------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}:  |       \-       |
        | `Host` | :check{20,#3CA34F}: |  `/root`  |
        | `Port` | :check{20,#3CA34F}: |     `0`    |
    === "Advanced"
        |   Field    |             Required              | Default Value |
        |:----------:|:---------------------------------:|:-------------:|
        | `Database` | :check{20,#3CA34F}: |    `local`    |
:::

#### Version (Validation)

---

!!! note

    DuckDB runs inside the MetaForge server, so the version is the DuckDB library the plugin bundles. The plugin's e2e test `DuckDBE2ETest` runs it in process, without a container: the version check, then a schema, table and columns created with SQL are listed as a database, table and columns, and their rows read back by later queries.

    - [x] 1.0.0

!!!

#### SQL and concepts

---

- `Host` is a directory on the MetaForge server and `Database` the database file in it, e.g. `/data` and `local` open `/data/local`.

| MetaForge | DuckDB |
|:--|:--|
| Database | Schema |
| Table | Table or view |
| Column | Column |
