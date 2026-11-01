<div align="center">

<img width="68" height="68" alt="MetaForge" src="core/metaforge-web/public/static/images/logo.png" />

# MetaForge

**用于数据转换、集成和可视化的集成软件**

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

## 什么是 MetaForge?

---

MetaForge 是用于数据转换、集成和可视化的集成软件。支持多种数据源 —— 关系型数据库、NoSQL 数据库、大数据引擎、消息队列和文件系统 —— 让您在一个控制台中管理多个数据源，查询和转换数据、构建数据集，并创建图表和监控视图。

## 功能特性

---

- **数据源管理** — 在单一控制台中连接、测试和管理 50+ 数据源
- **SQL 查询控制台** — IDE 风格的编辑器，支持语法高亮、格式化、执行历史和导出
- **数据集** — 基于表或临时查询构建可复用的数据集，预览数据并定义列元数据
- **可视化** — 拖拽式仪表盘，支持表格、饼图等多种图表类型
- **报表** — 定时调度并分享报表
- **工作流** — 将数据任务串联为可重复执行的工作流
- **文件存储** — 通过阿里云 OSS、Amazon S3、HDFS、本地存储等读写文件
- **通知** — 钉钉、邮件等渠道的任务与系统事件通知
- **AI 助手** — 通过配置的 AI 提供商与您的数据对话
- **插件架构** — 每个连接器、转换器和存储后端都是可热加载的插件
- **系统管理** — 用户、角色、菜单与运行时配置，支持细粒度权限

## 环境要求

---

| 依赖     | 版本                          |
|:---------|:------------------------------|
| JDK      | 21                            |
| Maven    | 3.9+（已内置 `./mvnw`）       |
| Node.js  | 20                            |
| pnpm     | 9.13.2                        |

## 快速开始

---

> 用户必读：请务必在本地克隆代码后执行以下命令

```bash
cp configure/git-hook/* .git/hooks
chmod 700 .git/hooks/*
```

### 运行后端（开发）

在 IDE 中运行主类 `org.devlive.metaforge.server.MetaForge`（模块 `core/metaforge-server`）。
服务启动后访问 [http://localhost:9096](http://localhost:9096)。首次启动时，安装向导会引导您选择元数据库
（默认内嵌 H2，也可选择 MySQL / PostgreSQL / SQL Server / Oracle）并创建管理员账号。

### 运行前端（开发）

```bash
pnpm install
pnpm --filter metaforge-web dev
```

Web 控制台启动在 [http://localhost:3001](http://localhost:3001)，API 请求会代理到后端。

### 生产构建

```bash
# 前端
pnpm --filter metaforge-web build

# 后端 + 发行包（服务端会直接加载 core/metaforge-web/dist
# 下构建好的前端；压缩包输出到 dist/）
./mvnw -T1C clean package -DskipTests
```

## Docker

---

```bash
docker pull devliveorg/metaforge:latest
docker run -d --name metaforge -p 9096:9096 devliveorg/metaforge:latest
```

或者解压本地构建的发行包（`dist/metaforge-server-2026.0.0-bin.tar.gz`），运行 `bin/startup.sh`。

## 项目结构

---

```
metaforge
├── client                        # 命令行客户端
├── configure                     # 默认配置与插件清单
├── core
│   ├── metaforge-captcha         # 验证码生成
│   ├── metaforge-common          # 通用工具与枚举
│   ├── metaforge-condor          # 调度集成
│   ├── metaforge-server          # Spring Boot 服务端（REST API 入口）
│   ├── metaforge-service         # 领域服务与持久化
│   ├── metaforge-security        # 安全配置
│   ├── metaforge-sql             # SQL 语句构建器
│   ├── metaforge-spi             # 插件 SPI 契约
│   ├── metaforge-web             # Vue 3 + Tailwind Web 控制台
├── convert                       # 数据转换器（JSON、CSV、XML 等）
├── driver                        # 数据库驱动（Redis、MongoDB 等）
├── executor                      # 执行引擎
├── fs                            # 文件存储插件（OSS、S3、本地存储等）
├── lib                           # 共享库（HTTP 客户端、日志、Shell）
├── notify                        # 通知渠道（钉钉等）
├── parser                        # SQL 解析器（MySQL、Trino 等）
├── plugin                        # 数据源插件（50+ 连接器）
└── shaded                        # 第三方集成 Shade 封装
```

## 支持的连接器

---

MetaForge 可以从任何使用 SQL 的数据存储或数据引擎（ClickHouse、MySQL、Presto 等）查询数据。

以下是一些受支持的主要数据库解决方案：

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

## 文档

---

[MetaForge 的扩展文档](https://metaforge.devlive.org)

## 加入我们

---

查看 [Issues 950](https://github.com/devlive-community/metaforge/issues/950)

## 鸣谢

---

[![Jetbrains](https://img.shields.io/badge/Development-Jetbrains-brightgreen?style=flat-square)](https://www.jetbrains.com/)
[![App Store](https://img.shields.io/badge/App%20Store-Rainbond-brightgreen?style=flat-square)](https://www.rainbond.com/)
[![Vue 3](https://img.shields.io/badge/UI-Vue%203%20%2B%20Tailwind-brightgreen?style=flat-square)](https://vuejs.org/)

## Star 历史

---

[![Star History Chart](https://star-history.dera.page/svg?repos=devlive-community/metaforge&type=Timeline)](https://star-history.dera.page/#devlive-community/metaforge&Timeline)

## 贡献者

---

<a href="https://github.com/EdurtIO/metaforge/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=EdurtIO/metaforge" />
</a>
