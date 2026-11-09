---
title: Apache IoTDB
---

<img src="/assets/plugin/iotdb.svg" class="connector-content-logo" />

#### What is Apache IoTDB ?

Apache IoTDB is a time series database for IoT data, organized as a tree of databases, devices and measurements.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `IoTDBPlugin#describe`) and connects through the JDBC driver `org.apache.iotdb.jdbc.IoTDBDriver`.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |  `6667`  |
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

    The plugin's e2e test `IoTDBE2ETest` verifies each version below in a container: the version check, then a database and time series created with SQL are listed as a database, device and measurements, and their points read back.

    - [x] 1.3
    - [x] 2.0

!!!

#### SQL and concepts

---

| MetaForge | Apache IoTDB |
|:--|:--|
| Database | Database, e.g. `root.sg` |
| Table | Device, by its full path, e.g. `root.sg.d1` |
| Column | Measurement |
