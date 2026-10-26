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

MetaForge uses configuration files by default [duckdb.json](https://github.com/EdurtIO/metaforge/blob/develop/server/src/main/etc/conf/plugins/jdbc/duckdb.json)

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

!!! warning

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/EdurtIO/metaforge/issues/new/choose) to us

    - [x] 0.7.0

!!!