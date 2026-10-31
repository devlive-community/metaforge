---
title: ScyllaDB
---

#### 介绍

ScyllaDB 是一个分布式 NoSQL 宽列数据库，适用于需要高性能和低延迟的数据密集型应用程序。

#### 驱动

---

MetaForge 只通过 JDBC 访问 ScyllaDB，使用 CQL 的 JDBC 驱动 `com.ing.data.cassandra.jdbc.CassandraDriver`（cassandra-jdbc-wrapper 5.0）。

- URL：`jdbc:cassandra://host:9042/keyspace?localdatacenter=datacenter1`；本地数据中心默认 `datacenter1`，在自定义参数里用 `localdatacenter` 修改。
- 原来「数据库」字段填写的数据中心改为在自定义参数中填写，「数据库」现在是 keyspace。

#### 概念

---

| MetaForge | ScyllaDB | SQL（CQL） |
|:--|:--|:--|
| 数据库 | Keyspace | `CREATE KEYSPACE` / `ALTER KEYSPACE` / `DROP KEYSPACE` |
| 表 | Table | `CREATE TABLE` / `ALTER TABLE` / `DROP TABLE` |
| 列 | Column | `ALTER TABLE ... ADD` / `DROP`（主键列不能修改） |

```sql
CREATE KEYSPACE app WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
CREATE TABLE app.users (id int PRIMARY KEY, name text);
ALTER TABLE app.users ADD age int;
ALTER TABLE app.users DROP age;
INSERT INTO app.users (id, name) VALUES (1, 'ann');
SELECT name FROM app.users WHERE id = 1;
SELECT version AS version FROM system.versions;
```

#### 版本（验证）

---

!!! note

    以下版本由插件的 e2e 测试 `ScyllaDBE2ETest` 在容器中逐一验证：版本检查，以及用 CQL 创建、读取、修改、删除 keyspace、表和列。

    - [x] 2026.1

!!!
