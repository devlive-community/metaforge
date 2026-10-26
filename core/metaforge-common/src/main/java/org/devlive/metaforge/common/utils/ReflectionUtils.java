package org.devlive.metaforge.common.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

@Slf4j
@SuppressFBWarnings(value = {"REFLF_REFLECTION_MAY_INCREASE_ACCESSIBILITY_OF_FIELD"})
public class ReflectionUtils
{
    /**
     * Checks if the given object has a field with the specified name.
     *
     * @param obj the object to inspect
     * @param fieldName the name of the field to check for
     * @return true if the field exists, false otherwise
     */
    public static boolean hasField(Object obj, String fieldName)
    {
        try {
            obj.getClass().getDeclaredField(fieldName);
            return true;
        }
        catch (NoSuchFieldException e) {
            log.debug("Has field exception", e);
            return false;
        }
    }

    /**
     * Sets the value of a field in an object using reflection.
     *
     * @param obj the object whose field value will be set
     * @param fieldName the name of the field to set
     * @param value the value to set in the field
     */
    public static void setFieldValue(Object obj, String fieldName, Object value)
    {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("Set field value exception", e);
        }
    }

    /**
     * Retrieves the value of a specified field from an object using reflection.
     *
     * @param obj the object from which to retrieve the field value
     * @param fieldName the name of the field to retrieve
     * @return the value of the specified field
     */
    public static Object getFieldValue(Object obj, String fieldName)
    {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("Get field value exception", e);
            return null;
        }
    }
}
