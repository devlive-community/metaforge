---
title: GreptimeDB
status: new
---

#### 什么是 GreptimeDB ?

一个开源、云原生、分布式时间序列数据库，支持PromQL/SQL/Python。

#### 环境

!!! note

    如果你需要使用这个数据源, 您需要将 MetaForge 服务升级到 >= `1.9.x`

!!!

支持时间: `2023-04-14`

#### 配置

---

!!! note

    如果您的 GreptimeDB 服务版本需要其他特殊配置，请参考修改配置文件并重启 MetaForge 服务。

!!!

::: tabs
    === "配置"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |    `4002`     |
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

- 通过 MySQL JDBC 驱动连接 GreptimeDB 的 MySQL 协议端口（默认 `4002`），不再使用 HTTP（端口 `4000`）方式；已有数据源需要把端口改为 `4002`。

#### 版本（验证）

---

!!! note

    以下版本由插件的 e2e 测试 `GreptimeDBE2ETest` 在容器中逐一验证：版本检查，以及用 SQL 创建、读取、修改、删除数据库、表和列。

    - [x] v0.15
    - [x] v0.17

!!!

#### SQL 与概念

---

| MetaForge | GreptimeDB | SQL |
|:--|:--|:--|
| 数据库 | Database | `CREATE DATABASE` / `DROP DATABASE` |
| 表 | Table（必须有一个 `TIME INDEX` 列） | `CREATE TABLE ... (host STRING PRIMARY KEY, ts TIMESTAMP TIME INDEX, ...)` / `DROP TABLE` |
| 列 | Column（Tag / Field / Timestamp） | `ALTER TABLE ... ADD / MODIFY / DROP COLUMN` |
