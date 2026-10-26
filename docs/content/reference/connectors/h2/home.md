---
title: H2 Database
---

#### What is h2 ?

H2 is an embedded database developed in Java that is itself just a class library. 

#### Environment

---

!!! note
    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.8.x`
!!!

Support Time: `2023-04-05`

#### Configure

---

!!! note

    If your h2 service version requires other special configurations, please refer to modifying the configuration file and restarting the MetaForge service.

!!!

::: tabs
    === "Configure"
        | Field  |             Required              | Default Value |
        |:------:|:---------------------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}:  |       \-       |
    === "Authorization"
        |   Field    |             Required              | Default Value |
        |:----------:|:---------------------------------:|:-------------:|
        | `Username` | :check{20,#3CA34F}:  |       \-       |
        | `Password` | :x{20,#ff0000}:  |       \-       |
    === "Advanced"
        |   Field    |             Required             | Default Value |
        |:----------:|:--------------------------------:|:-------------:|
        | `Database` | :check{20,#3CA34F}: |               |
:::

#### Version (Validation)

---

!!! warning

    The online service has not been tested yet, if you have detailed test results, please submit [issues](https://github.com/EdurtIO/metaforge/issues/new/choose) to us

    - [x] `all`

!!!