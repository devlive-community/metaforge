---
title: Apache Kafka
---

#### What is Apache Kafka ?

Apache Kafka is an open-source distributed event streaming platform used by thousands of companies for high-performance data pipelines, streaming analytics, data integration, and mission-critical applications.

#### Driver

---

MetaForge reaches Kafka only through JDBC, with its own driver `org.devlive.metaforge.KafkaDriver` (module `metaforge-driver-kafka`, built on `metaforge-parser` and kafka-clients 3.9).

- URL: `jdbc:kafka://broker:9092[,broker:9092...][?timeout=10000&maxRows=10000&security.protocol=...]`
- The `Host` field lists the bootstrap servers. Options other than `timeout` (ms) and `maxRows` (records read per query, default 10000) are passed to the Kafka clients, for example SASL / TLS settings.

#### Concepts

---

| MetaForge | Kafka |
|:--|:--|
| Database | The cluster, always named `kafka` |
| Table | A topic |
| Column | Fixed: `partition` (INTEGER), `offset` (BIGINT), `timestamp` (TIMESTAMP), `key` (VARCHAR), `value` (VARCHAR) |
| Row | A record; keys and values are shown as UTF-8 text |

The column names are SQL keywords, so quote them with backticks: `` `key` ``, `` `offset` ``.

#### Supported SQL

---

```sql
SHOW DATABASES;                                  -- kafka
SHOW TABLES FROM kafka;                          -- topics
SHOW COLUMNS FROM events FROM kafka;
SELECT version();                                -- the protocol version the cluster speaks

CREATE TABLE kafka.events (`key` VARCHAR, `value` VARCHAR)
    WITH ('partitions' = '3', 'replication.factor' = '1', 'retention.ms' = '604800000');
DROP TABLE kafka.events;                         -- deletes the topic

INSERT INTO kafka.events (`key`, `value`) VALUES ('k1', 'hello'), ('k2', 'world');
INSERT INTO kafka.events (`key`, `value`, `partition`, `timestamp`) VALUES ('k3', 'again', 1, '2026-10-31 12:00:00');

SELECT `partition`, `offset`, `key`, `value` FROM kafka.events WHERE `key` = 'k1';
SELECT * FROM kafka.events ORDER BY `timestamp` DESC LIMIT 100;
```

- `CREATE TABLE` options other than `partitions` and `replication.factor` become topic configs; the columns of a topic are fixed, so `ALTER TABLE` is refused, and `CREATE DATABASE` / `DROP DATABASE` too.
- A query reads the records present when it starts, every partition from its beginning, up to `maxRows`; `WHERE`, `ORDER BY` and `LIMIT` apply to what was read.
- `UPDATE` and `DELETE` are refused: records can not be changed.

#### Version (Validation)

---

!!! note

    The plugin's e2e test `KafkaE2ETest` verifies each version below in a container: the version check, then topics created, written, read and dropped as tables with SQL.

    - [x] 3.8
    - [x] 3.9
    - [x] 4.0

!!!
