---
title: 插件列表
---

请求地址：`/api/v1/plugin`

请求方式：`GET`

## Response

::: tabs
=== "示例"

    ```json
    [
      {
        "name": "TxtConvert",
        "version": "2024.4.0-SNAPSHOT",
        "state": "CREATED",
        "loadTimestamp": 1732353263492,
        "loadTime": "2024-11-23 17:14:23",
        "type": "CONVERT",
        "loaderName": "org.devlive.metaforge.plugin.loader.PropertiesPluginLoader"
      }
    ]
    ```

=== "参数"

    | 名称 | 类型 | 是否必须 | 描述 |
    | --- | --- | --- | --- |
    | `name` | String | 是 | 插件名称 |
    | `version` | String | 是 | 插件版本 |
    | `state` | String | 是 | 插件状态 |
    | `loadTimestamp` | Long | 是 | 插件加载时间戳 |
    | `loadTime` | String | 是 | 插件加载时间 |
    | `type` | String | 是 | 插件类型 |
    | `loaderName` | String | 是 | 插件加载器名称 |
:::