package org.devlive.metaforge.common.enums;

import lombok.Getter;

public enum NodeType
{
    CATALOG("CATALOG"),
    SCHEMA("SCHEMA"),
    CHARSET("CHARSET"),
    TABLE("TABLE"),
    TYPE("TYPE"),
    COLUMN_TYPE("COLUMN_TYPE"),
    ENGINE("ENGINE"),
    FORMAT("FORMAT"),
    ROWS("ROWS"),
    CREATE_TIME("CREATE_TIME"),
    UPDATE_TIME("UPDATE_TIME"),
    COLLATION("COLLATION"),
    COMMENT("COMMENT"),
    DEFAULT("DEFAULT"),
    POSITION("POSITION"),
    NULLABLE("NULLABLE"),
    MAXIMUM_LENGTH("MAXIMUM_LENGTH"),
    OCTET_LENGTH("OCTET_LENGTH"),
    KEY("KEY"),
    PRIVILEGES("PRIVILEGES"),
    COLUMN("COLUMN"),
    DATA_TYPE("DATA_TYPE"),
    EXTRA("EXTRA"),
    AVG_ROW("AVG_ROW"),
    DATA("DATA"),
    INDEX("INDEX"),
    AUTO_INCREMENT("AUTO_INCREMENT");

    @Getter
    private String value;

    NodeType(String value)
    {
        this.value = value;
    }
}
