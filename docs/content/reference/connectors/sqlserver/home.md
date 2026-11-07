---
title: SQL Server
---

<img src="/assets/plugin/sqlserver.svg" class="connector-content-logo" />

#### What is SQL Server ?

Microsoft SQL Server is a commercial relational database.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `SqlServerPlugin#describe`) and connects through the JDBC driver `com.microsoft.sqlserver.jdbc.SQLServerDriver`.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |  `1433`  |
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

    The plugin's e2e test `SqlServerE2ETest` verifies each version below in a container: the version check, then a schema, table and columns created with SQL are listed as a database, table and columns, and their rows read back. Add `trustServerCertificate=true` under Custom when the server uses a self-signed certificate.

    - [x] 2019
    - [x] 2022

!!!

#### SQL and concepts

---

| MetaForge | SQL Server |
|:--|:--|
| Database | Schema of the configured database |
| Table | Table or view |
| Column | Column |
