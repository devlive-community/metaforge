---
title: OceanBase
---

<img src="/assets/plugin/oceanbase.svg" class="connector-content-logo" />

#### What is OceanBase ?

OceanBase is a distributed relational database; MetaForge connects to its MySQL-mode tenants.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `OceanBasePlugin#describe`) and connects through the JDBC driver `com.oceanbase.jdbc.Driver`.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |  `2881`  |
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

    The plugin's e2e test `OceanBaseE2ETest` verifies each version below in a container: the version check, then a database, table and columns created with SQL are listed, and their rows read back, in a MySQL-mode tenant. Sign in as `user@tenant`, e.g. `root@test`.

    - [x] 4.2 LTS
    - [x] 4.3 LTS
    - [x] 4.4 LTS

!!!

#### SQL and concepts

---

| MetaForge | OceanBase |
|:--|:--|
| Database | Database |
| Table | Table or view |
| Column | Column |
