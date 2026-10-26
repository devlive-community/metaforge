---
title: Apache Hadoop HDFS
---

#### What is HDFS ?

The Hadoop Distributed File System (HDFS) is a distributed file system designed to run on commodity hardware. It has many similarities with existing distributed file systems.

#### Environment

---

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.9.x`

!!!

Support Time: `2023-04-27`

#### Configure

---


!!! note

    If your HDFS service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

!!!

::: tabs
    === "Configure"
        | Field  |             Required              |             Default Value             |
        |:------:|:---------------------------------:|:-------------------------------------:|
        | `Name` | :check{20,#3CA34F}:  |                   \-                   |
    === "Advanced"
        |   Field    |             Required             | Description | Default Value |
        |:----------:|:--------------------------------:|:-----------:|:-------------:|
        | `file` | :check{20,#3CA34F}: | `core-site.xml` <br /> `hdfs-site.xml`  |   `[]`   |
:::

#### Version (Validation)

---

!!! warning

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/EdurtIO/metaforge/issues/new/choose) to us

    - [x] `3.x`

!!!