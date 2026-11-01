---
title: ClickHouse
---

<img src="/assets/plugin/clickhouse.svg" class="connector-content-logo" />

#### What is ClickHouse ?

ClickHouse® is a column-oriented database management system (DBMS) for online analytical processing of queries (OLAP). ClickHouse’s performance exceeds all other column-oriented database management systems. It processes billions of rows and tens of gigabytes of data per server per second.

#### Environment

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.0.x`

Support Time: `2022-09-22`

#### Configure

---

MetaForge declares the configuration fields inside the plugin class by default (see `ClickhousePlugin#describe`)

!!! note

    If your ClickHouse service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

=== "Configure"

    | Field  |             Required              | Default Value |
    |:------:|:---------------------------------:|:-------------:|
    | `Name` | :check{20,#3CA34F}:  |       -       |
    | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
    | `Port` | :check{20,#3CA34F}: |     `8123`    |

=== "Authorization"

    |   Field    |        Required         | Default Value |
    |:----------:|:-----------------------:|:-------------:|
    | `Username` | :x{20,#ff0000}: |       -       |
    | `Password` | :x{20,#ff0000}: |       -       |

=== "Advanced"

    |   Field    |        Required         | Default Value |
    |:----------:|:-----------------------:|:-------------:|
    | `Database` | :x{20,#ff0000}: |       -       |

=== "Custom"

    !!! note

        You can add the already supported ClickHouse parameters by adding Key Value, the parameters can be [reference document](https://clickhouse.com/docs/en/interfaces/jdbc)

#### Version (Validation)

---

!!! note

    The plugin's e2e test `ClickHouseE2ETest` verifies each version below in a container: the version check, then databases, tables and columns created, read, changed and dropped with SQL.

    - [x] 24.8
    - [x] 25.8
    - [x] 26.3
    - [x] 26.8

!!!

#### SQL and concepts

---

- Connects with the official JDBC driver `clickhouse-jdbc` 0.11.0 over HTTP, default port `8123`.
- The former "ClickHouse HTTP" plugin is merged into this one; existing sources switch to it on upgrade.

| MetaForge | ClickHouse | SQL |
|:--|:--|:--|
| Database | Database | `CREATE DATABASE` / `DROP DATABASE` |
| Table | Table | `CREATE TABLE ... ENGINE = MergeTree ORDER BY ...` / `DROP TABLE` |
| Column | Column | `ALTER TABLE ... ADD / MODIFY / DROP COLUMN` |
