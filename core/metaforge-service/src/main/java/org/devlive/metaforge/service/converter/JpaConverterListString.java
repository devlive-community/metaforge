package org.devlive.metaforge.service.converter;

import javax.persistence.AttributeConverter;

import java.util.Arrays;
import java.util.List;

public class JpaConverterListString
        implements AttributeConverter<List<String>, String>
{
    private String delimiter = ",";

    @Override
    public String convertToDatabaseColumn(List<String> attribute)
    {
        return String.join(delimiter, attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData)
    {
        return Arrays.asList(dbData.split(delimiter));
    }
}
