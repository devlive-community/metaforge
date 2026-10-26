---
title: Apache Pinot
status: 1.10.0
---

<img src="/assets/plugin/pinot.svg" class="connector-content-logo" />

#### What is Pinot ?

Apache Pinot is a real-time distributed OLAP datastore purpose-built for low-latency, high-throughput analytics, and perfect for user-facing analytical workloads.

#### Environment

---

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.10.x`

Support Time: `2023-05-06`

#### Configure

---


!!! note

    If your Pinot service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

=== "Configure"

    | Field  |             Required              | Default Value |
    |:------:|:---------------------------------:|:-------------:|
    | `Name` | :check{20,#3CA34F}:  |       -       |
    | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
    | `Port` | :check{20,#3CA34F}: |     `9000`    |

=== "Authorization"

    |   Field    |             Required              | Default Value |
    |:----------:|:---------------------------------:|:-------------:|
    | `Username` | :check{20,#3CA34F}:  |       -       |
    | `Password` | :check{20,#3CA34F}: |       -       |

#### Version (Validation)

---

!!! warning

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/EdurtIO/metaforge/issues/new/choose) to us

- [x] `0.8.x`
