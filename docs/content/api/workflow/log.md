---
title: 工作流日志
---

请求地址：`/api/v1/workflow/log/{code}`

请求方式：`GET`

## Path

::: tabs
=== "示例"

    ```json
    /api/v1/workflow/log/8ee2171b5d014779a45901fb9c2428c9
    ```

=== "参数"

    |参数|类型|描述|
    |---|---|---|
    |`code`|String|仪表盘编码|
:::

## Response

::: tabs
=== "示例"

    ```json
    "2024-12-19 22:19:30,265 INFO [pool-5-thread-1] [ProcessBuilderCommander.java:39] Execute task on username [ admin ]",
    "2024-12-19 22:19:30,349 INFO [pool-5-thread-1] [ProcessBuilderCommander.java:50] Execute command /bin/sh -c null/bin/start-seatunnel-spark-connector-v2.sh --master local --deploy-mode client --config /Users/shicheng/Code/metaforge/data/admin/20241219/workflow/seatunnelexecutor/c1ff398243994338a79730f8a1d4f55f/c1ff398243994338a79730f8a1d4f55f.configure --name c1ff398243994338a79730f8a1d4f55f",
    ```

=== "参数"

    工作流的日志信息。
:::