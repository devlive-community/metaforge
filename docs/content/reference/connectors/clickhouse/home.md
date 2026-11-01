---
title: ClickHouse
---

#### 什么是 ClickHouse ?

ClickHouse® 是一个面向列的数据库管理系统（DBMS），用于在线分析查询处理（OLAP）。ClickHouse 的性能超过了所有其他面向列的数据库管理系统。它每台服务器每秒处理数十亿行和数十 GB 的数据。

#### 环境

!!! note

    如果需要使用此数据源，则需要将 MetaForge 服务升级到 >= `1.0.x`

!!!

支持时间: `2022-09-22`

#### 配置

---

!!! note

    如果您的 ClickHouse 服务版本需要其他特殊配置，请参考修改配置文件并重启 MetaForge 服务。

!!!

::: tabs
    === "配置"
        | Field  |             Required              | Default Value |
        |:------:|:---------------------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}:  |       \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |     `8123`    |
    === "授权"
        |   Field    |        Required         | Default Value |
        |:----------:|:-----------------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |       \-       |
        | `Password` | :x{20,#ff0000}: |       \-       |
    === "高级"
        |   Field    |        Required         | Default Value |
        |:----------:|:-----------------------:|:-------------:|
        | `Database` | :x{20,#ff0000}: |       \-       |
    === "自定义"
        !!! note
    
            您可以通过添加 Key Value 来添加已经支持的 ClickHouse 参数，参数可以是 [参考文档](https://clickhouse.com/docs/en/interfaces/jdbc)
    
        !!!
:::
#### 版本（验证）

---

!!! note

    以下版本由插件的 e2e 测试 `ClickHouseE2ETest` 在容器中逐一验证：版本检查，以及用 SQL 创建、读取、修改、删除数据库、表和列。

    - [x] 24.8
    - [x] 25.8
    - [x] 26.3
    - [x] 26.8

!!!

#### SQL 与概念

---

- 通过官方 JDBC 驱动 `clickhouse-jdbc` 0.11.0 的 HTTP 接口连接，默认端口 `8123`。
- 原「ClickHouse HTTP」插件已合并到本插件，已有数据源升级后自动切换到本插件。

| MetaForge | ClickHouse | SQL |
|:--|:--|:--|
| 数据库 | Database | `CREATE DATABASE` / `DROP DATABASE` |
| 表 | Table | `CREATE TABLE ... ENGINE = MergeTree ORDER BY ...` / `DROP TABLE` |
| 列 | Column | `ALTER TABLE ... ADD / MODIFY / DROP COLUMN` |
