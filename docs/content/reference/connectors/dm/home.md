---
title: DM (Dameng)
---

<img src="/assets/plugin/dm.svg" class="connector-content-logo" />

#### What is DM (Dameng) ?

DM (Dameng) is a relational database developed in China, largely compatible with Oracle SQL.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `DmPlugin#describe`) and connects through the JDBC driver `dm.jdbc.driver.DmDriver`.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |  `5236`  |
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

    The plugin's e2e test `DmE2ETest` verifies each version below in a container: the version check, then a schema, table and columns created with SQL are listed as a database, table and columns, and their rows read back. DM 8 is the only release line published as a container image.

    - [x] DM 8

!!!

#### SQL and concepts

---

| MetaForge | DM (Dameng) |
|:--|:--|
| Database | Schema |
| Table | Table or view |
| Column | Column |
