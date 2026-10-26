package org.devlive.metaforge.sql.builder

open class BaseBuilderTest {
    val tableName: String = "TestTable"
    val columns: List<String> = listOf("id int(32) primary key auto_increment", "name varchar(32) comment 'name'", "age varchar(200) not null default 'xxx'");
}
