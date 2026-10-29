---
title: Redis
---

#### What is Redis ?

The open source, in-memory data store used by millions of developers as a database, cache, streaming engine, and message broker.

#### Environment

!!! note
    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.3.x`

!!!

Support Time: `2022-12-01`

#### Configure

---

MetaForge declares the configuration fields inside the plugin class by default (see `RedisPlugin#describe`)

!!! note

    Queries are executed through the JDBC driver (`metaforge-driver-redis`), supporting the `SELECT` and `SHOW` keywords.

!!!

::: tabs
    === "Configure"
        | Field  |             Required              | Default Value |
        |:------:|:---------------------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}:  |       \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |    `6379`     |
    === "Authorization"
        |   Field    |        Required         | Default Value |
        |:----------:|:-----------------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |       \-       |
        | `Password` | :x{20,#ff0000}: |       \-       |
    === "Advanced"
        |   Field    |        Required         | Default Value |
        |:----------:|:-----------------------:|:-------------:|
        | `Database` | :x{20,#ff0000}: |       `0`       |
:::

#### Version (Validation)

---

!!! warning

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/EdurtIO/metaforge/issues/new/choose) to us

    - [x] 6.x
    - [x] 7.x

!!!