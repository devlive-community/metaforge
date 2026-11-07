---
title: Oracle
---

<img src="/assets/plugin/oracle.svg" class="connector-content-logo" />

#### What is Oracle ?

Oracle Database is a commercial relational database; the Free edition runs the same engine for development.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `OraclePlugin#describe`) and connects through the JDBC driver `oracle.jdbc.OracleDriver`.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |  `1521`  |
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

    The plugin's e2e test `OracleE2ETest` verifies each version below in a container: the version check, then a user (schema), table and columns created with SQL are listed as a database, table and columns, and their rows read back. Set `Database` to the service name, e.g. `FREEPDB1`.

    - [x] 23ai (23.7)
    - [x] 26ai (23.26)

!!!

#### SQL and concepts

---

| MetaForge | Oracle |
|:--|:--|
| Database | User (schema) |
| Table | Table or view |
| Column | Column |
