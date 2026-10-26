---
title: Timescale
---

<img src="/assets/plugin/timescale.svg" class="connector-content-logo" />

#### Introduce

Timescale is a PostgreSQL data platform for time-series, events, and analytics. It gives you the reliability of PostgreSQL, the time-series superpowers of TimescaleDB, and the peace of mind of a fully managed service. It provides features like automatic backup and restore, high availability with replication, seamless scaling and resizing, and much more.

#### Environment

---

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `2024.02.1`

Support Time: `2024-02-05`

#### Configure

---

=== "Configure"

    | Field  |             Required              | Default Value |
    |:------:|:---------------------------------:|:-------------:|
    | `Name` | :check{20,#3CA34F}:  |       -       |
    | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
    | `Port` | :check{20,#3CA34F}: |     `5432`    |

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

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/devlive-community/metaforge/issues/new/choose) to us

- [x] `all`
