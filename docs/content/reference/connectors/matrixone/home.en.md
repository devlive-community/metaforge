---
title: MatrixOne
---

<img src="/assets/plugin/matrixone.svg" class="connector-content-logo" />

#### Introduce

MatrixOne is a future-oriented hyper-converged cloud and edge native DBMS that supports transactional, analytical, and streaming workloads with a simplified and distributed database engine, across multiple data centers, clouds, edges and other heterogeneous infrastructures.

#### Environment

---

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `2024.01.1`

Support Time: `2024-01-12`

#### Configure

---


!!! note

    If your MatrixOne service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

=== "Configure"

    | Field  |             Required              | Default Value |
    |:------:|:---------------------------------:|:-------------:|
    | `Name` | :check{20,#3CA34F}:  |       -       |
    | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
    | `Port` | :check{20,#3CA34F}: |     `6001`    |

=== "Authorization"

    |   Field    |             Required              | Default Value |
    |:----------:|:---------------------------------:|:-------------:|
    | `Username` | :check{20,#3CA34F}:  |       -       |
    | `Password` | :check{20,#3CA34F}: |       -       |
    |   `SSL`    |      :x{20,#ff0000}:      |    `false`    |

=== "Advanced"

    |   Field    |             Required             | Default Value |
    |:----------:|:--------------------------------:|:-------------:|
    | `Database` | :check{20,#3CA34F}: |   ` `   |

=== "Custom"

    There is no default configuration, and you can add it by user

#### Function

---

|      Function       |    Supported     |
|:-------------------:|:----------------:|
|       AD Hoc        | :check: |
|       DataSet       | :check: |
|      Pipeline       | :check: |
| Metadata management | :x: |

#### Version

---

!!! warning

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/EdurtIO/metaforge/issues/new/choose) to us

- [x] `1.1.0`
