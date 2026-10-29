package org.devlive.metaforge.service.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.utils.JsonUtils;
import org.devlive.metaforge.service.entity.convert.AvatarEntity;
import org.devlive.metaforge.service.entity.itransient.user.UserEditorEntity;
import org.devlive.metaforge.service.itransient.configuration.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Column conversions shared by every entity: JSON-backed value objects stored as text,
 * plus the vendor representations (CLOB, numeric flags) that differ between databases.
 */
public final class JdbcConverters
{
    private static final ObjectMapper NON_NULL = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private JdbcConverters()
    {
    }

    public static List<Object> all()
    {
        return Arrays.asList(
                new AvatarWriter(), new AvatarReader(),
                new UserEditorWriter(), new UserEditorReader(),
                new ConfigurationWriter(), new ConfigurationReader(),
                new ClobReader(), new NumberToBooleanReader());
    }

    @WritingConverter
    static class AvatarWriter
            implements Converter<AvatarEntity, String>
    {
        @Override
        public String convert(AvatarEntity source)
        {
            return JsonUtils.toJSON(source);
        }
    }

    @ReadingConverter
    static class AvatarReader
            implements Converter<String, AvatarEntity>
    {
        @Override
        public AvatarEntity convert(String source)
        {
            return StringUtils.isEmpty(source) ? null : JsonUtils.toObject(source, AvatarEntity.class);
        }
    }

    @WritingConverter
    static class UserEditorWriter
            implements Converter<UserEditorEntity, String>
    {
        @Override
        public String convert(UserEditorEntity source)
        {
            return JsonUtils.toJSON(source);
        }
    }

    @ReadingConverter
    static class UserEditorReader
            implements Converter<String, UserEditorEntity>
    {
        @Override
        public UserEditorEntity convert(String source)
        {
            return StringUtils.isEmpty(source) ? null : JsonUtils.toObject(source, UserEditorEntity.class);
        }
    }

    @WritingConverter
    static class ConfigurationWriter
            implements Converter<Configuration, String>
    {
        @Override
        public String convert(Configuration source)
        {
            try {
                return NON_NULL.writeValueAsString(source);
            }
            catch (IOException e) {
                throw new IllegalArgumentException("Error converting configuration to JSON", e);
            }
        }
    }

    @ReadingConverter
    static class ConfigurationReader
            implements Converter<String, Configuration>
    {
        @Override
        public Configuration convert(String source)
        {
            try {
                return StringUtils.isEmpty(source) ? null : NON_NULL.readValue(source, Configuration.class);
            }
            catch (IOException e) {
                throw new IllegalArgumentException("Error converting JSON to configuration", e);
            }
        }
    }

    /** Oracle and H2 hand CLOB columns back as java.sql.Clob. */
    @ReadingConverter
    static class ClobReader
            implements Converter<Clob, String>
    {
        @Override
        public String convert(Clob source)
        {
            try (Reader reader = source.getCharacterStream()) {
                StringBuilder builder = new StringBuilder();
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    builder.append(buffer, 0, read);
                }
                return builder.toString();
            }
            catch (SQLException e) {
                throw new IllegalStateException("Cannot read CLOB column", e);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /** Oracle stores flags as NUMBER(1). */
    @ReadingConverter
    static class NumberToBooleanReader
            implements Converter<Number, Boolean>
    {
        @Override
        public Boolean convert(Number source)
        {
            return source.intValue() != 0;
        }
    }
}
