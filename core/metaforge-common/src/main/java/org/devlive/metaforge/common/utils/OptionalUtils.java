package org.devlive.metaforge.common.utils;

import java.util.Optional;

public class OptionalUtils
{
    private OptionalUtils() {}

    public static boolean checkEmpty(Optional<String> optional)
    {
        boolean flag = false;
        if (optional.isPresent()) {
            flag = true;
        }
        return flag;
    }

    public static boolean isEmpty(Optional<String> optional)
    {
        return !checkEmpty(optional);
    }

    public static boolean isNotEmpty(Optional<String> optional)
    {
        return checkEmpty(optional);
    }
}
