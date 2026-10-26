---
title: Apache Cassandra
status: 1.11.0
---

#### What is Cassandra ?

Apache Cassandra® powers mission-critical deployments with improved performance and unparalleled levels of scale in the cloud.

#### Environment

---

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.11.x`

!!!

Support Time: `2023-06-07`

#### Configure

---


!!! note

    If your plugin service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

!!!

::: tabs
    === "Configure"
        | Field  |             Required              | Default Value |
        |:------:|:---------------------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}:  |       \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |     `9042`    |
    === "Advanced"
        |   Field    |             Required             | Default Value |
        |:----------:|:--------------------------------:|:-------------:|
        | `Database` | :check{20,#3CA34F}: | `datacenter`  |
:::

#### Version (Validation)

---

!!! warning

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/EdurtIO/metaforge/issues/new/choose) to us

    - [x] `0.4.x`

!!!