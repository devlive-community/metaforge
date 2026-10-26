package org.devlive.metaforge.driver;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedisTypeHelper
{
    private static final Map<String, RedisDataType> TYPE_MAP = new HashMap<>();

    private RedisTypeHelper() {}

    public static RedisDataType getType(String className)
    {
        RedisDataType type = TYPE_MAP.get(className);
        return type != null ? type : new RedisDataType(Types.OTHER, "OTHER", Object.class);
    }

    public static RedisDataType getType(Object value)
    {
        if (value == null) {
            return TYPE_MAP.get("NULL");
        }
        return getType(value.getClass().getSimpleName());
    }

    public static int getJdbcType(String className)
    {
        return getType(className).getJdbcType();
    }

    public static int getJdbcType(Object value)
    {
        return getType(value).getJdbcType();
    }

    public static String getTypeName(String className)
    {
        return getType(className).getTypeName();
    }

    public static String getTypeName(Object value)
    {
        return getType(value).getTypeName();
    }

    public static String getJavaClassName(String className)
    {
        return getType(className).getJavaClassName();
    }

    public static String getJavaClassName(Object value)
    {
        return getType(value).getJavaClassName();
    }

    public static class RedisDataType
    {
        private final int jdbcType;
        private final String typeName;
        private final Class<?> javaClass;

        public RedisDataType(int jdbcType, String typeName, Class<?> javaClass)
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
        TYPE_MAP.put("String", new RedisDataType(Types.VARCHAR, "VARCHAR", String.class));
        TYPE_MAP.put("Integer", new RedisDataType(Types.INTEGER, "INTEGER", Integer.class));
        TYPE_MAP.put("Long", new RedisDataType(Types.BIGINT, "BIGINT", Long.class));
        TYPE_MAP.put("Double", new RedisDataType(Types.DOUBLE, "DOUBLE", Double.class));
        TYPE_MAP.put("Float", new RedisDataType(Types.FLOAT, "FLOAT", Float.class));
        TYPE_MAP.put("Boolean", new RedisDataType(Types.BOOLEAN, "BOOLEAN", Boolean.class));
        TYPE_MAP.put("ArrayList", new RedisDataType(Types.ARRAY, "ARRAY", List.class));
        TYPE_MAP.put("LinkedList", new RedisDataType(Types.ARRAY, "ARRAY", List.class));
        TYPE_MAP.put("HashSet", new RedisDataType(Types.ARRAY, "SET", Set.class));
        TYPE_MAP.put("LinkedHashSet", new RedisDataType(Types.ARRAY, "SET", Set.class));
        TYPE_MAP.put("TreeSet", new RedisDataType(Types.ARRAY, "ZSET", Set.class));
        TYPE_MAP.put("HashMap", new RedisDataType(Types.OTHER, "HASH", Map.class));
        TYPE_MAP.put("LinkedHashMap", new RedisDataType(Types.OTHER, "HASH", Map.class));
        TYPE_MAP.put("byte[]", new RedisDataType(Types.BINARY, "BINARY", byte[].class));
        TYPE_MAP.put("NULL", new RedisDataType(Types.NULL, "NULL", Object.class));
    }
}
