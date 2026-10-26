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
        | `Port` | :check{20,#3CA34F}: |     `9000`    |
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

!!! warning

    在线服务尚未测试，如果您有详细的测试结果，请提交 [issues](https://github.com/EdurtIO/metaforge/issues/new/choose)

    - [x] 19.x
    - [x] 20.x
    - [x] 21.x

!!!