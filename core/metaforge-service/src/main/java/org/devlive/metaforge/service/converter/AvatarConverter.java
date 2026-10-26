package org.devlive.metaforge.service.converter;

import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.utils.JsonUtils;
import org.devlive.metaforge.service.entity.convert.AvatarEntity;

import javax.persistence.AttributeConverter;

public class AvatarConverter
        implements AttributeConverter<AvatarEntity, String>
{
    @Override
    public String convertToDatabaseColumn(AvatarEntity entity)
    {
        return JsonUtils.toJSON(entity);
    }

    @Override
    public AvatarEntity convertToEntityAttribute(String s)
    {
        if (StringUtils.isEmpty(s)) {
            return null;
        }
        return JsonUtils.toObject(s, AvatarEntity.class);
    }
}
