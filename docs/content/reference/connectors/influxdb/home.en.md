---
title: InfluxDB
---

<img src="/assets/plugin/influxdb.svg" class="connector-content-logo" />

InfluxDB is an open source sequential database developed by InfluxData. It is written in Go and focuses on high-performance query and storage of time series data. InfluxDB is widely used to store monitoring data of systems.

#### describe

---

MetaForge integrates the InfluxDB module to implement data operations on InfluxDB data sources.

In this module we use the `influxdb-jdbc` dependency, version `0.2.6`.

The driver used is `net.suteren.jdbc.influxdb.InfluxDbDriver`.

For driver source code, please refer to: https://github.com/konikvranik/jdbc-influxdb/

#### operate

---

| Operation | Whether supported |
|:--------:|:----:|
| `SELECT` | ✅ |

> All operations supported by the drive are supported.

#### environment

---

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `2024.03.7`

Support time: `2024-06-26`

#### Configuration

---


!!! note

    If your service version requires other special configurations, please refer to Modifying the Configuration File and Restarting the MetaForge Service.

=== "Basic configuration"

    | Properties | Is it necessary | Default value | Remarks |
    |---|---|---|---|
    | `name` | ✅ | - |-|
    | `Host address` | ✅ | `127.0.0.1` | - |
    | `Port` | ✅ | `8086` | - |

=== "Authorization Configuration"

    | Properties | Is it necessary | Default value |
    |---|---|---|
    | `Username` | ❌ | - |
    | `Password` | ❌ | - |

=== "Custom"

    > You can add all configurations supported by the InfluxDB driver by key = value

    default:
    
    | Properties | Default Value |
    |---|---|
    | `database` | `default` |

