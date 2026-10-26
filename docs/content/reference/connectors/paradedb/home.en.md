---
title: ParadeDB
---

<img src="/assets/plugin/paradedb.svg" class="connector-content-logo" />

#### Introduce

ParadeDB is an Elasticsearch alternative built on Postgres. We’re modernizing the features of Elasticsearch, starting with real-time search and analytics.

ParadeDB is not a fork of Postgres, but regular Postgres with custom extensions installed. ParadeDB itself ships with Postgres 16.

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
