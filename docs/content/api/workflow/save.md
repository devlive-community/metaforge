---
title: 提交工作流
---

请求地址：`/api/v1/workflow`

请求方式：`POST`

## Body

::: tabs
=== "示例"
    
    ```json
    {
      "executor": "MetaForge",
      "name": "dsf",
      "configure": {
        "nodes": [
          {
            "id": "52c6222f-e49a-4afc-9201-35e296201a33",
            "tid": "jdbc-source",
            "category": "source",
            "position": {
              "x": 137,
              "y": 95
            },
            "key": "Jdbc",
            "data": { ... }
          }
        ],
        "connections": [
          {
            "id": "668a7830-5c48-433e-9688-c6f8c9341ff7",
            "source": "52c6222f-e49a-4afc-9201-35e296201a33-output",
            "target": "f2f8b63e-902a-4467-acdf-32c0a7ed4074-input"
          }
        ]
      }
    }
    ```

=== "参数"

    | 参数          |类型| 描述                                                       |
    |-------------|---|----------------------------------------------------------|
    | `name`      |String| 工作流名称                                                    |
    | `configure` |String| 工作流配置                                                    |
    | `executor`  |String| 执行器                                                      |

    > configure 参数请参考组件：https://view-shadcn-ui.devlive.org/components/workflow.html#workflow-props
:::

## Response

::: tabs
=== "示例"

    ```json
    {
      "name": "sss",
      "code": "c1ff398243994338a79730f8a1d4f55f",
      "active": true,
      "createTime": "2024-12-19 22:19:30",
      "updateTime": "2024-12-19 22:19:31",
      "state": "FAILURE",
      "message": "/bin/sh: null/bin/start-seatunnel-spark-connector-v2.sh: No such file or directory",
      "work": "...",
      "elapsed": 492,
      "executor": "SeatunnelExecutor",
      "configure": { ... },
      "user": { ... }
    }
    ```
:::