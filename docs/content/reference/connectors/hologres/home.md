---
title: Hologres
status: new
---

#### 什么是 Hologres ?

Hologres是兼容PostgreSQL的一站式实时数据仓库引擎，支持PB级数据多维分析（OLAP）与即席分析（Ad Hoc），支持高并发低延迟的在线数据服务（Serving）。与MaxCompute、Flink、DataWorks深度融合，提供离在线一体化全栈数仓解决方案。

#### 环境

!!! note

    如果你需要使用这个数据源, 您需要将 MetaForge 服务升级到 >= `1.9.x`

!!!

支持时间: `2023-04-25`

#### 配置

---

!!! note

    如果您的 Hologres 服务版本需要其他特殊配置，请参考修改配置文件并重启 MetaForge 服务。

!!!

::: tabs
    === "配置"
        | Field  |             Required              | Default Value |
        |:------:|:---------------------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}:  |       \-       |
        | `Host` | :check{20,#3CA34F}: |  `hologres-cn-regison.aliyuncs.com`  |
        | `Port` | :check{20,#3CA34F}: |     `80`    |
    === "授权"
        |   Field    |        Required         | Default Value |
        |:----------:|:-----------------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |       \-       |
        | `Password` | :x{20,#ff0000}: |       \-       |
    === "高级"
        |   Field    |        Required         | Default Value |
        |:----------:|:-----------------------:|:-------------:|
        | `Database` | :x{20,#ff0000}: |       \-       |
:::

#### 版本（验证）

---

!!! warning

    Hologres 仅以阿里云托管服务形式提供，没有容器镜像，因此插件没有容器 e2e 测试，也没有经过验证的版本。插件通过 PostgreSQL JDBC 驱动连接，这条路径由 `PostgreSQLE2ETest` 在 PostgreSQL 上验证。如果您在 Hologres 上测试过，请在 [issues](https://github.com/devlive-community/metaforge/issues/new/choose) 中反馈结果。

!!!
