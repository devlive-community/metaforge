---
title: 命令行界面
---

MetaForge CLI 提供基于终端的交互式 shell 来运行查询。 CLI 是一个自动执行的 JAR 文件，这意味着它的行为就像普通的 UNIX 可执行文件一样。

### 要求

---

CLI 需要路径上有可用的 Java 虚拟机。它可以与 Java 版本 8 及更高版本一起使用。

CLI 使用基于 HTTP/HTTPS 的 MetaForge 客户端 REST API 与系统进行通信。

CLI 版本应与系统版本相同或更高。

### 安装

---

下载 [metaforge-client-cli-1.6.0.jar](https://repo1.maven.org/maven2/org/devlive/metaforge/metaforge-client-cli/1.6.0/metaforge-client-cli-1.6.0.jar), 将其重命名为 metaforge，使用 `chmod +x` 命令将其设置为可执行。

### 运行 CLI

---

```bash
./metaforge

connect -h 127.0.0.1 -p 9096 -u username -P password
```

如果成功，您将收到执行命令的提示。使用 `help` 命令查看支持的命令列表。

| 命令                            | 描述                  |
|-------------------------------|---------------------|
| `source info`                 | 获取数据源详细信息           |
| `source list`                 | 获取远程服务器数据源列表        |
| `source use <SourceID>`       | 设置数据源标志，以便后续对数据源的操作 |
| `source execute "<QuerySQL>"` | 执行远程SQL             |
