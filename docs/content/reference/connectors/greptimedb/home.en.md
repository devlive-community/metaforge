---
title: GreptimeDB
status: new
---

<img src="/assets/plugin/greptimedb.svg" class="connector-content-logo" />

#### What is GreptimeDB ?

An open-source, cloud-native, distributed time-series database with PromQL/SQL/Python supported.

#### Environment

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.9.x`

Support Time: `2023-04-14`

#### Configure

---

!!! note

    If your GreptimeDB service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

=== "Configure"

    | Field  |             Required              | Default Value |
    |:------:|:---------------------------------:|:-------------:|
    | `Name` | :check{20,#3CA34F}:  |       -       |
    | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
    | `Port` | :check{20,#3CA34F}: |    `4000`     |

#### Version (Validation)

---

!!! warning

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/EdurtIO/metaforge/issues/new/choose) to us

- [x] 0.x
