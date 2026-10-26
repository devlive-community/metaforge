---
title: 主页
icon: home
template: home

config:
  toc: false
  sidebar: false

hero:
  title: 开源数据中台 MetaForge
  description: MetaForge 是用于数据转换、集成和可视化的集成软件。支持多种数据源、文件类型、大数据相关数据库、关系数据库、NoSQL数据库等。通过软件可以实现多数据源的管理，对源下的数据进行各种操作转换、制作数据图表、监控数据源和其他功能。
  primaryCta:
    url: /<%= pageData.language %>/download.html
    text: 下载使用
  secondaryCta:
    url: /<%= pageData.language %>/reference/get-started/install.html
    text: 了解更多

features:
  subtitle: 核心优势
  title: 为什么选择我们
  description: 提供全方位的技术支持和服务
  items:
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
        </svg>
      title: 统一查询语言
      description: MetaForge 将所有数据源的查询语言统一为 SQL，无论是关系型数据库、NoSQL、文件系统还是其他中间件，都可以使用 SQL 进行操作。
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
      title: 广泛的数据源支持
      description: MetaForge 支持超过 40+ 数据源，包括 ClickHouse、MySQL、PostgreSQL、MongoDB、Redis、Elasticsearch、Kafka 等主流数据库和中间件。
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
        </svg>
      title: 灵活的连接方式
      description: MetaForge 系统支持通过 JDBC、Native、HTTP 等多种协议连接到不同的数据源，提供了更大的灵活性和兼容性。
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 10l-2 1m0 0l-2-1m2 1v2.5M20 7l-2 1m2-1l-2-1m2 1v2.5M14 4l-2-1-2 1M4 7l2-1M4 7l2 1M4 7v2.5M12 21l-2-1m2 1l2-1m-2 1v-2.5M6 18l-2-1v-2.5M18 18l2-1v-2.5" />
        </svg>
      title: 插件化架构设计
      description: MetaForge 采用插件化系统设计，支持在线安装、卸载、更新和热部署，方便系统的扩展和维护。
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
        </svg>
      title: 完整的 SQL 解析能力
      description: MetaForge 内置完整的 SQL 解析器，确保对 SQL 查询的准确解析和执行。
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      title: 数据可视化功能
      description: MetaForge 提供了数据可视化的功能，可以将数据转换为图表、图形和报表，实现数据的可视化展示，帮助用户更直观地理解和分析数据。
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22" />
        </svg>
      title: 数据源监控
      description: MetaForge 提供了数据源监控的功能，可以实时监控数据源的状态、性能和健康指标，帮助用户了解数据源的运行情况，提供数据源的可靠性保障。
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
        </svg>
      title: 数据转换与集成
      description: MetaForge 支持数据转换和集成，可以将不同数据源的数据转换为统一的格式，并进行数据集成，实现数据的统一化管理和分析，方便用户进行数据迁移和整合。
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 14v3m4-3v3m4-3v3M3 21h18M3 10h18M3 7l9-4 9 4M4 10h16v11H4V10z" />
        </svg>
      title: 开源透明
      description: MetaForge 是开源的，用户可以自由查看、修改和使用源代码，确保系统的透明度和可定制性。
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
      title: 多用户支持
      description: MetaForge 内置多用户管理系统，支持不同用户权限的配置和管理。
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z" />
        </svg>
      title: 完整的 Web UI
      description: MetaForge 内置完整的 Web UI，提供了用户友好的界面，方便用户进行数据查询、管理和可视化，实现数据管理的全面控制。

    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8h2a2 2 0 012 2v6a2 2 0 01-2 2h-2v4l-4-4H9a1.994 1.994 0 01-1.414-.586m0 0L11 14h4a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2v4l.586-.586z" />
        </svg>
      title: 加入 (钉钉 ｜ 微信)
      description: >-
        https://github.com/devlive-community/metaforge/issues/950

stats:
  title: 用数据说话
  description: 我们取得的成就
  items:
    - label: GitHub Stars
      value: 900+
    - label: 服务客户
      value: 100+
    - label: 支持数据源
      value: 60+
    - label: Gitee Stars
      value: 800+

cta:
  title: 准备好开始了吗？
  description: 立即加入我们，开启您的技术创新之旅
  button:
    url: /<%= pageData.language %>/reference/get-started/install.html
    text: 立即使用
---