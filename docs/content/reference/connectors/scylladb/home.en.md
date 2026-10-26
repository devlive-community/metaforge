---
title: ScyllaDB
---

<img src="/assets/plugin/scylladb.svg" class="connector-content-logo" />

#### Introduce

ScyllaDB is a distributed NoSQL wide-column database for data-intensive apps that require high performance and low latency.

#### Environment

---

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `2024.01.1`

Support Time: `2024-01-12`

#### Configure

---

=== "Configure"

    | Field  |             Required              | Default Value |
    |:------:|:---------------------------------:|:-------------:|
    | `Name` | :check{20,#3CA34F}:  |       -       |
    | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
    | `Port` | :check{20,#3CA34F}: |     `9042`    |

=== "Advanced"

    |   Field    |             Required             | Default Value |
    |:----------:|:--------------------------------:|:-------------:|
    | `Database` | :check{20,#3CA34F}: |   ` `   |

#### Function

---

|      Function       |    Supported     |
|:-------------------:|:----------------:|
|       AD Hoc        | :check: |
|       DataSet       | :check: |
|      Pipeline       | :x: |
| Metadata management | :x: |

#### Version

---

!!! Version

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/EdurtIO/metaforge/issues/new/choose) to us

- [x] `all`
