---
title: Apache Ignite
---

<img src="/assets/plugin/ignite.svg" class="connector-content-logo" />

#### What is Apache Ignite ?

Apache Ignite is a distributed in-memory database; MetaForge queries Ignite 2 with SQL through the thin JDBC driver.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `IgnitePlugin#describe`) and connects through the JDBC driver `org.apache.ignite.IgniteJdbcThinDriver`.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |  `10800`  |
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

    The plugin's e2e test `IgniteE2ETest` verifies each version below in a container: the version check, then a table created with SQL is listed in the `PUBLIC` schema with its columns, and its rows read back. The driver needs the MetaForge JVM started with `--add-opens=java.base/java.nio=ALL-UNNAMED`, which `configure/jvm.conf` sets.

    - [x] 2.17
    - [x] 2.18

!!!

#### SQL and concepts

---

| MetaForge | Apache Ignite |
|:--|:--|
| Database | Schema, e.g. `PUBLIC` |
| Table | Table |
| Column | Column |
