package org.devlive.metaforge.service.persistence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rows a user may see in a filtered listing: everything, or rows where any of the given
 * properties equals its value (for example the owner id, or a public flag).
 */
public final class Visibility
{
    private static final Visibility ALL = new Visibility(Collections.emptyMap());

    private final Map<String, Object> anyOf;

    private Visibility(Map<String, Object> anyOf)
    {
        this.anyOf = anyOf;
    }

    public static Visibility all()
    {
        return ALL;
    }

    public static Visibility where(String property, Object value)
    {
        Map<String, Object> anyOf = new LinkedHashMap<>();
        anyOf.put(property, value);
        return new Visibility(anyOf);
    }

    public Visibility or(String property, Object value)
    {
        Map<String, Object> anyOf = new LinkedHashMap<>(this.anyOf);
        anyOf.put(property, value);
        return new Visibility(anyOf);
    }

    public Map<String, Object> anyOf()
    {
        return Collections.unmodifiableMap(anyOf);
    }
}
