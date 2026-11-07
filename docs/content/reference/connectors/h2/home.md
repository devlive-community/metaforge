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

!!! note

    H2 runs inside the MetaForge server, so the version is the H2 library the plugin bundles. The plugin's e2e test `H2E2ETest` runs it in process, without a container: the version check, then a schema, table and columns created with SQL are listed as a database, table and columns, and their rows read back by later queries.

    - [x] 2.1.214

!!!

#### SQL and concepts

---

- The database is an in-memory database named after the `Database` field; it lives until the MetaForge server stops.

| MetaForge | H2 |
|:--|:--|
| Database | Schema |
| Table | Table or view |
| Column | Column |
