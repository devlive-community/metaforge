<div align="center">

<img width="68" height="68" alt="MetaForge" src="core/metaforge-web/public/static/images/logo.png" />

# MetaForge

---

![Visitors](https://api.visitorbadge.io/api/visitors?path=https%3A%2F%2Fgithub.com%2FEdurtIO%2Fmetaforge.git&countColor=%23263759&style=flat&labelStyle=none)
[![](https://tokei.rs/b1/github/EdurtIO/metaforge)](https://github.com/EdurtIO/metaforge)
![version](https://img.shields.io/github/v/release/EdurtIO/metaforge.svg)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![GitHub Release Date](https://img.shields.io/github/release-date/EdurtIO/metaforge?style=flat-square)

![Docker Automated build](https://img.shields.io/docker/automated/devliveorg/metaforge)
![Docker Image Size (latest by date)](https://img.shields.io/docker/image-size/devliveorg/metaforge?style=flat-square)
![Docker Pulls](https://img.shields.io/docker/pulls/devliveorg/metaforge?style=flat-square)

![GitHub commit activity](https://img.shields.io/github/commit-activity/y/EdurtIO/metaforge?style=flat-square)
![GitHub contributors](https://img.shields.io/github/contributors-anon/EdurtIO/metaforge?style=flat-square)
![GitHub last commit](https://img.shields.io/github/last-commit/EdurtIO/metaforge?style=flat-square)

</div>

### Что такое MetaForge?

---

MetaForge — это интегрированное программное обеспечение для преобразования, интеграции и визуализации данных. Поддержка нескольких источников данных, типов файлов, баз данных, связанных с большими данными, реляционных баз данных, баз данных NoSQL и многого другого. С помощью программного обеспечения может быть реализовано управление несколькими источниками данных, а также могут выполняться различные операции с данными в источнике, могут быть построены диаграммы данных, могут быть обнаружены источники данных и могут быть выполнены другие функции.

| Имя пользователя | пароль      |
|:----------------:|:------------|
|    `metaforge`     | `123456789` |
|     `admin`      | `12345678`  |

### просьба

---

> Обязательный к прочтению пользователь: обязательно клонируйте код локально и выполните следующую команду

```bash
cp configure/git-hook/* .git/hooks
chmod 700 .git/hooks/*
```

## Поддерживаемые коннекторы

---

MetaForge может запрашивать данные из любого хранилища данных или движка данных (ClickHouse, MySQL, Presto и т.д.), использующего SQL.

Ниже приведены некоторые из основных поддерживаемых решений для баз данных.

<div style="display: flex; flex-wrap: wrap; gap: 20px; justify-content: center;">
 <a href="https://clickhouse.com"><img src="docs/assets/plugin/clickhouse.svg" alt="ClickHouse" height="60"/></a>
 <a href="https://redis.io/"><img src="docs/assets/plugin/redis.svg" alt="Redis" height="60"/></a>
 <a href="https://www.postgresql.org/"><img src="docs/assets/plugin/postgresql.svg" alt="PostgreSQL" height="60"/></a>
 <a href="https://prestodb.io/"><img src="docs/assets/plugin/presto.svg" alt="Presto" height="60"/></a>
 <a href="https://www.mysql.com"><img src="docs/assets/plugin/mysql.svg" alt="MySQL" height="60"/></a>
 <a href="https://hive.apache.org/"><img src="docs/assets/plugin/hive2x.svg" alt="Hive" height="60"/></a>
 <a href="https://kyuubi.apache.org/"><img src="docs/assets/plugin/kyuubi.svg" alt="Kyuubi" height="60"/></a>
 <a href="https://druid.apache.org/"><img src="docs/assets/plugin/druid.svg" alt="Druid" height="60"/></a>
 <a href="https://www.elastic.co/"><img src="docs/assets/plugin/elasticsearch8x.svg" alt="ElasticSearch" height="60"/></a>
 <a href="https://trino.io/"><img src="docs/assets/plugin/trino.svg" alt="Trino" height="60"/></a>
 <a href="https://kylin.apache.org"><img src="docs/assets/plugin/kylin.svg" alt="Kylin" height="60"/></a>
 <a href="https://ignite.apache.org/"><img src="docs/assets/plugin/ignite.svg" alt="Ignite" height="60"/></a>
 <a href="https://www.ibm.com/db2/"><img src="docs/assets/plugin/db2.svg" alt="IBM DB2" height="60"/></a>
 <a href="https://www.mongodb.com/"><img src="docs/assets/plugin/mongocommunity.svg" alt="MongoDB" height="60"/></a>
 <a href="https://www.dremio.com/"><img src="docs/assets/plugin/dremio.svg" alt="Dremio" height="60"/></a>
 <a href="https://www.monetdb.org/"><img src="docs/assets/plugin/monetdb.svg" alt="MonetDB" height="60"/></a>
 <a href="https://phoenix.apache.org/"><img src="docs/assets/plugin/phoenix.svg" alt="Phoenix" height="60"/></a>
 <a href="https://www.h2database.com/"><img src="docs/assets/plugin/h2.svg" alt="H2" height="60"/></a>
 <a href="https://www.microsoft.com/sql-server"><img src="docs/assets/plugin/sqlserver.svg" alt="SqlServer" height="60"/></a>
 <a href="https://www.oracle.com/"><img src="docs/assets/plugin/oracle.svg" alt="Oracle" height="60"/></a>
 <a href="https://crate.io/"><img src="docs/assets/plugin/cratedb.svg" alt="CrateDB" height="60"/></a>
 <a href="https://www.dameng.com/"><img src="docs/assets/plugin/dm.svg" alt="DaMeng" height="60"/></a>
 <a href="https://tdengine.com/"><img src="docs/assets/plugin/tdengine.svg" alt="TDengine" height="60"/></a>
 <a href="https://impala.apache.org/"><img src="docs/assets/plugin/impala.svg" alt="Impala" height="60"/></a>
 <a href="https://www.oceanbase.com/"><img src="docs/assets/plugin/oceanbase.svg" alt="OceanBase" height="60"/></a>
 <a href="https://neo4j.com/"><img src="docs/assets/plugin/neo4j.svg" alt="Neo4j" height="60"/></a>
 <a href="https://iotdb.apache.org/"><img src="docs/assets/plugin/iotdb.svg" alt="IoTDB" height="60"/></a>
 <a href="https://www.snowflake.com/"><img src="docs/assets/plugin/snowflake.svg" alt="Snowflake" height="60"/></a>
 <a href="https://ydb.tech/"><img src="docs/assets/plugin/ydb.svg" alt="YDB" height="60"/></a>
 <a href="https://zookeeper.apache.org/"><img src="docs/assets/plugin/zookeeper.svg" alt="Zookeeper" height="60"/></a>
 <a href="https://duckdb.org/"><img src="docs/assets/plugin/duckdb.svg" alt="DuckDB" height="60"/></a>
 <a href="https://www.alibabacloud.com/product/oss"><img src="docs/assets/plugin/alioss.svg" alt="Aliyun OSS" height="60"/></a>
 <a href="https://kafka.apache.org"><img src="docs/assets/plugin/kafka.svg" alt="Apache Kafka" height="60"/></a>
 <a href="https://ceresdb.io/"><img src="docs/assets/plugin/ceresdb.svg" alt="CeresDB" height="60"/></a>
 <a href="https://greptime.com/"><img src="docs/assets/plugin/greptimedb.svg" alt="GreptimeDB" height="60"/></a>
 <a href="https://questdb.io/"><img src="docs/assets/plugin/questdb.svg" alt="QuestDB" height="60"/></a>
 <a href="https://doris.apache.org/"><img src="docs/assets/plugin/doris.svg" alt="Apache Doris" height="60"/></a>
 <a href="https://www.starrocks.io/"><img src="docs/assets/plugin/starrocks.svg" alt="StarRocks" height="60"/></a>
 <a href="https://www.alibabacloud.com/product/hologres"><img src="docs/assets/plugin/hologres.svg" alt="Hologres" height="60"/></a>
 <a href="https://hadoop.apache.org/"><img src="docs/assets/plugin/hdfs.svg" alt="Apache Hdfs" height="60"/></a>
 <a href="https://pinot.apache.org/"><img src="docs/assets/plugin/pinot.svg" alt="Apache Pinot" height="60"/></a>
 <a href="https://cassandra.apache.org/"><img src="docs/assets/plugin/cassandra.svg" alt="Apache Cassandra" height="60"/></a>
 <a href="https://matrixorigin.cn/"><img src="docs/assets/plugin/matrixone.svg" alt="MatrixOne" height="60"/></a>
 <a href="https://www.scylladb.com/"><img src="docs/assets/plugin/scylladb.svg" alt="ScyllaDB" height="60"/></a>
 <a href="https://www.paradedb.com/"><img src="docs/assets/plugin/paradedb.svg" alt="ParadeDB" height="60"/></a>
 <a href="https://www.timescale.com/"><img src="docs/assets/plugin/timescale.svg" alt="Timescale" height="60"/></a>
 <a href="https://solr.apache.org/"><img src="docs/assets/plugin/solr.svg" alt="Apache Solr" height="60"/></a>
 <a href="https://www.influxdata.com/"><img src="docs/assets/plugin/influxdb.svg" alt="InfluxDB" height="60"/></a>
 <a href="https://dolphindb.com/"><img src="docs/assets/plugin/dolphindb.svg" alt="DolphinDB" height="60"/></a>
</div>

## Архитектура системы

---

<img src="docs/assets/architecture.jpg" width="100%" />

## Атлас вкладов

---

[![Star History Chart](https://star-history.dera.page/svg?repos=devlive-community/metaforge&type=Timeline)](https://star-history.dera.page/#devlive-community/metaforge&Timeline)

## Присоединяйтесь к нам

---

Вид [Issues 950](https://github.com/devlive-community/metaforge/issues/950)

## Признание

---

[![Jetbrains](https://img.shields.io/badge/Development-Jetbrains-brightgreen?style=flat-square)](https://www.jetbrains.com/)
[![App Store](https://img.shields.io/badge/App%20Store-Rainbond-brightgreen?style=flat-square)](https://www.rainbond.com/)
[![Vue 3](https://img.shields.io/badge/UI-Vue%203%20%2B%20Tailwind-brightgreen?style=flat-square)](https://vuejs.org/)

## Установка и настройка

---

[Расширенная документация по MetaForge](https://metaforge.devlive.org)

## Участников

---

<a href="https://github.com/EdurtIO/metaforge/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=EdurtIO/metaforge" />
</a>
