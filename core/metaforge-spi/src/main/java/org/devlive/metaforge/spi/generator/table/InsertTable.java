package org.devlive.metaforge.spi.generator.table;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InsertTable
        extends AbstractTable
{
    private final List<JsonNode> values = new ArrayList<>();
    private final Set<String> primaryKeys = new HashSet<>();
    private Set<String> columnNames;

    private InsertTable(String database, String name)
    {
        super(database, name);
    }

    public static InsertTable create(String database, String name)
    {
        return new InsertTable(database, name);
    }

    /**
     * 设置主键列
     *
     * @param keys 主键列名列表
     * @return InsertTable实例
     */
    public InsertTable addPrimaryKeys(List<String> keys)
    {
        primaryKeys.clear();
        primaryKeys.addAll(keys);
        return this;
    }

    /**
     * 添加单行数据
     *
     * @param value JsonNode 对象
     * @return InsertTable实例
     */
    public InsertTable addValue(JsonNode value)
    {
        if (columnNames == null) {
            columnNames = new HashSet<>();
            Iterator<String> fieldNames = value.fieldNames();
            while (fieldNames.hasNext()) {
                columnNames.add(fieldNames.next());
            }
        }
        values.add(value);
        return this;
    }

    /**
     * 添加多行数据
     *
     * @param multipleValues JsonNode 列表
     * @return InsertTable实例
     */
    public InsertTable addValues(List<JsonNode> multipleValues)
    {
        if (!multipleValues.isEmpty() && columnNames == null) {
            columnNames = new HashSet<>();
            Iterator<String> fieldNames = multipleValues.get(0).fieldNames();
            while (fieldNames.hasNext()) {
                columnNames.add(fieldNames.next());
            }
        }
        values.addAll(multipleValues);
        return this;
    }

    private Object processJsonValue(JsonNode value, String columnName)
    {
        // 如果是主键列，返回 null
        if (primaryKeys.contains(columnName)) {
            return null;
        }

        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return value.asText().replace("'", "''");
        }
        if (value.isNumber()) {
            return value.isInt() || value.isLong() ?
                    value.asLong() : value.asDouble();
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isArray()) {
            Object[] arrayValues = new Object[value.size()];
            for (int i = 0; i < value.size(); i++) {
                arrayValues[i] = processJsonValue(value.get(i), columnName);
            }
            return arrayValues;
        }
        return value.toString();
    }

    private String formatValue(Object value)
    {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return "'" + value + "'";
        }
        if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            return "(" + String.join(", ", formatArrayValues(array)) + ")";
        }
        return String.valueOf(value);
    }

    private String[] formatArrayValues(Object[] values)
    {
        String[] formattedValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                formattedValues[i] = "NULL";
            }
            else if (values[i] instanceof String) {
                formattedValues[i] = "'" + values[i] + "'";
            }
            else {
                formattedValues[i] = String.valueOf(values[i]);
            }
        }
        return formattedValues;
    }

    @Override
    public String build()
    {
        if (values.isEmpty()) {
            throw new IllegalStateException("No values specified for INSERT statement");
        }
        if (columnNames == null || columnNames.isEmpty()) {
            throw new IllegalStateException("No columns found in the provided data");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO `")
                .append(database)
                .append("`.`")
                .append(name)
                .append("` (")
                .append(columnNames.stream()
                        .map(col -> "`" + col + "`")
                        .collect(Collectors.joining(", ")))
                .append(")\nVALUES ");

        // 构建值部分
        List<String> valueGroups = values.stream()
                .map(jsonNode -> {
                    List<String> rowValues = columnNames.stream()
                            .map(col -> {
                                JsonNode value = jsonNode.get(col);
                                Object processed = processJsonValue(value, col);
                                return formatValue(processed);
                            })
                            .collect(Collectors.toList());
                    return "(" + String.join(", ", rowValues) + ")";
                })
                .collect(Collectors.toList());

        sql.append(String.join(",\n", valueGroups));
        sql.append(";");

        return sql.toString();
    }
}
