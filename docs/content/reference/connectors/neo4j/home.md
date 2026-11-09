---
title: Neo4j
---

<img src="/assets/plugin/neo4j.svg" class="connector-content-logo" />

#### What is Neo4j ?

Neo4j is a graph database: nodes with labels and properties, connected by relationships, queried with Cypher.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `Neo4jPlugin#describe`) and connects with the Neo4j JDBC driver (`org.neo4j.jdbc.Driver`) over Bolt (port `7687`).

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |    `7687`     |
    === "Authorization"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |      \-       |
        | `Password` | :x{20,#ff0000}: |      \-       |
    === "Advanced"
        |   Field    |      Required       | Default Value |
        |:----------:|:-------------------:|:-------------:|
        | `Database` | :check{20,#3CA34F}: |    `neo4j`    |
:::

#### Version (Validation)

---

!!! note

    The plugin's e2e test `Neo4jE2ETest` verifies each version below in a container: nodes created with Cypher are listed as the database, labels and properties, and queried back.

    - [x] 4.4
    - [x] 5

!!!

#### Queries and concepts

---

| MetaForge | Neo4j |
|:--|:--|
| Database | The database the source connects to (`CALL db.info()`) |
| Table | Node label |
| Column | Property of the label's nodes |

- Queries are Cypher: `MATCH (p:Person) RETURN p.name`.
