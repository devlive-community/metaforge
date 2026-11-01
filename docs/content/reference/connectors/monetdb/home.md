---
title: MonetDB
---

<img src="/assets/plugin/monetdb.svg" class="connector-content-logo" />

#### What is MonetDB ?

MonetDB is an open-source column-store database built for analytical queries.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `MonetDBPlugin#describe`) and connects through the MonetDB JDBC driver (`nl.cwi.monetdb.jdbc.MonetDriver`).

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |    `50000`    |
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

    The plugin's e2e test `MonetDBE2ETest` verifies each version below in a container: the version check, then a schema, table and columns created with SQL are listed as a database, table and columns, and their rows read back.

    - [x] Aug2024 (11.51)
    - [x] Mar2025 (11.53)

!!!

#### SQL and concepts

---

| MetaForge | MonetDB |
|:--|:--|
| Database | Schema of the configured database |
| Table | Table or view |
| Column | Column |
