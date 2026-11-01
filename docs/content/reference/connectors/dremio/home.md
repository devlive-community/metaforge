---
title: Dremio
---

<img src="/assets/plugin/dremio.svg" class="connector-content-logo" />

#### What is Dremio ?

Dremio is a lakehouse query engine that queries files, tables and other sources with SQL.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `DremioPlugin#describe`) and connects through the JDBC driver `org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver`.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |  `32010`  |
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

    The plugin's e2e test `DremioE2ETest` verifies each version below in a container: the version check, then a view created with SQL in the user's home space is listed as a table of a database with its columns, and its rows read back. MetaForge connects to Dremio's Arrow Flight port; set `SSL` when the port uses TLS.

    - [x] 25.2
    - [x] 26.0

!!!

#### SQL and concepts

---

| MetaForge | Dremio |
|:--|:--|
| Database | Space, source or folder, e.g. `@user` or `$scratch` |
| Table | Table or view |
| Column | Column |
