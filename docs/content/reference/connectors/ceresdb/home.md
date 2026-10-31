---
title: CeresDB
status: new
---

#### 什么是 CeresDB ?

CeresDB 是一款高性能、分布式的云原生时序数据库。

#### 环境

!!! note

    如果你需要使用这个数据源, 您需要将 MetaForge 服务升级到 >= `1.9.x`

!!!

支持时间: `2023-04-12`

#### 配置

---

!!! note

    如果您的CeresDB服务版本需要其他特殊配置，请参考修改配置文件并重启 MetaForge 服务。

!!!

::: tabs
    === "配置"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |    `5433`     |
    === "授权"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |      \-       |
        | `Password` | :x{20,#ff0000}: |      \-       |
    === "高级"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Database` | :x{20,#ff0000}: |   `public`    |
:::

- 通过 PostgreSQL JDBC 驱动连接 CeresDB（HoraeDB）的 PostgreSQL 协议端口（默认 `5433`），自动使用简单查询协议（`preferQueryMode=simple`），不再使用 HTTP（端口 `5440`）方式；已有数据源需要把端口改为 `5433`。

#### 版本（验证）

---

!!! note

    以下版本由插件的 e2e 测试 `CeresDBE2ETest` 在容器中逐一验证：连接检查，以及用 SQL 创建、读取、删除表和新增列。

    - [x] CeresDB 1.2
    - [x] HoraeDB 2.1

!!!

#### SQL 与概念

---

| MetaForge | CeresDB | SQL |
|:--|:--|:--|
| 数据库 | 唯一的 Schema `public` | 不能创建或删除 |
| 表 | Table（必须有 `TIMESTAMP KEY`） | `CREATE TABLE ... (name string TAG, ts timestamp NOT NULL, TIMESTAMP KEY(ts)) ENGINE=Analytic` / `DROP TABLE`，表列表来自 `SHOW TABLES` |
| 列 | Column（Tag / Field） | 只支持 `ALTER TABLE ... ADD COLUMN`，CeresDB 不支持修改或删除列；列结构来自 `DESCRIBE` |

- 表默认保留 7 天（TTL），写入更早时间戳的数据会立即过期。
