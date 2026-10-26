package org.devlive.metaforge.sql.model

import org.junit.Assert
import org.junit.Test
import org.slf4j.LoggerFactory.getLogger

class ColumnTest {
    private val log = getLogger(this.javaClass)

    @Test
    fun testToColumnVar() {
        val column = Column()

        log.info("======== Test basic ========")
        column.name = "testColumnName"
        column.type = "VARCHAR"
        log.info(column.toColumnVar())
        Assert.assertEquals(column.toColumnVar(), "testColumnName VARCHAR")

        log.info("======== Test length ========")
        column.length = 10
        log.info(column.toColumnVar())
        Assert.assertEquals(column.toColumnVar(), "testColumnName VARCHAR(10)")

        log.info("======== Test not null ========")
        column.isNullable = true
        log.info(column.toColumnVar())
        Assert.assertEquals(column.toColumnVar(), "testColumnName VARCHAR(10) NOT NULL")

        log.info("======== Test primary key ========")
        column.primaryKey = true
        log.info(column.toColumnVar())
        Assert.assertEquals(column.toColumnVar(), "testColumnName VARCHAR(10) NOT NULL PRIMARY KEY")

        log.info("======== Test auto increment ========")
        column.autoIncrement = true
        log.info(column.toColumnVar())
        Assert.assertEquals(column.toColumnVar(), "testColumnName VARCHAR(10) NOT NULL PRIMARY KEY AUTO_INCREMENT")

        log.info("======== Test comment ========")
        column.comment = "test"
        log.info(column.toColumnVar())
        Assert.assertEquals(column.toColumnVar(), "testColumnName VARCHAR(10) NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT 'test'")

        log.info("======== Test default value ========")
        column.defaultValue = "test"
        log.info(column.toColumnVar())
        Assert.assertEquals(column.toColumnVar(), "testColumnName VARCHAR(10) NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT 'test' DEFAULT 'test'")
    }
}