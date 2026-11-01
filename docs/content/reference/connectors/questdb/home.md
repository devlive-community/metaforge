---
title: QuestDB
status: new
---

#### 什么是 QuestDB ?

QuestDB 是一个开源时间序列数据库，用于高吞吐量摄取和快速 SQL 查询，操作简单。它支持使用 InfluxDB 行协议、PostgreSQL 有线协议和用于批量导入和导出的 REST API 进行与模式无关的摄取。

#### 环境

!!! note

    如果你需要使用这个数据源, 您需要将 MetaForge 服务升级到 >= `1.9.x`

!!!

支持时间: `2023-04-17`

#### 配置

---

!!! note

    如果您的 QuestDB 服务版本需要其他特殊配置，请参考修改配置文件并重启 MetaForge 服务。

!!!

::: tabs
    === "配置"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |    `8812`     |
    === "授权"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |   `admin`     |
        | `Password` | :x{20,#ff0000}: |      \-       |
    === "高级"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Database` | :x{20,#ff0000}: |    `qdb`      |
:::

- 通过 PostgreSQL JDBC 驱动连接 QuestDB 的 PostgreSQL 协议端口（默认 `8812`，默认账号 `admin` / `quest`），不再使用 HTTP（端口 `9000`）方式；已有数据源需要把端口改为 `8812` 并填写账号。

#### 版本（验证）

---

!!! note

    以下版本由插件的 e2e 测试 `QuestDBE2ETest` 在容器中逐一验证：版本检查，以及用 SQL 创建、读取、修改、删除表和列。

    - [x] 8.3
    - [x] 9.1

!!!

#### SQL 与概念

---

| MetaForge | QuestDB | SQL |
|:--|:--|:--|
| 数据库 | 服务器唯一的数据库 `qdb` | 不能创建或删除 |
| 表 | Table | `CREATE TABLE ... TIMESTAMP(ts) PARTITION BY DAY` / `DROP TABLE`，表列表来自 `tables()` |
| 列 | Column | `ALTER TABLE ... ADD COLUMN` / `RENAME COLUMN` / `DROP COLUMN`，列结构来自 `table_columns()` |

- WAL 表（分区表默认）的结构变更在后台应用，执行后稍等片刻再刷新即可看到。
