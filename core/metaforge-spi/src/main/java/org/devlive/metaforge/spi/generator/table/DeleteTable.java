package org.devlive.metaforge.spi.generator.table;

import com.fasterxml.jackson.databind.JsonNode;
import org.devlive.metaforge.spi.generator.Filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeleteTable
        extends AbstractTable
{
    private final List<Filter> filters = new ArrayList<>();
    private final List<BatchDelete> batchDeletes = new ArrayList<>();
    private Long limit;

    private DeleteTable(String database, String name)
    {
        super(database, name);
    }

    public static DeleteTable create(String database, String name)
    {
        return new DeleteTable(database, name);
    }

    /**
     * 从 JsonNode 添加过滤条件
     *
     * @param node JsonNode 对象
     * @return DeleteTable实例
     */
    public DeleteTable addFilters(JsonNode node)
    {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode value = field.getValue();

            if (!value.isNull()) {
                Object processedValue;
                if (value.isTextual()) {
                    // 文本类型，需要处理单引号转义
                    processedValue = value.asText().replace("'", "''");
                }
                else if (value.isNumber()) {
                    // 数值类型，保持原始数值
                    if (value.isInt() || value.isLong()) {
                        processedValue = value.asLong();
                    }
                    else {
                        processedValue = value.asDouble();
                    }
                }
                else if (value.isBoolean()) {
                    // 布尔类型
                    processedValue = value.asBoolean();
                }
                else if (value.isArray()) {
                    // 数组类型，递归处理每个元素
                    Object[] arrayValues = new Object[value.size()];
                    for (int i = 0; i < value.size(); i++) {
                        JsonNode element = value.get(i);
                        if (element.isTextual()) {
                            arrayValues[i] = element.asText().replace("'", "''");
                        }
                        else if (element.isNumber()) {
                            arrayValues[i] = element.isInt() || element.isLong() ?
                                    element.asLong() : element.asDouble();
                        }
                        else if (element.isBoolean()) {
                            arrayValues[i] = element.asBoolean();
                        }
                        else if (element.isNull()) {
                            arrayValues[i] = null;
                        }
                        else {
                            arrayValues[i] = element.toString();
                        }
                    }
                    processedValue = arrayValues;
                }
                else if (value.asText().equalsIgnoreCase("null")) {
                    processedValue = null;
                }
                else {
                    // 其他类型，转换为字符串
                    processedValue = value.toString();
                }
                addFilter(Filter.create(key, Filter.Operator.EQ, processedValue));
            }
        }
        return this;
    }

    public DeleteTable addFilter(Filter filter)
    {
        filters.add(filter);
        return this;
    }

    public DeleteTable limit(long limit)
    {
        this.limit = limit;
        return this;
    }

    /**
     * 添加单列的批量删除条件
     *
     * @param column 列名
     * @param values 值列表
     * @return DeleteTable实例
     */
    public DeleteTable addBatchDelete(String column, List<String> values)
    {
        batchDeletes.add(new SingleColumnBatchDelete(column, values));
        return this;
    }

    /**
     * 添加多列的批量删除条件
     *
     * @param columns 列名列表
     * @param valuesList 值组合列表
     * @return DeleteTable实例
     */
    public DeleteTable addBatchDelete(List<String> columns, List<List<String>> valuesList)
    {
        batchDeletes.add(new MultiColumnBatchDelete(columns, valuesList));
        return this;
    }

    @Override
    public String build()
    {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM `")
                .append(database).append("`.`")
                .append(name)
                .append("`");

        // 添加 WHERE 条件
        List<String> allConditions = new ArrayList<>();

        // 添加普通过滤条件
        if (!filters.isEmpty()) {
            allConditions.add(filters.stream()
                    .map(Filter::build)
                    .collect(Collectors.joining("\nAND ")));
        }

        // 添加批量删除条件
        if (!batchDeletes.isEmpty()) {
            allConditions.addAll(batchDeletes.stream()
                    .map(BatchDelete::build)
                    .collect(Collectors.toList()));
        }

        if (!allConditions.isEmpty()) {
            sql.append("\nWHERE ")
                    .append(String.join("\nAND ", allConditions));
        }

        // 添加 LIMIT
        if (limit != null) {
            sql.append("\nLIMIT ").append(limit);
        }

        sql.append(";");
        return sql.toString();
    }

    private interface BatchDelete
    {
        String build();
    }

    private static class SingleColumnBatchDelete
            implements BatchDelete
    {
        private final String column;
        private final List<String> values;

        public SingleColumnBatchDelete(String column, List<String> values)
        {
            this.column = column;
            this.values = values;
        }

        @Override
        public String build()
        {
            return "`" + column + "` IN (" +
                    String.join(", ", values) + ")";
        }
    }

    private static class MultiColumnBatchDelete
            implements BatchDelete
    {
        private final List<String> columns;
        private final List<List<String>> valuesList;

        public MultiColumnBatchDelete(List<String> columns, List<List<String>> valuesList)
        {
            this.columns = columns;
            this.valuesList = valuesList;
        }

        @Override
        public String build()
        {
            String columnStr = columns.stream()
                    .map(col -> "`" + col + "`")
                    .collect(Collectors.joining(", "));

            String valuesStr = valuesList.stream()
                    .map(values -> "(" + String.join(", ", values) + ")")
                    .collect(Collectors.joining(", "));

            return "(" + columnStr + ") IN (" + valuesStr + ")";
        }
    }
}
