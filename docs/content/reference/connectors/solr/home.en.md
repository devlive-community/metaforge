---
title: Apache Solr
---

<img src="/assets/plugin/solr.svg" class="connector-content-logo" />

Apache Solr is a fast open source search platform built on Apache Lucene™, providing scalable indexing and searching, as well as faceting, hit highlighting, and advanced analytics/tokenization capabilities. Solr is managed by the Apache Software Foundation.

#### Describe

---

MetaForge integrates the Apache Solr module to implement data operations on Apache Solr data sources.

In this module we use the `solr-solrj` dependency, version `6.0.0`.

The driver used is `org.apache.solr.client.solrj.io.sql.DriverImpl`.

#### Operate

---

| Operation | Whether supported |
|:---------:|:-----------------:|
| `SELECT`  |         ✅         |

> All operations supported by the drive are supported.

#### Environment

---

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `2024.03.7`

Support time: `2024-06-25`

#### Configuration

---

!!! note

    If your service version requires other special configurations, please refer to Modifying the Configuration File and Restarting the MetaForge Service.

=== "Basic configuration"

    | Properties | Is it necessary | Default value | Remarks |
    |---|---|---|---|
    | `name` | ✅ | - |-|
    | `Host address` | ✅ | `127.0.0.1` | The address here needs to be filled in with the relevant address of zookeeper |
    | `Port` | ✅ | `8983` | - |

=== "Authorization Configuration"

    | Properties | Is it necessary | Default value |
    |---|---|---|
    | `Username` | ❌ | - |
    | `Password` | ❌ | - |

=== "Advanced Configuration"

    | Properties | Is it necessary | Default value | Remarks |
    |---|---|---|---|
    | `database` | ✅ | `local` | This refers to the collection created in the solr server |

=== "Custom"

    > You can add all configurations supported by the Solr driver by key = value

    No default configuration yet
    