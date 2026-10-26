---
title: Apache Doris
status: new
---

<img src="/assets/plugin/doris.svg" class="connector-content-logo" />

#### What is Doris ?

An easy-to-use, high-performance and unified analytical database

#### Environment

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.9.x`

Support Time: `2023-04-19`

#### Configure

---

!!! note

    If your Doris service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

=== "Configure"

    | Field  |             Required              | Default Value |
    |:------:|:---------------------------------:|:-------------:|
    | `Name` | :check{20,#3CA34F}:  |       -       |
    | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
    | `Port` | :check{20,#3CA34F}: |     `9093`    |

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
