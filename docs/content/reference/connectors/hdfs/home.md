---
title: Apache Hadoop HDFS
---

#### What is HDFS ?

The Hadoop Distributed File System (HDFS) is a distributed file system designed to run on commodity hardware. It has many similarities with existing distributed file systems.

#### Driver

---

MetaForge reaches HDFS only through JDBC, with its own driver `org.devlive.metaforge.HdfsDriver` (module `metaforge-driver-hdfs`, built on `metaforge-parser` and the Hadoop 3.4 client).

- URL: `jdbc:hdfs://namenode:8020[?root=/&user=hadoop&httpPort=9870&dfs.nameservices=...]`
- `Host` / `Port` are the NameNode's RPC address; `Username` is the HDFS user. Custom options are Hadoop configuration (for HA or Kerberos settings); `root` (default `/`) is where databases live and `httpPort` (default `9870`) the NameNode web port used for its version.
- The uploaded `core-site.xml` / `hdfs-site.xml` of earlier versions are replaced by custom options.

#### Concepts

---

| MetaForge | HDFS | Example |
|:--|:--|:--|
| Database | A directory under `root` | `/warehouse` |
| Table | A directory inside the database | `/warehouse/logs` |
| Row | A file or directory inside the table | `/warehouse/logs/a.log` |
| Column | Fixed: `name`, `type` (file / directory), `size`, `replication`, `block_size`, `owner`, `group`, `permission`, `modified`, `accessed` | |

`group` is an SQL keyword: write `` `group` ``.

#### Supported SQL

---

```sql
SHOW DATABASES;  SHOW TABLES FROM warehouse;  SHOW COLUMNS FROM logs FROM warehouse;
SELECT version();                                            -- the NameNode's Hadoop version

CREATE DATABASE warehouse;                                   -- mkdir /warehouse
DROP DATABASE warehouse;                                     -- rm -r /warehouse
CREATE TABLE warehouse.logs (name VARCHAR);                  -- mkdir /warehouse/logs (columns are fixed)
DROP TABLE warehouse.logs;                                   -- rm -r /warehouse/logs

INSERT INTO warehouse.logs (name, type) VALUES ('a.log', 'file'), ('archive', 'directory');   -- empty file, directory
UPDATE warehouse.logs SET name = 'b.log' WHERE name = 'a.log';                                 -- rename
UPDATE warehouse.logs SET permission = '640', owner = 'etl', `group` = 'hadoop' WHERE name = 'b.log';
DELETE FROM warehouse.logs WHERE type = 'directory';                                           -- rm -r
SELECT name, size, modified FROM warehouse.logs WHERE size > 0 ORDER BY modified DESC LIMIT 20;
```

Only NameNode operations are used, so file contents are neither read nor written; `ALTER TABLE` is refused because the columns are fixed.

#### Version (Validation)

---

!!! note

    The plugin's e2e test `HdfsE2ETest` verifies each version below in a container: the version check, then directories created, listed, changed and dropped as databases, tables and rows with SQL.

    - [x] 3.3
    - [x] 3.4

!!!
