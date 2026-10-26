package org.devlive.metaforge.service.converter;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.enums.DataSetState;

import javax.persistence.AttributeConverter;

import java.util.Arrays;
import java.util.List;

public class ListConverter
        implements AttributeConverter<List, String>
{
    @Override
    public String convertToDatabaseColumn(List map)
    {
        // 处理 null 值
        if (map == null || map.isEmpty()) {
            return null;
        }

        List<String> values = Lists.newArrayList();
        for (Object state : map) {
            if (state != null) {  // 添加 null 检查
                values.add(state.toString());
            }
        }
        return String.join(",", values);
    }

    @Override
    public List convertToEntityAttribute(String s)
    {
        if (StringUtils.isEmpty(s)) {
            return Lists.newArrayList();  // 返回空列表而不是 null
        }
        else {
            try {
                // 尝试转换为 DataSetState
                return Lists.newArrayList(
                        Arrays.stream(s.split(","))
                                .map(DataSetState::valueOf)
                                .toArray(DataSetState[]::new)
                );
            }
            catch (IllegalArgumentException e) {
                // 如果转换失败，至少返回原始字符串列表
                return Lists.newArrayList(s.split(","));
            }
        }
    }
}
