---
title: MongoDB Driver
---

MetaForge MongoDB Driver 主要要用于在 MetaForge 中连接和操作 MongoDB 数据库。该驱动支持以下语法:

- `SHOW ...` 语法
- `SELECT ...` 语法

MetaForge MongoDB Driver 适用于所有 MetaForge 版本。

## 使用方式

---

```xml
<dependency>
    <groupId>org.devlive.metaforge</groupId>
    <artifactId>metaforge-driver-mongodb</artifactId>
    <version>${VERSION}</version>
    <scope>test</scope>
</dependency>
```

`VERSION` 可以在 Maven 中央仓库中找到。

驱动名称：`org.devlive.metaforge.driver.MongoJdbcDriver`

支持的连接语法：

- `jdbc:mongodb:`
- `jdbc:mongo:`
- `jdbc:mongodb+srv:`

### 使用示例

- 授权用户

```java
Class.forName("org.devlive.metaforge.driver.MongoJdbcDriver");
Properties props = new Properties();
props.setProperty("database", "xxxx");
props.setProperty("user", "xxxx");
props.setProperty("password", "xxxx");

String jdbcUrl = String.format("jdbc:mongodb://%s:%d", "127.0.0.1", 27017);
connection = DriverManager.getConnection(jdbcUrl, props);
```

- 非授权用户

```java
Class.forName("org.devlive.metaforge.driver.MongoJdbcDriver");
String jdbcUrl = String.format("jdbc:mongodb://%s:%d", "127.0.0.1", 27017);
connection = DriverManager.getConnection(jdbcUrl);
```

## SHOW 语法

MetaForge MongoDB Driver 支持以下 SHOW 语法:

- `SHOW DATABASES`
- `SHOW DATABASES LIKE ...`
- `SHOW TABLES`
- `SHOW TABLES FROM ...`
- `SHOW TABLES LIKE ...`
- `SHOW COLUMNS`
- `SHOW COLUMNS FROM ...`
- `SHOW COLUMNS FROM ... FROM ...`
- `SHOW COLUMNS FROM ... LIKE ...`

## SELECT 语法

MetaForge MongoDB Driver 支持以下 SELECT 语法:

- `SELECT * FROM ...`
- `SELECT ... FROM ...`
- `SELECT column_name AS alias_name FROM ...`
- `SELECT column_name AS alias_name, ... FROM ...`
- `SELECT column_name AS alias_name, ... FROM ... WHERE ...`
- `SELECT column_name AS alias_name, ... FROM ... WHERE ... ORDER BY ...`
- `SELECT column_name AS alias_name, ... FROM ... WHERE ... ORDER BY ... LIMIT ...`
- `SELECT column_name AS alias_name, ... FROM ... WHERE ... GROUP BY ...`
- `SELECT column_name AS alias_name, ... FROM ... WHERE ... GROUP BY ... LIMIT ... OFFSET ...`
- `SELECT column_name AS alias_name, SUM(columnName) ... FROM ... WHERE ... GROUP BY ...`

### 聚合函数

MetaForge MongoDB Driver 支持以下聚合函数:

- `COUNT(*)`
- `COUNT(columnName)`
- `SUM(columnName)`
- `AVG(columnName)`
- `MIN(columnName)`
- `MAX(columnName)`

## 系统函数

MetaForge MongoDB Driver 支持以下系统函数:

- `SELECT VERSION()`
