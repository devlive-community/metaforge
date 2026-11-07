---
title: Db2
---

<img src="/assets/plugin/db2.svg" class="connector-content-logo" />

#### What is Db2 ?

IBM Db2 is a commercial relational database.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `Db2Plugin#describe`) and connects through the JDBC driver `com.ibm.db2.jcc.DB2Driver`.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |  `50000`  |
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

    The plugin's e2e test `Db2E2ETest` verifies each version below in a container: the version check, then a schema, table and columns created with SQL are listed as a database, table and columns, and their rows read back. Set `Database` to the Db2 database, e.g. `testdb`.

    - [x] 11.5
    - [x] 12.1

!!!

#### SQL and concepts

---

| MetaForge | Db2 |
|:--|:--|
| Database | Schema of the configured database |
| Table | Table or view |
| Column | Column |
