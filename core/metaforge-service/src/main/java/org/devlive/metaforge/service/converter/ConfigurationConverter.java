package org.devlive.metaforge.service.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.devlive.metaforge.service.itransient.configuration.Configuration;

import javax.persistence.AttributeConverter;

public class ConfigurationConverter
        implements AttributeConverter<Configuration, String>
{
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Override
    public String convertToDatabaseColumn(Configuration configuration)
    {
        try {
            return configuration != null ? objectMapper.writeValueAsString(configuration) : null;
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Error converting configuration to JSON", e);
        }
    }

    @Override
    public Configuration convertToEntityAttribute(String json)
    {
        try {
            return json != null ? objectMapper.readValue(json, Configuration.class) : null;
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Error converting JSON to configuration", e);
        }
    }
}
