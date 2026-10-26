---
title: DolphinDB
---

Real-time Platform for Analytics and Stream Processing Powered by High-performance Time Series Database.

#### describe

---

MetaForge integrates the DolphinDB module to implement data operations on DolphinDB data sources.

In this module we use the `com.dolphindb` dependency, version `3.00.2.0`.

The driver used is `com.dolphindb.jdbc.Driver`.

#### operate

---

| Operation | Whether supported |
|:--------:|:----:|
| `SELECT` | ✅ |

> All operations supported by the drive are supported.

#### environment

---

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `2025.1.1`

!!!

#### Configuration

---


!!! note

    If your service version requires other special configurations, please refer to Modifying the Configuration File and Restarting the MetaForge Service.

!!!

=== "Basic configuration"

    | Properties | Is it necessary | Default value | Remarks |
    |---|---|---|---|
    | `name` | ✅ | - |-|
    | `Host address` | ✅ | `127.0.0.1` | - |
    | `Port` | ✅ | `8848` | - |

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
