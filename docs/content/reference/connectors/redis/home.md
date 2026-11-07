---
title: Redis
---

#### What is Redis ?

The open source, in-memory data store used by millions of developers as a database, cache, streaming engine, and message broker.

#### Environment

!!! note
    If you need to use this data source, you need to upgrade the MetaForge service to >= `1.3.x`

!!!

Support Time: `2022-12-01`

#### Configure

---

MetaForge declares the configuration fields inside the plugin class by default (see `RedisPlugin#describe`)

!!! note

    Queries are executed through the JDBC driver (`metaforge-driver-redis`): `SELECT` and `SHOW` read keys, and any other statement is sent to Redis as a plain command (`GET user:1`, `HGETALL h`, `KEYS *`), as earlier releases did.

!!!

::: tabs
    === "Configure"
        | Field  |             Required              | Default Value |
        |:------:|:---------------------------------:|:-------------:|
        | `Name` | :check{20,#3CA34F}:  |       \-       |
        | `Host` | :check{20,#3CA34F}: |  `127.0.0.1`  |
        | `Port` | :check{20,#3CA34F}: |    `6379`     |
    === "Authorization"
        |   Field    |        Required         | Default Value |
        |:----------:|:-----------------------:|:-------------:|
        | `Username` | :x{20,#ff0000}: |       \-       |
        | `Password` | :x{20,#ff0000}: |       \-       |
    === "Advanced"
        |   Field    |        Required         | Default Value |
        |:----------:|:-----------------------:|:-------------:|
        | `Database` | :x{20,#ff0000}: |       `0`       |
:::

#### Version (Validation)

---

!!! note

    The plugin's e2e test `RedisE2ETest` verifies each version below in a container: keys written with plain Redis commands, read back with SQL, and listed as databases, tables and columns.

    - [x] 6.2
    - [x] 7.4

!!!

#### SQL and concepts

---

Redis is browsed like MySQL: choose a database at the top, and its keys are the tables.

| MetaForge | Redis | SQL |
|:--|:--|:--|
| Database | Database `0` .. `n-1` (`CONFIG GET databases`, 16 when CONFIG is refused) | `SHOW DATABASES` |
| Table | Key (the first 10 000 per database) | `SHOW TABLES` |
| Column | `key`, `type`, `value` | `SHOW COLUMNS FROM` `` `user:1` `` |

| Statement | Result |
|:--|:--|
| ``SELECT * FROM `user:1` `` | The key, one row of `key`, `type`, `value` |
| ``SELECT * FROM `2`.`user:1` LIMIT 10`` | The key in database 2 |
| ``SELECT `key`, type FROM `user:*` `` | Every key matching the pattern (`*`, `?`) |
| `SELECT * FROM user` | `user` itself, or else every key starting with `user` |
| `GET user:1`, `HGETALL h`, ... | A plain command; its reply in one `RESULTS` column |

- Quote keys with `:` or other symbols in backticks, and write the `key` column as `` `key` ``: `KEY` is a reserved word.
- Choose the database in the source settings or with `` `n`.`key` ``, not with `SELECT n`: connections are pooled, so the command is refused.
