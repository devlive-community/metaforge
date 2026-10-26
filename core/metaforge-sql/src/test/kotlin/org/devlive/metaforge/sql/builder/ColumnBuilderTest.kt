package org.devlive.metaforge.sql.builder

import org.junit.Assert
import org.junit.Test
import org.slf4j.LoggerFactory.getLogger

class ColumnBuilderTest : BaseBuilderTest() {
    private val log = getLogger(this.javaClass)

    @Test
    fun testCreateColumn() {
        ColumnBuilder.BEGIN()
        ColumnBuilder.CREATE_COLUMN(tableName)
        ColumnBuilder.COLUMNS(columns)
        log.info("======== Test basic ========")
        val sql = ColumnBuilder.SQL()
        log.info(sql)
        Assert.assertEquals(sql, "ALTER TABLE TestTable\n" +
                "ADD COLUMN(\tid int(32) primary key auto_increment,\n" +
                "\tname varchar(32) comment 'name',\n" +
                "\tage varchar(200) not null default 'xxx')")
    }

    @Test
    fun testDropColumn() {
        ColumnBuilder.BEGIN()
        ColumnBuilder.DROP_COLUMN(tableName)
        ColumnBuilder.COLUMNS(listOf("id"))
        log.info("======== Test basic ========")
        val sql = ColumnBuilder.SQL()
        log.info(sql)
        Assert.assertEquals(sql, "ALTER TABLE TestTable\n" +
                "DROP COLUMN \tid")
    }

    @Test
    fun testModifyColumn() {
        ColumnBuilder.BEGIN()
        ColumnBuilder.MODIFY_COLUMN(tableName)
        ColumnBuilder.COLUMNS(listOf(columns.get(0)))
        log.info("======== Test basic ========")
        val sql = ColumnBuilder.SQL()
        log.info(sql)
        Assert.assertEquals(sql, "ALTER TABLE TestTable\n" +
                "MODIFY \tid int(32) primary key auto_increment")
    }
}
