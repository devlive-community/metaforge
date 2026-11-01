---
title: Apache Zookeeper
---

#### What is Zookeeper ?

ZooKeeper is a centralized service for maintaining configuration information, naming, providing distributed synchronization, and providing group services. All of these kinds of services are used in some form or another by distributed applications. Each time they are implemented there is a lot of work that goes into fixing the bugs and race conditions that are inevitable. 

#### Driver

---

MetaForge reaches ZooKeeper only through JDBC, with its own driver `org.devlive.metaforge.ZookeeperDriver` (module `metaforge-driver-zookeeper`, built on `metaforge-parser`).

- URL: `jdbc:zookeeper://host:port[,host:port...][/database][?sessionTimeout=30000&connectTimeout=10000]`
- Username / password become ZooKeeper digest authentication.
- The `Host` field may list a whole ensemble, such as `zk1:2181,zk2:2181,zk3:2181`.

#### Concepts

---

| MetaForge | ZooKeeper | Example |
|:--|:--|:--|
| Database | A znode under the root (`/zookeeper` is hidden) | `/app` |
| Table | A child of that znode | `/app/config` |
| Column | A child of the table znode; its data is the value | `/app/config/host` = `db.local` |
| Row | A table has one row: the values of its columns | |

Values are UTF-8 text whatever type a column is created with; znodes deeper than a column are not shown, and are removed with their column.

#### Supported SQL

---

```sql
SHOW DATABASES;
SHOW TABLES FROM app;
SHOW COLUMNS FROM config FROM app;
USE app;
SELECT version();

CREATE DATABASE app;                                   -- creates /app
DROP DATABASE app;                                     -- removes /app and everything under it

CREATE TABLE app.config (host VARCHAR, port VARCHAR);  -- creates /app/config, /app/config/host, /app/config/port
DROP TABLE app.config;

ALTER TABLE app.config ADD COLUMN timeout VARCHAR;     -- creates /app/config/timeout
ALTER TABLE app.config RENAME COLUMN host TO address;  -- moves the value in one transaction
ALTER TABLE app.config MODIFY COLUMN port INT;         -- accepted; values stay text
ALTER TABLE app.config DROP COLUMN timeout;

INSERT INTO app.config (host, port) VALUES ('db.local', '3306');   -- writes the values (one row)
UPDATE app.config SET port = '3307' WHERE host = 'db.local';
DELETE FROM app.config;                                -- clears every value, keeps the columns
SELECT host, port FROM app.config WHERE port LIKE '33%';
```

WHERE supports comparisons, `LIKE`, `IN`, `IS [NOT] NULL`, `NOT`, `AND` and `OR`; `ORDER BY` and `LIMIT ... OFFSET ...` are supported. Joins, `GROUP BY` and functions other than `version()` are refused.

#### Version (Validation)

---

!!! note

    The plugin's e2e test `ZookeeperE2ETest` verifies each version below in a container: the version check, then znodes created, read, changed and dropped as databases, tables, columns and values with SQL.

    - [x] 3.8
    - [x] 3.9

!!!
