---
title: Hologres
status: new
---

<img src="/assets/plugin/hologres.svg" class="connector-content-logo" />

#### What is Hologres ?

Hologres is an all-in-one real-time data warehouse engine that is compatible with PostgreSQL. It supports online analytical processing (OLAP) and ad hoc analysis of PB-scale data. Hologres supports online data serving at high concurrency and low latency. It is deeply integrated with MaxCompute, Flink and DataWorks, provides a full-stack data warehouse solution that integrates online and offline processing.

#### Environment

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.9.x`

Support Time: `2023-04-25`

#### Configure

---

!!! note

    If your Hologres service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

=== "Configure"

    | Field  |             Required              | Default Value |
    |:------:|:---------------------------------:|:-------------:|
    | `Name` | :check{20,#3CA34F}:  |       -       |
    | `Host` | :check{20,#3CA34F}: |  `hologres-cn-regison.aliyuncs.com`  |
    | `Port` | :check{20,#3CA34F}: |     `80`    |

=== "Authorization"

    |   Field    |        Required         | Default Value |
    |:----------:|:-----------------------:|:-------------:|
    | `Username` | :x{20,#ff0000}: |       -       |
    | `Password` | :x{20,#ff0000}: |       -       |

=== "Advanced"

    |   Field    |        Required         | Default Value |
    |:----------:|:-----------------------:|:-------------:|
    | `Database` | :x{20,#ff0000}: |       -       |

#### Version (Validation)

---

!!! warning

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/EdurtIO/metaforge/issues/new/choose) to us

- [x] all
