---
title: TDengine
---

<img src="/assets/plugin/tdengine.svg" class="connector-content-logo" />

#### What is TDengine ?

TDengine is a time series database; MetaForge reaches it through taosAdapter's REST port.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `TDenginePlugin#describe`) and connects through the JDBC driver `com.taosdata.jdbc.TSDBDriver`.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |  `6041`  |
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

    The plugin's e2e test `TDengineE2ETest` verifies each version below in a container: the version check, then a database, super table and sub tables created with SQL are listed as a database, tables and columns (tags included), and their rows read back.

    - [x] 3.1
    - [x] 3.3

!!!

#### SQL and concepts

---

| MetaForge | TDengine |
|:--|:--|
| Database | Database |
| Table | Super table, sub table or table |
| Column | Column or tag |
