---
title: CeresDB
status: new
---

<img src="/assets/plugin/ceresdb.svg" class="connector-content-logo" />

#### What is CeresDB ?

CeresDB is a high-performance, distributed, cloud native time-series database.

#### Environment

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.9.x`

Support Time: `2023-04-12`

#### Configure

---

!!! note

    If your CeresDB service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

=== "Configure"

    | Field  |             Required              | Default Value |
    |:------:|:---------------------------------:|:-------------:|
    | `Name` | :check{20,#3CA34F}:  |       -       |
    | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
    | `Port` | :check{20,#3CA34F}: |    `5440`     |

#### Version (Validation)

---

!!! warning

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/EdurtIO/metaforge/issues/new/choose) to us

- [x] 1.x
