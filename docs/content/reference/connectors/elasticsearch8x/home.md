---
title: Elasticsearch 8.x
---

<img src="/assets/plugin/elasticsearch8x.svg" class="connector-content-logo" />

#### What is Elasticsearch 8.x ?

Elasticsearch is a distributed search and analytics engine; MetaForge queries it with Elasticsearch SQL.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `ElasticSearch8xPlugin#describe`) and connects through the JDBC driver `org.elasticsearch.xpack.sql.jdbc.EsDriver`.

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |  `9200`  |
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

    The plugin's e2e test `ElasticSearch8xE2ETest` verifies each version below in a container: the source check, then an index written over HTTP is listed as a table of the cluster with its fields as columns, and its documents queried with SQL. The SQL JDBC driver needs a Platinum or trial license on the cluster; Elasticsearch SQL cannot report the server version, so the source check only proves the connection.

    - [x] 8.15
    - [x] 8.19

!!!

#### SQL and concepts

---

| MetaForge | Elasticsearch 8.x |
|:--|:--|
| Database | Cluster |
| Table | Index or alias |
| Column | Field |
