---
title: Rainbond 部署
---

如果您不熟悉 Kubernetes，想在 Kubernetes 中安装 MetaForge，可以使用 Rainbond 来部署。[Rainbond](https://www.rainbond.com/) 是一个基于 Kubernetes 构建的云原生应用管理平台，可以很简单的将你的应用部署到 Kubernetes 中。

## 前提

安装 Rainbond, 请参阅 [Rainbond 快速安装](https://rainbond.com/docs/quick-start/quick-install).

## 部署 MetaForge

MetaForge 已发布到 Rainbond 开源应用商店，可通过 Rainbond 开源应用商店一键部署 MetaForge。

进入 Rainbond 控制台的 `平台管理 -> 应用市场 -> 开源应用商店` 中搜索 `MetaForge` 并安装。

![](https://static.goodrain.com/wechat/metaforge/1.png)

填写以下信息，然后点击确认按钮进行安装。

- 团队：选择现有团队或创建新的团队
- 集群：选择对应的集群
- 应用：选择现有应用或创建新的应用
- 版本：选择要安装的版本

安装完成后，可通过 Rainbond 提供的默认域名访问 MetaForge，默认用户密码 `admin/12345678`

![](https://static.goodrain.com/wechat/metaforge/topology.png)