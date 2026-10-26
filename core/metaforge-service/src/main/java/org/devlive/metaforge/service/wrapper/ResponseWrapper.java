package org.devlive.metaforge.service.wrapper;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.spi.model.Pagination;
import org.devlive.metaforge.spi.model.Response;

import java.util.ArrayList;
import java.util.List;

@Data
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ResponseWrapper
{
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private List<String> headers;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private List<String> types;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private List<Object> columns;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Boolean isConnected;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Boolean isSuccessful;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String message;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private TimeWrapper connection;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private TimeWrapper processor;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String content;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Pagination pagination;

    public static ResponseWrapper from(Response response)
    {
        ResponseWrapper wrapper = new ResponseWrapper();
        wrapper.setHeaders(response.getHeaders());
        wrapper.setTypes(response.getTypes());
        wrapper.setColumns(processColumns(response));
        wrapper.setIsConnected(response.getIsConnected());
        wrapper.setIsSuccessful(response.getIsSuccessful());
        wrapper.setMessage(response.getMessage());
        wrapper.setConnection(TimeWrapper.from(response.getConnection()));
        wrapper.setProcessor(TimeWrapper.from(response.getProcessor()));
        wrapper.setContent(response.getContent());
        wrapper.setPagination(response.getPagination());
        return wrapper;
    }

    // 处理 columns 数据，支持 ObjectNode 类型
    // Process columns data with ObjectNode support
    private static List<Object> processColumns(Response response)
    {
        List<Object> originalColumns = response.getColumns();
        if (originalColumns == null || originalColumns.isEmpty()) {
            return originalColumns;
        }

        List<String> headers = response.getHeaders();
        if (headers == null || headers.isEmpty()) {
            return originalColumns;
        }

        List<Object> processedColumns = new ArrayList<>();
        String firstHeader = headers.get(0);

        for (Object column : originalColumns) {
            if (column instanceof ObjectNode) {
                ObjectNode objectNode = (ObjectNode) column;
                // 通过第一个 header 获取实际数据
                // Get actual data through the first header
                JsonNode valueNode = objectNode.get(firstHeader);
                if (valueNode != null) {
                    Object extractedValue = extractValueFromNode(valueNode);
                    processedColumns.add(extractedValue);
                }
                else {
                    // 如果找不到对应的值，保持原 ObjectNode
                    // If the corresponding value is not found, keep the original ObjectNode
                    processedColumns.add(column);
                }
            }
            else {
                // 非 ObjectNode 类型，直接添加
                // Non-ObjectNode type, add directly
                processedColumns.add(column);
            }
        }

        return processedColumns;
    }

    // 从 JsonNode 中提取实际值，支持 POJONode 类型
    // Extract actual value from JsonNode, supporting POJONode type
    private static Object extractValueFromNode(JsonNode valueNode)
    {
        // 处理基本类型
        // Handle basic types
        if (valueNode.isTextual()) {
            return valueNode.asText();
        }
        else if (valueNode.isNumber()) {
            return valueNode.numberValue();
        }
        else if (valueNode.isBoolean()) {
            return valueNode.asBoolean();
        }
        else if (valueNode.isNull()) {
            return null;
        }
        else if (valueNode.isArray()) {
            // 处理数组类型
            // Handle array type
            List<Object> arrayValues = new ArrayList<>();
            for (JsonNode arrayElement : valueNode) {
                arrayValues.add(extractValueFromNode(arrayElement));
            }
            return arrayValues;
        }
        else if (valueNode.isPojo()) {
            // 处理 POJONode - 需要先转换为 ObjectNode
            // Handle POJONode - need to convert to ObjectNode first
            try {
                // 将 POJONode 转换为 ObjectNode 以便访问字段
                // Convert POJONode to ObjectNode for field access
                ObjectNode objectNode = (ObjectNode) valueNode;
                return extractValueFromObjectNode(objectNode);
            }
            catch (ClassCastException e) {
                // 如果转换失败，尝试通过 toString 然后重新解析
                // If conversion fails, try parsing through toString
                return extractValueFromPojoString(valueNode.toString());
            }
        }
        else if (valueNode.isObject()) {
            // 处理普通的 ObjectNode
            // Handle regular ObjectNode
            ObjectNode objectNode = (ObjectNode) valueNode;
            return extractValueFromObjectNode(objectNode);
        }
        else {
            // 对于其他复杂类型，保持原样
            // For other complex types, keep as is
            return valueNode;
        }
    }

    // 从 ObjectNode 中提取值
    // Extract value from ObjectNode
    private static Object extractValueFromObjectNode(ObjectNode objectNode)
    {
        // 优先尝试获取 values 字段
        // Try to get values field first
        JsonNode valuesNode = objectNode.get("values");
        if (valuesNode != null && valuesNode.isArray()) {
            List<Object> values = new ArrayList<>();
            for (JsonNode arrayElement : valuesNode) {
                values.add(extractValueFromNode(arrayElement));
            }
            // 如果只有一个元素，直接返回该元素
            // If there is only one element, return it directly
            if (values.size() == 1) {
                return values.get(0);
            }
            return values;
        }

        // 如果没有 values 字段，尝试获取 string 字段
        // If no values field, try to get string field
        JsonNode stringNode = objectNode.get("string");
        if (stringNode != null && stringNode.isTextual()) {
            return stringNode.asText();
        }

        // 如果都没有，返回原对象
        // If neither exists, return original object
        return objectNode;
    }

    // 从 POJO 的字符串表示中提取值
    // Extract value from POJO string representation
    private static Object extractValueFromPojoString(String pojoString)
    {
        try {
            // 使用 Jackson 解析字符串为 JsonNode
            // Use Jackson to parse string to JsonNode
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode parsedNode = mapper.readTree(pojoString);

            if (parsedNode.isObject()) {
                return extractValueFromObjectNode((ObjectNode) parsedNode);
            }

            return pojoString;
        }
        catch (Exception e) {
            // 解析失败，返回原字符串
            // If parsing fails, return original string
            return pojoString;
        }
    }
}
