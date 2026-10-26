package org.devlive.metaforge.common.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class CollectorUtils
{
    private CollectorUtils() {}

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor)
    {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        // The putIfAbsent method adds the key and value pair. If the key does not exist in the map, the key pair is added and null is returned. If the corresponding value already exists, the key pair remains the same.
        // If null is returned, the data is added successfully (no repeat). No repeat (null==null :TRUE)
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
