---
title: Snowflake
---

#### What is Snowflake ?

Execute your most critical workloads on top of Snowflake's multi-cluster shared data architecture in a fully managed platform that capitalizes on the near-infinite resources of the cloud.

#### Environment

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.4.x`

!!!

Support Time: `2023-01-29`

#### Configure

---

MetaForge uses configuration files by default [snowflake.json](https://github.com/EdurtIO/metaforge/blob/develop/server/src/main/etc/conf/plugins/jdbc/snowflake.json)

!!! note

    If your Snowflake service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

!!!

::: tabs
    === "Configure"
        | Field  |             Required              | Default Value |
        |:------:|:---------------------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}:  |       \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |     `80`      |
    === "Authorization"
        |   Field    |             Required              | Default Value |
        |:----------:|:---------------------------------:|:-------------:|
        | `Username` | :check{20,#3CA34F}:  |       \-       |
        | `Password` | :check{20,#3CA34F}: |       \-       |
    === "Advanced"
        |   Field    |             Required              | Default Value |
        |:----------:|:---------------------------------:|:-------------:|
        | `Database` | :x{20,#ff0000}:           |       \-       |
    === "Custom"
    
        !!! note
    
            You can add the already supported Snowflake parameters by adding Key Value, the parameters can be [reference document](https://docs.snowflake.com/en/user-guide/jdbc-configure.html#other-parameters)
    
        !!!
:::

#### Version (Validation)

---

!!! warning

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/EdurtIO/metaforge/issues/new/choose) to us

!!!