---
title: Apache Pinot
status: 1.10.0
---

#### 什么是 Pinot ?

Apache Pinot 是一个实时分布式 OLAP 数据存储，专为低延迟、高吞吐量分析而构建，非常适合面向用户的分析工作负载。

#### 环境

---

!!! note

    如果需要使用该数据源，需要将MetaForge服务升级到 >= `1.10.x`

!!!

支持时间: `2023-05-06`

#### 配置

---


!!! note

    如果您的服务版本需要其他特殊配置，请参考修改配置文件并重启MetaForge服务。

!!!

::: tabs
    === "配置"
        | Field  |             Required              | Default Value |
        |:------:|:---------------------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}:  |       \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |     `9000`    |
    === "授权"
        |   Field    |             Required              | Default Value |
        |:----------:|:---------------------------------:|:-------------:|
        | `Username` | :check{20,#3CA34F}:  |       \-       |
        | `Password` | :check{20,#3CA34F}: |       \-       |
:::

#### 版本（验证）

---

!!! note

    插件的 e2e 测试 `PinotE2ETest` 在容器中逐一验证以下版本：batch 快速启动的数据表连同字段被列出，并用 SQL 查询。若 broker 以宿主不可达的地址注册，在自定义参数中加入 `brokers=<host>:<port>` 直连 broker。

    - [x] 1.4
    - [x] 1.5

!!!
