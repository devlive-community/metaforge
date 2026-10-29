package org.devlive.metaforge.service.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads the {@link Id} field of any mapped entity.
 */
public final class Ids
{
    private static final Map<Class<?>, Field> FIELDS = new ConcurrentHashMap<>();

    private Ids()
    {
    }

    public static Object of(Object entity)
    {
        Field field = FIELDS.computeIfAbsent(entity.getClass(), type -> {
            Field[] found = new Field[1];
            ReflectionUtils.doWithFields(type, candidate -> found[0] = candidate, candidate -> candidate.isAnnotationPresent(Id.class));
            if (found[0] == null) {
                throw new IllegalArgumentException(type.getName() + " has no @Id field");
            }
            ReflectionUtils.makeAccessible(found[0]);
            return found[0];
        });
        return ReflectionUtils.getField(field, entity);
    }
}
