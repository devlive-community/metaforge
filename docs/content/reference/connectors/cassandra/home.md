---
title: Apache Cassandra
status: 1.11.0
---

#### What is Cassandra ?

Apache Cassandra® powers mission-critical deployments with improved performance and unparalleled levels of scale in the cloud.

#### Driver

---

MetaForge reaches Cassandra only through JDBC, with the CQL JDBC driver `com.ing.data.cassandra.jdbc.CassandraDriver` (cassandra-jdbc-wrapper 5.0).

- URL: `jdbc:cassandra://host:9042/keyspace?localdatacenter=datacenter1`; the local data center defaults to `datacenter1` and is set with the custom option `localdatacenter`.
- The data center that the `Database` field held before moves to the custom options; `Database` is now the keyspace.

#### Concepts

---

| MetaForge | Cassandra | SQL (CQL) |
|:--|:--|:--|
| Database | Keyspace | `CREATE KEYSPACE` / `ALTER KEYSPACE` / `DROP KEYSPACE` |
| Table | Table | `CREATE TABLE` / `ALTER TABLE` / `DROP TABLE` |
| Column | Column | `ALTER TABLE ... ADD` / `DROP` (primary key columns can not change) |

```sql
CREATE KEYSPACE app WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
CREATE TABLE app.users (id int PRIMARY KEY, name text);
ALTER TABLE app.users ADD age int;
ALTER TABLE app.users DROP age;
INSERT INTO app.users (id, name) VALUES (1, 'ann');
SELECT name FROM app.users WHERE id = 1;
SELECT release_version AS version FROM system.local;
```

#### Version (Validation)

---

!!! note

    The plugin's e2e test `CassandraE2ETest` verifies each version below in a container: the version check, then keyspaces, tables and columns created, read, changed and dropped with CQL.

    - [x] 4.1
    - [x] 5.0

!!!
