package org.devlive.metaforge.sql.builder

import org.junit.Assert
import org.junit.Test
import org.slf4j.LoggerFactory

class TableBuilderTest {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val tableName: String = "TestTable"
    private val columns: List<String> = listOf("id int(32) primary key auto_increment", "name varchar(32) comment 'name'", "age varchar(200) not null default 'xxx'");

    @Test
    fun testCreateTable() {
        TableBuilder.BEGIN()
        TableBuilder.CREATE_TABLE(tableName)
        TableBuilder.COLUMNS(columns)
        Assert.assertEquals(TableBuilder.SQL(), "CREATE TABLE TestTable\n" +
                "(\n" +
                "\tid int(32) primary key auto_increment,\n" +
                "\tname varchar(32) comment 'name',\n" +
                "\tage varchar(200) not null default 'xxx'\n" +
                ")")
    }

    @Test
    fun testCreateTableWithEngine() {
        TableBuilder.BEGIN()
        TableBuilder.CREATE_TABLE(tableName)
        TableBuilder.COLUMNS(columns)
        TableBuilder.ENGINE("MergeTree")
        val sql = TableBuilder.SQL()
        log.info(sql)
        Assert.assertEquals(sql, "CREATE TABLE TestTable\n" +
                "(\n" +
                "\tid int(32) primary key auto_increment,\n" +
                "\tname varchar(32) comment 'name',\n" +
                "\tage varchar(200) not null default 'xxx'\n" +
                ")\n" +
                "ENGINE = MergeTree")
    }

    @Test
    fun testCreateTableWithEngineAndOrderBy() {
        TableBuilder.BEGIN()
        TableBuilder.CREATE_TABLE(tableName)
        TableBuilder.COLUMNS(columns)
        TableBuilder.ENGINE("MergeTree")
        TableBuilder.ORDER_BY(listOf("id"))
        val sql = TableBuilder.SQL()
        log.info(sql)
        Assert.assertEquals(sql, "CREATE TABLE TestTable\n" +
                "(\n" +
                "\tid int(32) primary key auto_increment,\n" +
                "\tname varchar(32) comment 'name',\n" +
                "\tage varchar(200) not null default 'xxx'\n" +
                ")\n" +
                "ENGINE = MergeTree\n" +
                "ORDER BY(\tid)")
    }
}
