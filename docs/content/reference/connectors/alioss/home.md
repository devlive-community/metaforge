---
title: AliYun OSS
---

#### What is Aliyun OSS ?

Fully managed object storage service to store and access any amount of data from anywhere

#### Driver

---

MetaForge reaches OSS only through JDBC, with its own driver `org.devlive.metaforge.AliOssDriver` (module `metaforge-driver-alioss`, built on `metaforge-parser` and aliyun-sdk-oss).

- URL: `jdbc:alioss://oss-cn-hangzhou.aliyuncs.com[/bucket][?ssl=true&maxRows=10000&contentLimit=0]`
- `Host` is the endpoint (an `http://` endpoint turns TLS off); `Username` / `Password` are the AccessKey ID and secret; `Database` is an optional bucket, which is the only one listed when the key may not list buckets.
- `maxRows` caps the objects a query reads; `contentLimit` is the largest object (bytes) whose text `SELECT` reads into `content`, 0 (the default) reads none.
- The object browser of earlier versions is replaced by the database / table / column workspace.

#### Concepts

---

| MetaForge | OSS | Example |
|:--|:--|:--|
| Database | Bucket | `lake` |
| Table | A top-level prefix | `logs/` |
| Row | An object under the prefix | `logs/2026/a.log` → key `2026/a.log` |
| Column | Fixed: `key`, `size`, `etag`, `last_modified`, `storage_class`, `content` | |

`key` is an SQL keyword: write `` `key` ``. Objects directly at the bucket root are not under any table.

#### Supported SQL

---

```sql
SHOW DATABASES;  SHOW TABLES FROM lake;  SHOW COLUMNS FROM logs FROM lake;

CREATE DATABASE lake;                      -- creates the bucket
DROP DATABASE lake;                        -- deletes the bucket (OSS refuses one that holds objects)
CREATE TABLE lake.logs (`key` VARCHAR);    -- writes the folder object logs/ (columns are fixed)
DROP TABLE lake.logs;                      -- deletes every object under logs/

INSERT INTO lake.logs (`key`, content) VALUES ('a.log', 'hello');          -- writes logs/a.log
UPDATE lake.logs SET content = 'changed' WHERE `key` = 'a.log';            -- rewrites it
UPDATE lake.logs SET `key` = 'b.log' WHERE `key` = 'a.log';                -- copies, then deletes the old object
DELETE FROM lake.logs WHERE `key` LIKE '2026/%';
SELECT `key`, size, last_modified FROM lake.logs WHERE size > 1024 ORDER BY last_modified DESC LIMIT 50;
SELECT version();                          -- the OSS API version, 2013-10-15
```

`ALTER TABLE` is refused because the columns are fixed.

#### Version (Validation)

---

!!! note

    OSS is a cloud service without releases or a local server. The driver's unit test `AliOssSourceTest` checks the mapping against an in-memory OSS on every build; the plugin's e2e test `AliOssE2ETest` runs the same SQL against a real bucket when `OSS_E2E_ENDPOINT`, `OSS_E2E_ACCESS_KEY_ID`, `OSS_E2E_ACCESS_KEY_SECRET` and `OSS_E2E_BUCKET` are set, and is skipped otherwise.

    - [x] OSS API 2013-10-15 (in-memory, every build)
    - [ ] Aliyun OSS service (needs an account: run `AliOssE2ETest` with the variables above)

!!!
