---
title: Apache Kyuubi
---

<img src="/assets/plugin/kyuubi.svg" class="connector-content-logo" />

#### What is Apache Kyuubi ?

Apache Kyuubi is a multi-tenant SQL gateway that serves Spark, Flink, Trino and other engines over the HiveServer2 protocol.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `KyuubiPlugin#describe`) and connects through the JDBC driver `org.apache.kyuubi.jdbc.KyuubiHiveDriver`.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |  `10009`  |
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

    The plugin's e2e test `KyuubiE2ETest` verifies each version below in a container: with its Spark SQL engine, the version check (`kyuubi_version()`), then a database and table created with SQL are listed as a database, table and columns, and their rows read back.

    - [x] 1.10
    - [x] 1.11

!!!

#### SQL and concepts

---

| MetaForge | Apache Kyuubi |
|:--|:--|
| Database | Database of the engine |
| Table | Table or view |
| Column | Column |
