---
title: MySQL
---

#### What is MySQL ?

MySQL is one of the most recognizable technologies in the modern big data ecosystem. Often called the most popular database and currently enjoying widespread, effective use regardless of industry, it’s clear that anyone involved with enterprise data or general IT should at least aim for a basic familiarity of MySQL.

#### Environment

!!! note

    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.0.x`

!!!

Support Time: `2022-09-19`

#### Configure

---

MetaForge declares the configuration fields inside the plugin class by default (see `MySQLPlugin#describe`)

!!! note

    If your MySQL service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

!!!

::: tabs
    === "Configure"
        | Field  |             Required              | Default Value |
        |:------:|:---------------------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}:  |       \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |     `3306`    |
    === "Authorization"
        |   Field    |             Required              | Default Value |
        |:----------:|:---------------------------------:|:-------------:|
        | `Username` | :check{20,#3CA34F}:  |       \-       |
        | `Password` | :check{20,#3CA34F}: |       \-       |
        |   `SSL`    |      :x{20,#ff0000}:      |    `false`    |
    === "Advanced"
        |   Field    |             Required             | Default Value |
        |:----------:|:--------------------------------:|:-------------:|
        | `Database` | :check{20,#3CA34F}: |   `default`   |
    === "Custom"
        !!! note
    
            You can add the already supported MySQL parameters by adding Key Value, the parameters can be [reference document](https://www.mysql.com/cn/products/connector/)
    
            Default:
            
            |              Key              | value  | 
            |:-----------------------------:|:------:|
            | `useOldAliasMetadataBehavior` | `true` |
        !!!
:::
#### Version (Validation)

---

!!! warning

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/EdurtIO/metaforge/issues/new/choose) to us

    - [x] 5.x
    - [x] 6.x
    - [x] 7.x

!!!