package org.devlive.metaforge.service.converter;

import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.utils.JsonUtils;
import org.devlive.metaforge.service.entity.itransient.user.UserEditorEntity;

import javax.persistence.AttributeConverter;

public class UserEditorConverter
        implements AttributeConverter<UserEditorEntity, String>
{
    @Override
    public String convertToDatabaseColumn(UserEditorEntity userEditorEntity)
    {
        return JsonUtils.toJSON(userEditorEntity);
    }

    @Override
    public UserEditorEntity convertToEntityAttribute(String s)
    {
        if (StringUtils.isEmpty(s)) {
            return null;
        }
        else {
            return JsonUtils.toObject(s, UserEditorEntity.class);
        }
    }
}
