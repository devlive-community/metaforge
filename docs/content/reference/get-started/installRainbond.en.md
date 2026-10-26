---
title: Deploying in Rainbond
---

If you are unfamiliar with Kubernetes, and want to install MetaForge in Kubernetes, you can use Rainbond to deploy. [Rainbond](http://www.rainbond.com/) is a cloud-native application management platform built on Kubernetes and simplifies the application deployment to Kubernetes.

## Prerequisite

To install Rainbond, please refer to [Rainbond Quick Install](https://rainbond.com/docs/quick-start/quick-install).

## Deploy MetaForge

MetaForge has been released to the Rainbond Open Source App Store, MetaForge can be deployed with one click through the Rainbond Open Source App Store.

Go to the Rainbond Console's `Platform Management -> App Marketplace -> Open Source App Store` and search for `metaforge` and install it.

![](https://static.goodrain.com/wechat/metaforge/metaforge-store-en.png)

Fill in the following information, and click `Confirm` button to install.

- Team: select a team or create a new team
- Cluster: select a cluster
- Application: select an application or create a new application
- Version: select a version

After installation, MetaForge can be accessed through the default domain name provided by Rainbond, Default user password `admin/12345678`

![](https://static.goodrain.com/wechat/metaforge/topology-metaforge-en.png)
