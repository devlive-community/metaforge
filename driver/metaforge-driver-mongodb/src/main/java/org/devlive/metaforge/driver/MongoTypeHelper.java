package org.devlive.metaforge.driver;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public class MongoTypeHelper
{
    private static final Map<String, MongoDataType> TYPE_MAP = new HashMap<>();

    private MongoTypeHelper() {}

    // Get MongoDataType by class name
    // 通过类名获取MongoDataType
    public static MongoDataType getType(String className)
    {
        MongoDataType type = TYPE_MAP.get(className);
        return type != null ? type : new MongoDataType(Types.OTHER, "OTHER", Object.class);
    }

    // Get MongoDataType by object
    // 通过对象获取MongoDataType
    public static MongoDataType getType(Object value)
    {
        if (value == null) {
            return TYPE_MAP.get("NULL");
        }
        return getType(value.getClass().getSimpleName());
    }

    // Get JDBC type by class name
    // 通过类名获取JDBC类型
    public static int getJdbcType(String className)
    {
        return getType(className).getJdbcType();
    }

    // Get JDBC type by object
    // 通过对象获取JDBC类型
    public static int getJdbcType(Object value)
    {
        return getType(value).getJdbcType();
    }

    // Get type name by class name
    // 通过类名获取类型名称
    public static String getTypeName(String className)
    {
        return getType(className).getTypeName();
    }

    // Get type name by object
    // 通过对象获取类型名称
    public static String getTypeName(Object value)
    {
        return getType(value).getTypeName();
    }

    // Get Java class name by class name
    // 通过类名获取Java类名
    public static String getJavaClassName(String className)
    {
        return getType(className).getJavaClassName();
    }

    // Get Java class name by object
    // 通过对象获取Java类名
    public static String getJavaClassName(Object value)
    {
        return getType(value).getJavaClassName();
    }

    // Data type container class
    // 数据类型容器类
    public static class MongoDataType
    {
        private final int jdbcType;
        private final String typeName;
        private final Class<?> javaClass;

        public MongoDataType(int jdbcType, String typeName, Class<?> javaClass)
        {
            this.jdbcType = jdbcType;
            this.typeName = typeName;
            this.javaClass = javaClass;
        }

        public int getJdbcType()
        {
            return jdbcType;
        }

        public String getTypeName()
        {
            return typeName;
        }

        public String getJavaClassName()
        {
            return javaClass.getName();
        }
    }

    static {
        // Initialize basic types
        // 初始化基本类型
        TYPE_MAP.put("String", new MongoDataType(Types.VARCHAR, "VARCHAR", String.class));
        TYPE_MAP.put("ObjectId", new MongoDataType(Types.VARCHAR, "VARCHAR", String.class));
        TYPE_MAP.put("Integer", new MongoDataType(Types.INTEGER, "INTEGER", Integer.class));
        TYPE_MAP.put("Long", new MongoDataType(Types.BIGINT, "BIGINT", Long.class));
        TYPE_MAP.put("Double", new MongoDataType(Types.DOUBLE, "DOUBLE", Double.class));
        TYPE_MAP.put("Boolean", new MongoDataType(Types.BOOLEAN, "BOOLEAN", Boolean.class));
        TYPE_MAP.put("Date", new MongoDataType(Types.TIMESTAMP, "TIMESTAMP", java.sql.Timestamp.class));
        TYPE_MAP.put("ArrayList", new MongoDataType(Types.ARRAY, "ARRAY", java.util.ArrayList.class));
        TYPE_MAP.put("Document", new MongoDataType(Types.OTHER, "OBJECT", Object.class));
        TYPE_MAP.put("Binary", new MongoDataType(Types.BINARY, "BINARY", byte[].class));
        TYPE_MAP.put("Decimal128", new MongoDataType(Types.DECIMAL, "DECIMAL", java.math.BigDecimal.class));
        TYPE_MAP.put("NULL", new MongoDataType(Types.NULL, "NULL", Object.class));
    }
}
