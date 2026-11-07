---
title: H2 (TCP)
---

<img src="/assets/plugin/h2tcp.svg" class="connector-content-logo" />

#### What is H2 (TCP) ?

H2 is a lightweight Java database; its TCP server lets remote clients open the databases it serves.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `H2TCPPlugin#describe`) and connects through the H2 JDBC driver (`org.h2.Driver`) with `jdbc:h2:tcp://<host>:<port>/<database>`.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |    `9092`     |
    === "Authorization"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |      \-       |
        | `Password` | :x{20,#ff0000}: |      \-       |
    === "Advanced"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Database` | :x{20,#ff0000}: |      \-       |
:::

#### Version (Validation)

---

!!! note

    The plugin's e2e test `H2TCPE2ETest` starts an H2 TCP server of the version the plugin's driver bundles, in process: the version check, then a schema, table and columns created with SQL are listed as a database, table and columns, and their rows read back. H2 servers speak the protocol of their own release, so use a server of the bundled version.

    - [x] 2.1.214

!!!

#### SQL and concepts

---

| MetaForge | H2 |
|:--|:--|
| Database | Schema of the configured database |
| Table | Table or view |
| Column | Column |
