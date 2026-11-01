<div align="center">

<img width="68" height="68" alt="MetaForge" src="core/metaforge-web/public/static/images/logo.png" />

# MetaForge

**Integrated software for data transformation, integration, and visualization**

---

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![version](https://img.shields.io/github/v/release/EdurtIO/metaforge.svg)

[![Docker Automated build](https://img.shields.io/docker/automated/devliveorg/metaforge)](https://hub.docker.com/r/devliveorg/metaforge)
![Docker Image Size (latest by date)](https://img.shields.io/docker/image-size/devliveorg/metaforge?style=flat-square)
![Docker Pulls](https://img.shields.io/docker/pulls/devliveorg/metaforge?style=flat-square)

![GitHub commit activity](https://img.shields.io/github/commit-activity/y/EdurtIO/metaforge?style=flat-square)
![GitHub contributors](https://img.shields.io/github/contributors-anon/EdurtIO/metaforge?style=flat-square)
![GitHub last commit](https://img.shields.io/github/last-commit/EdurtIO/metaforge?style=flat-square)

</div>

## What is MetaForge?

---

MetaForge is integrated software for data transformation, integration, and visualization. It supports a wide variety of data sources — relational databases, NoSQL databases, big data engines, message queues and file systems — and lets you manage multiple data sources, query and transform data, build datasets, and create charts and monitoring views.

## Features

---

- **Data source management** — connect, test and manage 50+ data sources from a single console
- **SQL query console** — IDE-style editor with syntax highlighting, formatting, execution history and export
- **Datasets** — build reusable datasets from tables or ad-hoc queries, preview data and define column metadata
- **Visualization** — drag-and-drop dashboards with tables, pies and more chart types
- **Reports** — schedule and share reports
- **Workflows** — chain data jobs into repeatable workflows
- **File storage** — read and write files through Aliyun OSS, Amazon S3, HDFS, local storage and more
- **Notifications** — DingTalk, email and other channels for job and system events
- **AI assistant** — chat with your data through configured AI providers
- **Plugin architecture** — every connector, converter and storage backend is a hot-loadable plugin
- **Administration** — users, roles, menus and runtime configuration with fine-grained permissions

## Requirements

---

| Dependency | Version                                      |
|:-----------|:---------------------------------------------|
| JDK        | 21                                           |
| Maven      | 3.9+ (wrapper `./mvnw` included)             |
| Node.js    | 20                                           |
| pnpm       | 9.13.2                                       |

## Getting Started

---

> Must-read for users: be sure to execute the following command after cloning the code locally

```bash
cp configure/git-hook/* .git/hooks
chmod 700 .git/hooks/*
```

### Run the backend (development)

Run the main class `org.devlive.metaforge.server.MetaForge` (module `core/metaforge-server`) from your IDE.
The server starts on [http://localhost:9096](http://localhost:9096). On first launch the setup wizard guides you through
selecting the metadata database (embedded H2 by default, or MySQL / PostgreSQL / SQL Server / Oracle) and creating the
administrator account.

### Run the frontend (development)

```bash
pnpm install
pnpm --filter metaforge-web dev
```

The web console starts on [http://localhost:3001](http://localhost:3001) and proxies API calls to the backend.

### Build for production

```bash
# Frontend
pnpm --filter metaforge-web build

# Backend + distribution package (the server serves the built frontend
# from core/metaforge-web/dist; the tarball lands in dist/)
./mvnw -T1C clean package -DskipTests
```

## Docker

---

```bash
docker pull devliveorg/metaforge:latest
docker run -d --name metaforge -p 9096:9096 devliveorg/metaforge:latest
```

Or unpack the distribution built locally (`dist/metaforge-server-2026.0.0-bin.tar.gz`) and run `bin/startup.sh`.

## Project Structure

---

```
metaforge
├── client                        # Command line client
├── configure                     # Default configuration and plugin manifests
├── core
│   ├── metaforge-captcha         # Captcha generation
│   ├── metaforge-common          # Shared utilities and enums
│   ├── metaforge-condor          # Scheduling integration
│   ├── metaforge-server          # Spring Boot server (REST API entry point)
│   ├── metaforge-service         # Domain services and persistence
│   ├── metaforge-security        # Security configuration
│   ├── metaforge-sql             # SQL statement builders
│   ├── metaforge-spi             # Plugin SPI contracts
│   ├── metaforge-web             # Vue 3 + Tailwind web console
├── convert                       # Data converters (JSON, CSV, XML, ...)
├── driver                        # Database drivers (Redis, MongoDB, ...)
├── executor                      # Execution engines
├── fs                            # File storage plugins (OSS, S3, local, ...)
├── lib                           # Shared libraries (HTTP client, logging, shell)
├── notify                        # Notification channels (DingTalk, ...)
├── parser                        # SQL parsers (MySQL, Trino, ...)
├── plugin                        # Data source plugins (50+ connectors)
├── scheduler                     # Job scheduling
└── shaded                        # Shaded third-party integrations
```

## Supported Connectors

---

MetaForge can query data from any SQL-speaking datastore or data engine (ClickHouse, MySQL, Presto and more).

Here are some of the major database solutions that are supported:

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

## Documentation

---

[Extended documentation for MetaForge](https://metaforge.devlive.org)

## Join Us

---

Please visit [Issues 950](https://github.com/devlive-community/metaforge/issues/950)

## Thank you

---

[![Jetbrains](https://img.shields.io/badge/Development-Jetbrains-brightgreen?style=flat-square)](https://www.jetbrains.com/)
[![App Store](https://img.shields.io/badge/App%20Store-Rainbond-brightgreen?style=flat-square)](https://www.rainbond.com/)
[![Vue 3](https://img.shields.io/badge/UI-Vue%203%20%2B%20Tailwind-brightgreen?style=flat-square)](https://vuejs.org/)

## Stargazers over time

---

[![Star History Chart](https://star-history.dera.page/svg?repos=devlive-community/metaforge&type=Timeline)](https://star-history.dera.page/#devlive-community/metaforge&Timeline)

## Contributors

---

<a href="https://github.com/EdurtIO/metaforge/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=EdurtIO/metaforge" />
</a>
