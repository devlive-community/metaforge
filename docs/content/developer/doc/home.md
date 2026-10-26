---
title: 文档端
---

本文章主要用来介绍我们如何来贡献 metaforge UI 端源码。

### 准备工作

---

**重要** 克隆源码到本地（如果您需要提交代码到主仓库中需要先将源码 fork 到您的 github 账户中）

```bash
git clone https://github.com/devlive-community/metaforge.git
```

!!! note

    如果您已经 fork 源码到您的账户中，请将 `devlive-community` 替换为您的 github 账户的 ID
!!!

**重要** 以下是基本的环境配置

| 环境                          | 版本   | 必需 |
|:----------------------------|:-----|:---|
| `mkdocs`                    | 1.5+ | 必须 |
| `Python`                    | 3+   | 必须 |
| `IDEA` \| `Eclipse` \| `其他` | 任意版本 | 必须 |

!!! note

    在本文中我们使用的是 `IDEA` 编辑器环境，用户可以根据自己喜好更换相应编辑器。
!!!

可参考 [服务端](../server/home.md) 文档中的 `加载源码到 IDEA` 部分。

### 服务启动

---

- 进入源码目录

```sql
cd docs
```

- 安装依赖

```bash
pip install -r requirements.txt
```

- 启动服务

```bash
mkdocs serve --dev-addr=0.0.0.0:8001
```

返回类似如下信息

![img.png](img.png)

通过浏览器访问 `http://localhost:8001` 调试源代码，看到返回如下页面表示启动成功。

![img_1.png](img_1.png)
