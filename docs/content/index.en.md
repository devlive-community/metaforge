---
title: Home
icon: home
template: home

config:
  toc: false
  sidebar: false

hero:
  title: Open Source Data Platform MetaForge
  description: MetaForge is an integrated software for data conversion, integration and visualization. It supports multiple data sources, file types, big data related databases, relational databases, NoSQL databases, etc. The software can be used to manage multiple data sources, perform various operations on the data under the source, make data charts, monitor data sources and other functions.
  primaryCta:
    url: /<%= pageData.language %>/download.html
    text: Download and use
  secondaryCta:
    url: /<%= pageData.language %>/reference/get-started/install.html
    text: Learn more

features:
  subtitle: Core Benefits
  title: Why choose us
  description: Provide a full range of technical support and services
  items:
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
        </svg>
      title: Unified query language
      description: MetaForge unifies the query language for all data sources into SQL, whether it's a relational database, NoSQL, file system, or other middleware.
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
      title: Extensive data source support
      description: MetaForge supports over 40+ data sources, including popular databases and middleware such as ClickHouse, MySQL, PostgreSQL, MongoDB, Redis, Elasticsearch, Kafka, and more.
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
        </svg>
      title: Flexible connectivity
      description: The MetaForge system supports connecting to different data sources through multiple protocols such as JDBC, Native, HTTP, etc., providing greater flexibility and compatibility.
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 10l-2 1m0 0l-2-1m2 1v2.5M20 7l-2 1m2-1l-2-1m2 1v2.5M14 4l-2-1-2 1M4 7l2-1M4 7l2 1M4 7v2.5M12 21l-2-1m2 1l2-1m-2 1v-2.5M6 18l-2-1v-2.5M18 18l2-1v-2.5" />
        </svg>
      title: Plug-in architecture design
      description: MetaForge is designed as a plug-in system that supports online installation, uninstallation, updates, and hot deployment, facilitating system expansion and maintenance.
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
        </svg>
      title: Full SQL parsing capabilities
      description: MetaForge has a built-in full SQL parser to ensure accurate parsing and execution of SQL queries.
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      title: Data visualization features
      description: MetaForge provides data visualization functions, which can convert data into charts, graphs, and reports to visualize data and help users understand and analyze data more intuitively.
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22" />
        </svg>
      title: Data source monitoring
      description: MetaForge provides the data source monitoring function, which can monitor the status, performance, and health metrics of the data source in real time, help users understand the operation of the data source, and ensure the reliability of the data source.
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
        </svg>
      title: Data Transformation & Integration
      description: MetaForge supports data transformation and integration, which can convert data from different data sources into a unified format and integrate data to achieve unified data management and analysis, and facilitate data migration and integration.
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 14v3m4-3v3m4-3v3M3 21h18M3 10h18M3 7l9-4 9 4M4 10h16v11H4V10z" />
        </svg>
      title: Open Source and Transparency
      description: MetaForge is open source, and users are free to view, modify, and use the source code, ensuring transparency and customizability of the system.
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
      title: Multi-user support
      description: MetaForge has a built-in multi-user management system that supports the configuration and management of different user permissions.
  
    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z" />
        </svg>
      title: Full Web UI
      description: MetaForge has a built-in full web UI that provides a user-friendly interface for data query, management, and visualization, and provides full control over data management.

    - icon: >-
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6 text-indigo-600">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8h2a2 2 0 012 2v6a2 2 0 01-2 2h-2v4l-4-4H9a1.994 1.994 0 01-1.414-.586m0 0L11 14h4a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2v4l.586-.586z" />
        </svg>
      title: Join (DingTalk | WeChat)
      description: >-
        https://github.com/devlive-community/metaforge/issues/950

stats:
  title: Let the numbers speak
  description: What we've achieved
  items:
    - label: GitHub Stars
      value: 900+
    - label: Serving customers
      value: 100+
    - label: Data sources are supported
      value: 60+
    - label: Gitee Stars
      value: 800+

cta:
  title: Ready to get started？
  description: Join us today and start your journey of technological innovation
  button:
    url: /<%= pageData.language %>/reference/get-started/install.html
    text: Use it immediately
---