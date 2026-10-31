---
title: CrateDB
---

<img src="/assets/plugin/cratedb.svg" class="connector-content-logo" />

#### 什么是 CrateDB ?

CrateDB 是面向实时分析的分布式 SQL 数据库，兼容 PostgreSQL 协议。

#### 配置

---

::: tabs
    === "配置"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |    `5432`     |
    === "授权"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |   `crate`     |
        | `Password` | :x{20,#ff0000}: |      \-       |
    === "高级"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Database` | :x{20,#ff0000}: |      \-       |
:::

- 通过 CrateDB JDBC 驱动（PostgreSQL 协议，默认端口 `5432`）连接，不再提供 HTTP（端口 `4200`）方式。
- 原「CrateDB HTTP」插件已合并到本插件：已有数据源升级后自动切换到本插件，原来填写 `4200` 端口的需要改为 `5432`。

#### 版本（验证）

---

!!! note

    以下版本由插件的 e2e 测试 `CrateDBE2ETest` 在容器中逐一验证：版本检查，以及用 SQL 创建、读取、修改、删除 Schema、表和列。

    - [x] 5.10
    - [x] 6.1

!!!

#### SQL 与概念

---

| MetaForge | CrateDB | SQL |
|:--|:--|:--|
| 数据库 | Schema | 随第一张表创建（`CREATE TABLE schema.table ...`），随最后一张表删除而消失；CrateDB 没有 `CREATE SCHEMA` |
| 表 | Table | `CREATE TABLE` / `DROP TABLE`，写入后用 `REFRESH TABLE` 让查询立即可见 |
| 列 | Column | `ALTER TABLE ... ADD COLUMN` / `RENAME COLUMN` / `DROP COLUMN`（列类型不能修改） |
