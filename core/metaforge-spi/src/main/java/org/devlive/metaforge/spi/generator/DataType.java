package org.devlive.metaforge.spi.generator;

public enum DataType
{
    // 数值类型
    TINYINT("TINYINT"),
    SMALLINT("SMALLINT"),
    INT("INT"),
    BIGINT("BIGINT"),
    FLOAT("FLOAT"),
    DOUBLE("DOUBLE"),
    DECIMAL("DECIMAL"),

    // 字符串类型
    CHAR("CHAR"),
    VARCHAR("VARCHAR"),
    TEXT("TEXT"),
    LONGTEXT("LONGTEXT"),

    // 日期时间类型
    DATE("DATE"),
    TIME("TIME"),
    DATETIME("DATETIME"),
    TIMESTAMP("TIMESTAMP"),

    // 二进制类型
    BLOB("BLOB"),
    LONGBLOB("LONGBLOB");

    private final String value;

    DataType(String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }

    public String withLength(int length)
    {
        if (this == VARCHAR || this == CHAR) {
            return value + "(" + length + ")";
        }
        return value;
    }

    public String withPrecision(int precision, int scale)
    {
        if (this == DECIMAL) {
            return value + "(" + precision + "," + scale + ")";
        }
        return value;
    }
}
