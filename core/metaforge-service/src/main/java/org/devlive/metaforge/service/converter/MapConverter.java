package org.devlive.metaforge.service.converter;

import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.utils.JsonUtils;

import javax.persistence.AttributeConverter;

import java.util.Map;

public class MapConverter
        implements AttributeConverter<Map, String>
{
    @Override
    public String convertToDatabaseColumn(Map map)
    {
        return JsonUtils.toJSON(map);
    }

    @Override
    public Map convertToEntityAttribute(String s)
    {
        if (StringUtils.isEmpty(s)) {
            return null;
        }
        else {
            return JsonUtils.toObject(s, Map.class);
        }
    }
}
