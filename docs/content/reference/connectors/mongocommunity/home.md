---
title: MongoDB Community
---

<img src="/assets/plugin/mongocommunity.svg" class="connector-content-logo" />

#### What is MongoDB ?

MongoDB is a document database: collections of JSON-like documents with no fixed schema.

#### Configure

---

MetaForge declares the configuration fields inside the plugin class (see `MongoCommunityPlugin#describe`) and connects through the JDBC driver `metaforge-driver-mongodb` (`org.devlive.metaforge.MongoDriver`).

::: tabs
    === "Configure"
        | Field  |      Required       | Default Value |
        |:------:|:-------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}: |      \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |    `27017`    |
    === "Authorization"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |      \-       |
        | `Password` | :x{20,#ff0000}: |      \-       |
    === "Advanced"
        |   Field    |    Required     | Default Value |
        |:----------:|:---------------:|:-------------:|
        | `Database` | :x{20,#ff0000}: |    `admin`    |
:::

- The database is the one queries without a database name read, e.g. `SELECT * FROM orders`; `SELECT * FROM shop.orders` names it.

#### Version (Validation)

---

!!! note

    The plugin's e2e test `MongoE2ETest` verifies each version below in a container: documents written by another client are listed as databases, collections and fields, and read back with SQL.

    - [x] 6.0
    - [x] 7.0

!!!

#### SQL and concepts

---

| MetaForge | MongoDB | SQL |
|:--|:--|:--|
| Database | Database | `SHOW DATABASES` |
| Table | Collection | `SHOW TABLES` / `SHOW TABLES FROM shop` |
| Column | Field of the first 100 documents | `SHOW COLUMNS FROM shop.orders` |

- `SELECT` supports `WHERE`, `GROUP BY`, `ORDER BY` and `LIMIT`; `SELECT version()` reports the server version.
