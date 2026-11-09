---
title: Apache Hive
---

<img src="/assets/plugin/hive2x.svg" class="connector-content-logo" />

#### What is Apache Hive ?

Apache Hive is a data warehouse that queries data in distributed storage with SQL through HiveServer2.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `Hive2xPlugin#describe`) and connects through the JDBC driver `org.apache.hive.jdbc.HiveDriver`.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |  `10000`  |
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

    The plugin's e2e test `Hive2xE2ETest` verifies each version below in a container: the version check, then a database and table created with SQL are listed as a database, table and columns, and their rows read back. The plugin bundles the Hive 2.3 JDBC driver, which HiveServer2 4.x accepts.

    - [x] 4.0
    - [x] 4.1

!!!

#### SQL and concepts

---

| MetaForge | Apache Hive |
|:--|:--|
| Database | Database |
| Table | Table or view |
| Column | Column |
