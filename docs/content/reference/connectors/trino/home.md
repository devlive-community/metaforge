---
title: Trino
---

<img src="/assets/plugin/trino.svg" class="connector-content-logo" />

#### What is Trino ?

Trino is a distributed SQL query engine that queries many data sources through catalogs.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `TrinoPlugin#describe`) and connects through the JDBC driver `io.trino.jdbc.TrinoDriver`.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |  `8080`  |
    === "Authorization"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |      \-       |
        | `Password` | :x{20,#ff0000}: |      \-       |
    === "Advanced"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Database` | :x{20,#ff0000}: |      \-       |
:::

#### Version (Validation)

---

!!! note

    The plugin's e2e test `TrinoE2ETest` verifies each version below in a container: the version check, then a schema and table created with SQL in the `memory` catalog are listed as a database, table and columns, and their rows read back. Set `Database` to the catalog, e.g. `memory` or `hive`.

    - [x] 470
    - [x] 476

!!!

#### SQL and concepts

---

| MetaForge | Trino |
|:--|:--|
| Database | Schema of the configured catalog |
| Table | Table or view |
| Column | Column |
