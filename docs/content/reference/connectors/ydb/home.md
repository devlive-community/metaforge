---
title: Yandex Database
---

#### What is YDB ?

YDB is a fault-tolerant distributed SQL DBMS. YDB provides high availability, horizontal scalability, strict consistency, and ACID transaction support. Queries are made using an SQL dialect (YQL).

#### Environment

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.4.x`

!!!

Support Time: `2023-01-30`

#### Configure

---

MetaForge declares the configuration fields inside the plugin class by default (see `YdbPlugin#describe`)

!!! note

    If your YDB service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

!!!

::: tabs
    === "Configure"
        | Field  |             Required              | Default Value |
        |:------:|:---------------------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}:  |       \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |     `2136`    |
    === "Authorization"
        |   Field    |        Required         | Default Value |
        |:----------:|:-----------------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |       \-       |
        | `Password` | :x{20,#ff0000}: |       \-       |
    === "Advanced"
        |   Field    |             Required              | Default Value |
        |:----------:|:---------------------------------:|:-------------:|
        | `Database` | :check{20,#3CA34F}: |    `local`    |
:::

#### Version (Validation)

---

!!! warning

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/EdurtIO/metaforge/issues/new/choose) to us

    - [x] 2.1.x

!!!