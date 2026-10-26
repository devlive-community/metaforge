package org.devlive.metaforge.service.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.common.utils.JsonUtils;
import org.devlive.metaforge.service.itransient.NotificationTypeEntity;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Converter
public class NotificationTypeConverter
        implements AttributeConverter<List<NotificationTypeEntity>, String>
{
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<NotificationTypeEntity> emptyList = Lists.newArrayList();

    @Override
    public String convertToDatabaseColumn(List<NotificationTypeEntity> attribute)
    {
        if (attribute == null || attribute.isEmpty()) {
            return JsonUtils.toJSON(emptyList);
        }

        return JsonUtils.toJSON(attribute);
    }

    @Override
    public List<NotificationTypeEntity> convertToEntityAttribute(String dbData)
    {
        if (dbData == null || dbData.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(dbData,
                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
        }
        catch (Exception e) {
            log.error("Error converting JSON to List<NotificationTypeEntity>", e);
            return new ArrayList<>();
        }
    }
}
