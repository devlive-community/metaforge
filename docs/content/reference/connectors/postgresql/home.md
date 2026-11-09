---
title: PostgreSQL
---

<img src="/assets/plugin/postgresql.svg" class="connector-content-logo" />

#### What is PostgreSQL ?

PostgreSQL is an open-source relational database known for standards compliance and extensibility.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `PostgreSQLPlugin#describe`) and connects through the PostgreSQL JDBC driver (`org.postgresql.Driver`).

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

    The plugin's e2e test `PostgreSQLE2ETest` verifies each version below in a container: the version check, then a schema, table and columns created with SQL are listed as a database, table and columns, and their rows read back.

    - [x] 15
    - [x] 16
    - [x] 17

!!!

#### SQL and concepts

---

| MetaForge | PostgreSQL |
|:--|:--|
| Database | Schema of the configured database |
| Table | Table or view |
| Column | Column |
